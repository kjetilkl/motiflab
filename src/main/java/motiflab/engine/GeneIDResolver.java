/*
 
 
 */

package motiflab.engine;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import motiflab.engine.data.Organism;
import motiflab.engine.data.Sequence;
import motiflab.engine.protocol.ParseError;

/**
 * This class contains methods for obtaining gene names and transcription start 
 * and end sites given one or more Gene Identifiers in various formats
 * (ENSEMBL, Entrez, UniGene etc.)
 *  
 * @author kjetikl
 */
public class GeneIDResolver {
    private HashMap<Integer,String> databaseMap=new HashMap<Integer,String>(); // Maps organism taxonomy ID to BioMart database 
    private HashMap<String,String> idFormatMap=new HashMap<String,String>(); // maps the "internal ID type" to BioMart ID (the internal ID defaults to lowercase version of presentation name)
    private HashMap<String,String> idFormatDatabaseMap=new HashMap<String,String>(); // maps the "internal ID type" to the name of the database of the ID
    private HashMap<String,String> idFormatPresentationNameMap=new HashMap<String,String>(); // maps "internal ID type" to Presentation name (the internal ID defaults to lowercase version of presentation name)
    private HashMap<String,String> webLinkTemplate=new HashMap<String,String>(); // maps "internal ID type" to a web link template (an URL string containing the placeholder "{ID}" which can be replaced by actual IDs)
    private HashMap<String,String[]> buildMap=new HashMap<String,String[]>(); // contains references to older biomarts for older builds or other biomart services. The value is an array containing: [0]=>Biomart URL (required), [1]=>Virtual Schema name, [2]=>config version, [3]=>Biomart attributes
    private ArrayList<String> otherIdentifiers=new ArrayList<String>(); // contains additional identifier types (not necessarily genes) that the system should be aware of
    
    private MotifLabEngine engine;
    private String cachefilename="GeneIDmappingCache.ser";
    private HashMap<String,ArrayList<GeneIDmapping>> cache=null;
    private String cacheDirectory=null;
    private boolean useCache=true;    
    private boolean cacheUpdated=false;
    private String biomartCurrent="http://www.biomart.org/biomart/martservice";
    private String defaultVirtualSchemaName="default";
    private String defaultDatasetConfigVersion ="0.6";
    private String[] defaultBioMartAttributes_new=new String[]{"external_gene_name","chromosome_name","start_position/transcript_start","end_position/transcript_end","strand","go_id"};
    private String[] defaultBioMartAttributes_old=new String[]{"external_gene_id","chromosome_name","start_position/transcript_start","end_position/transcript_end","strand","go_id"};
    
            
     public GeneIDResolver(MotifLabEngine engine) {
         // NOTE: these hardcoded settings are not really used anymore since the configuration of the GeneIDResolver
         //       is normally loaded from a config-file using the initializeFromFile() method below (which is called by the engine)
        databaseMap.put(Organism.HUMAN, "hsapiens_gene_ensembl");
        databaseMap.put(Organism.MOUSE, "mmusculus_gene_ensembl");
        databaseMap.put(Organism.RAT, "rnorvegicus_gene_ensembl");
        databaseMap.put(Organism.FRUITFLY, "dmelanogaster_gene_ensembl");
        databaseMap.put(Organism.CHICKEN, "ggallus_gene_ensembl");
        databaseMap.put(Organism.COW, "btaurus_gene_ensembl");
        databaseMap.put(Organism.YEAST, "scerevisiae_gene_ensembl");        
        databaseMap.put(Organism.FROG, "xtropicalis_gene_ensembl");        
        databaseMap.put(Organism.ZEBRAFISH, "drerio_gene_ensembl");
        databaseMap.put(Organism.RABBIT, "ocuniculus_gene_ensembl");
        idFormatMap.put("ensembl gene", "ensembl_gene_id");
        idFormatMap.put("ensembl transcript", "ensembl_transcript_id");
        idFormatMap.put("entrez gene", "entrezgene");
        idFormatMap.put("embl", "embl");
        idFormatMap.put("hgnc symbol", "hgnc_symbol");
        idFormatMap.put("unigene id", "unigene");
        idFormatMap.put("ccds id", "ccds");
        idFormatMap.put("refseq dna", "refseq_dna");
        idFormatPresentationNameMap.put("ensembl gene", "Ensemble Gene");
        idFormatPresentationNameMap.put("ensembl transcript", "Ensembl Transcript");
        idFormatPresentationNameMap.put("entrez gene", "Entrez Gene");
        idFormatPresentationNameMap.put("embl", "EMBL");
        idFormatPresentationNameMap.put("hgnc symbol", "HGNC symbol");
        idFormatPresentationNameMap.put("unigene id", "UniGene ID");
        idFormatPresentationNameMap.put("ccds id", "CCDS ID");
        idFormatPresentationNameMap.put("refseq dna", "RefSeq DNA");              
        buildMap.put("hg18",new String[]{"http://mar2009.archive.ensembl.org/biomart/martservice",null,null});        
        buildMap.put("mm8",new String[]{"http://aug2007.archive.ensembl.org/biomart/martservice",null,null});        
        buildMap.put("mm7",new String[]{"http://apr2006.archive.ensembl.org/biomart/martservice",null,null});        
        buildMap.put("danRer5",new String[]{"http://mar2009.archive.ensembl.org/biomart/martservice",null,null});        
        this.engine=engine;  
    }       

    /** This method can be used to initialize the configuration of GeneIDResolver
     *  with settings read from the given file
     */
    public void initializeFromFile(File file, boolean clearOldSettings) throws IOException, SystemError {
        if (clearOldSettings) {
           databaseMap.clear();
           idFormatMap.clear();
           idFormatPresentationNameMap.clear();      
           otherIdentifiers.clear();
           buildMap.clear();
        }
        BufferedReader inputStream=null;
        try {
            inputStream=new BufferedReader(new FileReader(file));
            String line;
            while((line=inputStream.readLine())!=null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue; // comments and empty lines
                String[] fields=line.split("\\t");
                if (fields[0].equalsIgnoreCase("IDFormat")) {
                   if (fields.length<3) throw new SystemError("Expected at least 3 fields for IDFormat-line in geneID-configuration file, but got "+fields.length+":\n"+line);                  
                   idFormatMap.put(fields[1].toLowerCase(),fields[2]);
                   idFormatPresentationNameMap.put(fields[1].toLowerCase(),fields[1]);
                   if (fields.length>=4 && !fields[3].isEmpty()) {
                       idFormatDatabaseMap.put(fields[1].toLowerCase(),fields[3]);
                   } else {
                       String dbname=fields[1].trim();
                       if (dbname.contains(" ")) dbname=dbname.substring(0,dbname.indexOf(' ')); // use first word
                       idFormatDatabaseMap.put(fields[1].toLowerCase(),dbname);
                   }
                   if (fields.length>=5 && !fields[4].isEmpty()) { 
                      webLinkTemplate.put(fields[1].toLowerCase(),fields[4]);
                   }                   
                } else if (fields[0].equalsIgnoreCase("otherID")) {
                   if (fields.length<2) throw new SystemError("Expected at least 2 fields for otherID-line in geneID-configuration file, but got "+fields.length+":\n"+line);                  
                   otherIdentifiers.add(fields[1]);
                   if (fields.length>=3 && !fields[2].isEmpty()) { 
                      webLinkTemplate.put(fields[1].toLowerCase(),fields[2]);
                   }                   
                } else if (fields[0].equalsIgnoreCase("BuildDB")) {
                   if (fields.length<3) throw new SystemError("Expected at least 3 fields for BuildDB-line in geneID-configuration file, but got "+fields.length+":\n"+line);                 
                   buildMap.put(fields[1],Arrays.copyOfRange(fields, 2, fields.length));
                } else if (fields[0].equalsIgnoreCase("OrganismDB")) {
                   if (fields.length<3) throw new SystemError("Expected 3 fields for OrganismDB-line in geneID-configuration file, but got "+fields.length+":\n"+line);
                   try {
                      int organism=Integer.parseInt(fields[1]);
                      databaseMap.put(new Integer(organism), fields[2]);
                   } catch (NumberFormatException nfe) {
                      throw new SystemError("Unable to parse expected integer taxonomy ID in second column for OrganismDB-line in geneID-configuration file:\n"+line);
                   }
                } else if (fields[0].equalsIgnoreCase("BaseURL")) {
                   if (fields.length<2) throw new SystemError("Expected 2 fields for BaseURL-line in geneID-configuration file, but got "+fields.length+":\n"+line);
                   biomartCurrent=fields[1];
                   if (fields.length>=3 && fields[2]!=null && !fields[2].isEmpty()) defaultVirtualSchemaName=fields[2];
                   if (fields.length>=4 && fields[3]!=null && !fields[3].isEmpty()) defaultDatasetConfigVersion=fields[3];
                } else throw new SystemError("Unrecognized entry in geneID-configuration file:\n"+line);
            }
            inputStream.close();
        } catch (IOException e) {
            throw e;
        } finally {
            try {if (inputStream!=null) inputStream.close();} catch (IOException ioe) {}
        }
    }
    
    /**
     * Adds a new identifier (web link) to the current configuration. 
     * @param identifierName
     * @param definition The definition of the identifier. If this is empty the identifier will be removed
     * @param replace If this parameter is FALSE then a ParseError will be thrown if the identifier already exists. If TRUE the existing identifier will be replaced 
     * @param persist If TRUE, then the new configuration will be saved to disc (persisted).
     * @throws ParseError thrown if "replace=FALSE" and the identifier already exists. The message will be "identifier exists: name"
     * @throws IOException 
     * @return Returns the old definition of the identifier the was replaced, or NULL of the identifier was new
     */
    public String addOtherIdentifier(String identifierName, String definition, boolean replace, boolean persist) throws ParseError, IOException {
        String internal=identifierName.toLowerCase();
        String oldValue=null;
        if (otherIdentifiers.contains(identifierName)) {
              oldValue=webLinkTemplate.get(internal);
              if (!replace) throw new ParseError("identifier exists: "+identifierName);
              if (definition.isEmpty()) {
                  otherIdentifiers.remove(identifierName);
                  webLinkTemplate.remove(internal);                  
              } else webLinkTemplate.put(internal, definition);    
        } else if (!definition.isEmpty()) {
            webLinkTemplate.put(internal, definition);
            otherIdentifiers.add(identifierName);
        }
        if (persist) {
            File file=new File(engine.getMotifLabDirectory()+java.io.File.separator+"GeneIDResolver.config");
            writeToFile(file);
        }
        return oldValue;
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
    public void replaceAndSave(ArrayList<String[]> biomartDBs, ArrayList<String[]> geneIdentifiers, ArrayList<String[]>otherIDs, ArrayList<String[]> buildBioMartURLs, File file) throws ParseError, IOException {
        for (String[] pair:biomartDBs) {
            try {int taxonomyID=Integer.parseInt(pair[0]);}
            catch (NumberFormatException e) {throw new ParseError("Taxonomy ID should be an integer number, but got '"+pair[0]+"'");}
            if (pair[1].isEmpty()) throw new ParseError("Missing BioMart database for organism with taxonomy ID '"+pair[0]+"'");
        }        
        for (String[] tuple:buildBioMartURLs) { // check BioMart URLs
            if (tuple.length<2) throw new ParseError("Expected at least two fields for BioMart entry");
            if (tuple[1]!=null && !(tuple[1].startsWith("config=") || tuple[1].startsWith("file=") || tuple[1].startsWith("webfile=") || tuple[1].startsWith("archive=") || tuple[1].startsWith("biomart=") || tuple[1].startsWith("ensembl="))) {
                try {URL url=new URL(tuple[1]);}
                catch (MalformedURLException e) {throw new ParseError("Malformed URL for genome build '"+tuple[0]+"'");}
            }
            if (tuple.length>=5 && tuple[4]!=null && !tuple[4].trim().isEmpty()) { // check that at least 5 BioMart attributes are listed
                String[] test=tuple[4].trim().split("\\s*,\\s*");
                if (test.length<5) throw new ParseError("Expected at least 5 Biomart attributes for "+tuple[0]+", but got "+test.length+" : "+tuple[4]);
            }
        }
        if (file!=null) {
            buildMap.clear();
            databaseMap.clear();
            idFormatMap.clear();
            idFormatPresentationNameMap.clear();
            otherIdentifiers.clear();            
            for (String[] pair:biomartDBs) {
                try {int taxonomyID=Integer.parseInt(pair[0]);
                    databaseMap.put(new Integer(taxonomyID),pair[1]);
                }
                catch (NumberFormatException e) {} // this should have been caught above
            } 
            for (String[] pair:geneIdentifiers) {
                String internal=pair[0].toLowerCase();
                idFormatMap.put(internal, pair[1]);
                idFormatPresentationNameMap.put(internal, pair[0]);
                if (pair.length>=3) {
                    if (!pair[2].isEmpty()) idFormatDatabaseMap.put(internal, pair[2]);
                    else idFormatDatabaseMap.remove(internal);                   
                }
                if (pair.length>=4) {
                    if (!pair[3].isEmpty()) webLinkTemplate.put(internal, pair[3]);
                    else webLinkTemplate.remove(internal);
                }                
            } 
            for (String[] pair:otherIDs) {
                String internal=pair[0].toLowerCase(); // use lower-case ID name in webLink hashmap
                otherIdentifiers.add(pair[0]);
                if (pair.length>=2) {               
                    if (!pair[1].isEmpty()) webLinkTemplate.put(internal, pair[1]);
                    else webLinkTemplate.remove(internal); 
                }                  
            }              
            for (String[] tuple:buildBioMartURLs) {
                if (tuple[0].equalsIgnoreCase("default")) { // use virtualschema, configversion and attributes values from "default" biomart as default for others also
                    biomartCurrent=tuple[1];
                    if (tuple.length>=3 && tuple[2]!=null && !tuple[2].isEmpty()) defaultVirtualSchemaName=tuple[2];
                    if (tuple.length>=4 && tuple[3]!=null && !tuple[3].isEmpty()) defaultDatasetConfigVersion=tuple[3];
                    if (tuple.length>=5 && tuple[4]!=null && !tuple[4].trim().isEmpty()) defaultBioMartAttributes_new=tuple[4].trim().split("\\s*,\\s*");  // do not use this as default                   
                }
                else buildMap.put(tuple[0], Arrays.copyOfRange(tuple,1,tuple.length));
            }                    
            writeToFile(file);
        }
    }    
    
    /**
     * Outputs the current configuration to the given file
     * @param file
     * @throws IOException
     */
    public void writeToFile(File file) throws IOException {
        BufferedWriter outputStream=null;
        try {
            outputStream=new BufferedWriter(new FileWriter(file));
            outputStream.write("# This file contains configuration data to enable the system to resolve different types of gene identifiers using BioMart");
            outputStream.newLine();
            outputStream.newLine();
            outputStream.write("# The base URL for BioMart");
            outputStream.newLine();
            outputStream.write("BaseURL\t"+biomartCurrent);
            if (!defaultVirtualSchemaName.equals("default") || !defaultDatasetConfigVersion.equals("0.6")) outputStream.write("\t"+defaultVirtualSchemaName);
            if (!defaultDatasetConfigVersion.equals("0.6")) outputStream.write("\t"+defaultDatasetConfigVersion);
            outputStream.newLine();
            outputStream.newLine();
            outputStream.write("# Different supported organisms and their respective databases");
            outputStream.newLine();
            for (Integer organism:databaseMap.keySet()) {
               outputStream.write("OrganismDB\t"+organism.intValue()+"\t"+databaseMap.get(organism));
               outputStream.newLine();
            }
            outputStream.newLine();
            outputStream.newLine();
            outputStream.write("# Different supported gene identifier types");
            outputStream.newLine();
            for (String idformat:idFormatMap.keySet()) {
               String presentationName=idFormatPresentationNameMap.get(idformat);
               String id=idFormatMap.get(idformat);
               String dbname=idFormatDatabaseMap.get(idformat);
               String weblink=webLinkTemplate.get(idformat);
               if (id==null) id="";
               if (dbname==null) dbname="";
               if (weblink==null) weblink="";
               outputStream.write("IDFormat\t"+presentationName+"\t"+id+"\t"+dbname+"\t"+weblink);
               outputStream.newLine();
            }
            outputStream.newLine();
            outputStream.newLine();
            outputStream.write("# Other identifier types (not necessarily gene IDs)");
            outputStream.newLine();            
            for (String id:otherIdentifiers) {
               
               String weblink=webLinkTemplate.get(id.toLowerCase());
               if (weblink==null) weblink="";
               outputStream.write("otherID\t"+id+"\t"+weblink);
               outputStream.newLine();
            }
            outputStream.newLine();
            outputStream.newLine();            
            outputStream.write("# Special BioMart services for genome builds that can not use the default BioMart");
            outputStream.newLine();
            for (String build:buildMap.keySet()) {
               String[] mapentry=buildMap.get(build);
               outputStream.write("BuildDB\t"+build);
               for (String value:mapentry) {
                  outputStream.write("\t"+value); 
               }
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
     * Returns a list with the URLs for all BioMart resources used by the GeneIDResolver
     * Each entry consisting of a tuple of Strings where the first is the build name and the second
     * is the URL. Optional third and fourth entries specify the virtual schema name and config version. 
     * An optional fifth entry specifies an ordered comma-separated list of Biomart attributes.
     * The first entry will always have the name "Default" and its URL will be the 
     * default BioMart resource used for all genome builds unless otherwise specified.
     * The remaining entries (sorted alphabetically by name) will consist of genome builds that
     * override this default to use alternative BioMart resources (e.g. older genome builds that
     * require archived BioMart resources, or perhaps even proxy servers).
     * @param columns The number of columns to return. This should at least be equal to the maximum number of fields used to describe each build
     */
    public String[][] getURLsForBuilds(int columns) {  
        if (columns<4) columns=4;
        String[][] list=new String[buildMap.size()+1][columns];
        list[0][0]="Default";
        list[0][1]=biomartCurrent;
        list[0][2]=defaultVirtualSchemaName;
        list[0][3]=defaultDatasetConfigVersion;
        list[0][4]=MotifLabEngine.splice(defaultBioMartAttributes_new,",");        
        ArrayList<String> keys=new ArrayList<String>(buildMap.keySet());
        Collections.sort(keys);
        for (int i=0;i<keys.size();i++) {
            String[] mapentry=buildMap.get(keys.get(i));
            String[] entry=new String[columns];
            entry[0]=keys.get(i);
            System.arraycopy(mapentry, 0, entry, 1, mapentry.length);
            for (int j=0;j<entry.length;j++) {
                if (entry[j]==null) entry[j]="";
            }
            list[i+1]=entry;
        }
        return list;
    }
    
    /** Specifies a cache directory to use for this GeneIDResolver */
    public void setCacheDirectory(String cacheDir) {
        cacheDirectory=cacheDir;
    }
    
    /** Specifies whether this GeneIDResolver should make use of a local disc cache to store resolved Gene ID mappings */
    public void setUseCache(boolean flag) {
        useCache=flag;
    }
    
    /** Clears the installed Cache for this GeneIDResolver
     * @return TRUE if the cached was successfully clear (or if no cached was installed) or
     * FALSE if the installed cached could not be successfully cleared
     */
    public boolean clearCache() {
        try {
          String fullpath=cacheDirectory+File.separator+cachefilename;
          File file=new File(fullpath);
          if (file.exists()) return file.delete();
        } catch (Exception e) {return false;}
        cache=new HashMap<String, ArrayList<GeneIDmapping>>();
        return true;
    }
    
    public String[] getBioMartAttributes(boolean newstyle) {      
        String[] attributes=(newstyle)?defaultBioMartAttributes_new:defaultBioMartAttributes_old;
        return Arrays.copyOf(attributes, attributes.length);
    }
    
    /** */
    @SuppressWarnings("unchecked")
    private void installCache() {
        String fullpath=cacheDirectory+File.separator+cachefilename;
        File file=new File(fullpath);
        if (file.exists()) cache=(HashMap<String,ArrayList<GeneIDmapping>>)getObjectFromFile(fullpath);
        if (cache==null) cache=new HashMap<String, ArrayList<GeneIDmapping>>();      
    }
    
    private void storeCacheToDisc() {
        String fullpath=cacheDirectory+File.separator+cachefilename;
        //System.err.println("Storing cache to disc with "+cache.size()+" entries");
        if (storeObjectInFile(fullpath,cache)) cacheUpdated=false;      
    }
    
    private void storeInCache(GeneIDmapping mapping,String format, int organism, String build) {
        String key=mapping.geneID+"\t"+format+"\t"+organism+"\t"+build;
        ArrayList<GeneIDmapping> list=cache.get(key);
        if (list==null) list = new ArrayList<GeneIDmapping>();     
        if (!containsMapping(list, mapping)) list.add(mapping);  
        cache.put(key, list);
        cacheUpdated=true;
    }
    
    /** Returns a list of organisms that this class can resolve Gene IDs for 
     * The numbers returned are NCBI taxonomy IDs. Common and latin names
     * corresponding to these IDs are obtained from the Organism class
     */
    public int[] getSupportedOrganisms() {
        Collection<Integer> organisms=databaseMap.keySet(); 
        int[] supported=new int[organisms.size()];
        int index=0;
        for (Integer org:organisms) {
            supported[index]=org;
            index++;
        }     
        return supported;
    }
    
    /** Returns true if the specified organism is supported by this GeneIDResolver */
    public boolean isOrganismSupported(int organism) {
        return databaseMap.containsKey(organism);      
    }
    
    /** Returns a list of ID formats that this class can resolve Gene IDs for (using presentable names) */
    public String[] getSupportedIDFormats() {
        Collection<String> formats=idFormatPresentationNameMap.values();
        String[] list=new String[formats.size()];
        list=formats.toArray(list);
        Arrays.sort(list);         
        return list;
    }
    
    /** Returns a list of ID formats that this class can resolve Gene IDs for (using internal names) */
    public String[] getSupportedIDFormatsInternalNames() {
        Collection<String> formats=idFormatPresentationNameMap.keySet();
        String[] list=new String[formats.size()];
        list=formats.toArray(list);
        Arrays.sort(list);
        return list;
    }
    
    /** Returns a list of other ID types (not necessarily gene identifiers) that are known 
     *  to the system, e.g. PubMed IDs or TRANSFAC IDs etc.
     */
    public String[] getOtherIDs() {
        if (otherIdentifiers==null) return new String[0];
        String[] list=new String[otherIdentifiers.size()];
        list=otherIdentifiers.toArray(list);
        Arrays.sort(list);
        return list;
    }    
    
    public String getBiomartID(String idformat) {
        return idFormatMap.get(idformat);
    } 
    
    public String getBiomartDBnameForOrganisms(int taxonomyID) {
        return databaseMap.get(taxonomyID);
    } 
    
    public String getPresentationName(String idformat) {
        return idFormatPresentationNameMap.get(idformat);
    }      
    
    public String getDatabaseName(String idformat) {
        return idFormatDatabaseMap.get(idformat);
    }    
    
    public String getWebLinkTemplate(String idformat) {
        return webLinkTemplate.get(idformat);
    }
    
    /** Returns a direct URL string to a database web page for the given identifier */
    public String getWebLink(String idformat, String identifier) {
        String weblink=webLinkTemplate.get(idformat);
        if (weblink==null) weblink=webLinkTemplate.get(idformat.toLowerCase()); // just in case
        if (weblink==null) return null;
        if (weblink.startsWith("[")) {
            try {
                weblink=getMatchingWebLink(weblink,identifier);
            } catch (ParseError p) {weblink=null;}
        }                 
        if (weblink==null || weblink.isEmpty()) return null;
        return weblink.replace("{ID}", identifier);
    } 
    
    public String getMatchingWebLink(String template, String identifier) throws ParseError {
        ArrayList<String> parts=MotifLabEngine.splitOnCharacter(template, ',', '[', ']');
        for (String element:parts) {
            if (element.startsWith("[")) {
                element=MotifLabEngine.stripBraces(element, "[", "]");
                String[] pair=element.split(",",2);
                if (pair.length!=2) throw new ParseError("Syntax error in web-link pattern (missing comma):"+element);
                if (identifier.matches(pair[0])) return pair[1];
            } else {
                return element;
            }
        }
        return null;
    }
    
    /** Returns true if the specified ID format is supported by this GeneIDResolver */
    public boolean isIDFormatSupported(String format) {
        return (idFormatPresentationNameMap.containsKey(format.toLowerCase()) || idFormatPresentationNameMap.containsValue(format));
    }
    
    /** Returns true if the specified genome build is supported by this GeneIDResolver */    
    public boolean isGenomeBuildSupported(String genomebuild) {
        return Organism.isGenomeBuildSupported(genomebuild);
    }
    
    public ArrayList<String> getExternalDatabaseNames() {
        ArrayList<String> names=new ArrayList<String>();
//        for (String db:idFormatDatabaseMap.values()) {
//            if (!names.contains(db)) names.add(db);
//        }
        for (String db:idFormatPresentationNameMap.values()) {
            if (!names.contains(db)) names.add(db);
        }        
        for (String db:otherIdentifiers) {
            if (!names.contains(db)) names.add(db);
        }        
        Collections.sort(names);
        return names;
    }
    
    /** 
     * This method tries to resolve a list of gene identifiers in the given format.
     * The format and organism must be stated explicitly for each identifier
     * and need not be the same for all identifiers. This method will include GO annotations
     * if possible.
     */
    public ArrayList<GeneIDmapping> resolveIDs(ArrayList<GeneIdentifier> list) throws Exception {   
        return resolveIDs(list, true);
    }    
    
    /** 
     * This method tries to resolve a list of gene identifiers in the given format.
     * The format and organism must be stated explicitly for each identifier
     * and need not be the same for all identifiers
     */
    public ArrayList<GeneIDmapping> resolveIDs(ArrayList<GeneIdentifier> list, boolean includeGO) throws Exception {        
        ArrayList<GeneIDmapping> result=new ArrayList<GeneIDmapping>(list.size());
            HashMap<String,ArrayList<String>> subgroups=new HashMap<String,ArrayList<String>>();
            for (GeneIdentifier geneID:list) {                
                String key=geneID.format+"\t"+geneID.organism+"\t"+geneID.build;
                if (useCache) {
                    if (cache==null) installCache();
                    ArrayList<GeneIDmapping> cacheresults=resolveIDFromCache(geneID.identifier,geneID.format,geneID.organism, geneID.build, includeGO);
                    // System.err.println("Got "+cacheresults.size()+" hits in cache for "+geneID.identifier);
                    if (cacheresults.size()>0) {addNewMappings(result,cacheresults); continue;}
                }
                // we proceed here only if the cache did not return results
                if (!subgroups.containsKey(key)) subgroups.put(key,new ArrayList<String>());
                ArrayList<String> uniformlist=subgroups.get(key);
                uniformlist.add(geneID.identifier); // these uniform lists contains IDs from the same organism/build and in the same format
            }
            // resolve those IDs that were not found in cache. They are divided into subgroups based on organism/build since these can be queried together
            Set<String> keys=subgroups.keySet();
            for (String key:keys) { // key is "ID-format + organism + build" and points to a list of similar IDs which will be resolved together by a single network request (or cache lookup)
                String[] elements=key.split("\t");
                String format=elements[0];
                String organismString=elements[1];
                String build=elements[2];
                int organism=0;
                try {organism=Integer.parseInt(organismString);} catch (NumberFormatException e) {System.err.println("SYSTEM ERROR: Unable to parse expected numeric value in GeneIDResolver.resolveIDs(): "+e.getMessage());}
                ArrayList<String> sublist=subgroups.get(key);
                ArrayList<GeneIDmapping> partial=resolveIDlist(sublist,format,organism,build,includeGO);
                addNewMappings(result,partial);
                if (useCache) {
                    for (GeneIDmapping mapping:partial) storeInCache(mapping,format,organism,build);
                }
            }
            if (useCache && cacheUpdated) storeCacheToDisc();
            cache=null; // release cache to free up memory?
            return result;
    }

    /** Adds the mappings in the sourceList to the targetList but only if the targetList does not      
     *  already contain any identical mappings
     */
    private void addNewMappings(ArrayList<GeneIDmapping> targetList, ArrayList<GeneIDmapping> sourceList) {
        for (GeneIDmapping mapping:sourceList) {
            if (!containsMapping(targetList, mapping)) targetList.add(mapping);
        }
    }
    
    /** returns TRUE if the list contains the mapping (the given mapping is similar in every respect to one in the list (except with regards to GO annotations)) */
    private boolean containsMapping(ArrayList<GeneIDmapping> list, GeneIDmapping mapping) {
        for (GeneIDmapping entry:list) {
            if (entry.equals(mapping)) return true;
        }
        return false;
    }
    
    /** returns a mapping from the list which is similar to the provided mapping (same location but not necessarily same GO annotation)
     *  or NULL if no such mapping exists in the list
     */
    private GeneIDmapping getMappingFromList(ArrayList<GeneIDmapping> list, GeneIDmapping mapping) {
        for (GeneIDmapping entry:list) {
            if (entry.equals(mapping)) return entry;
        }
        return null;
    }    
    
    /** Resolves a list of identifiers in a single format from a single organism+build by querying BioMart or some file containing mappings */
    private ArrayList<GeneIDmapping> resolveIDlist(ArrayList<String> list, String format, int organism, String build, boolean includeGO) throws Exception {
        boolean useTranscript=false;
        if (format.equalsIgnoreCase("Ensembl Transcript")) useTranscript=true;
        String database=databaseMap.get(organism);      
        if (database==null) throw new ExecutionError("Unsupported organism '"+Organism.getCommonName(organism)+"' in Gene ID Resolver");
        String idformat=idFormatMap.get(format.toLowerCase());
        if (idformat==null)  throw new ExecutionError("Unsupported format '"+format+"' in Gene ID Resolver");
        String biomartURL=biomartCurrent;
        String useVirtualSchema=defaultVirtualSchemaName;
        String useConfigVersion=defaultDatasetConfigVersion;
        String[] biomartURLentry=buildMap.get(build);      
        String[] biomartAttributes=null;
        if (biomartURLentry!=null) {
            if (biomartURLentry.length>0) biomartURL=biomartURLentry[0];
            if (biomartURLentry.length>1 && biomartURLentry[1]!=null && !biomartURLentry[1].isEmpty()) useVirtualSchema=biomartURLentry[1];
            if (biomartURLentry.length>2 && biomartURLentry[2]!=null && !biomartURLentry[2].isEmpty()) useConfigVersion=biomartURLentry[2];
            if (biomartURLentry.length>3 && biomartURLentry[3]!=null && !biomartURLentry[3].trim().isEmpty()) biomartAttributes=biomartURLentry[3].trim().split("\\s*,\\s*");            
        }
        if (!Organism.isGenomeBuildSupported(build)) throw new ExecutionError("Unsupported genome build '"+build+"' in Gene ID Resolver");
        if (biomartAttributes!=null && biomartAttributes.length<5) throw new ExecutionError("Unsupported genome build '"+build+"' in Gene ID Resolver");
        
        URL url=null;
        String xml=null;
        ArrayList<String> page=null;
        if (biomartURL.startsWith("archive=") || biomartURL.startsWith("biomart=") || biomartURL.startsWith("ensembl=")) { // shorthand for ensembl archived site using MONTH YEAR
            String archive=biomartURL.substring(8); // all the 3 strings above have length 8 :)
            String month="";
            String year="";
            if (archive.matches("[a-zA-Z]+\\s*\\d\\d")) { // two digit year
                int split=archive.length()-2;
                month=archive.substring(0,split).trim();
                year=archive.substring(split);
            } 
            else if (archive.matches("[a-zA-Z]+\\s*\\d\\d\\d\\d")) { // four digit year
                int split=archive.length()-4;
                month=archive.substring(0,split).trim();
                year=archive.substring(split);            
            } 
            if (month.length()>=3) month=month.substring(0, 3).toLowerCase();
            if (year.length()==2) year="20"+year;
            if (month.isEmpty() || year.isEmpty()) biomartURL=archive; // This will fail and notify the user
            else biomartURL="https://"+month+year+".archive.ensembl.org/biomart/martservice";
        } // note no ELSE here since we just convert the biomartURL string and will process it further below
        
        if (biomartURL.startsWith("config=")) { // Resolve gene IDs based on config file
            String filename=biomartURL.substring("config=".length());
            String configDir=engine.getMotifLabDirectory();
            if (!configDir.endsWith(File.separator)) configDir+=File.separator;
            filename=configDir+filename;
            page=resolveFromLocalFile(filename, idformat, list, includeGO);         
        } else if (biomartURL.startsWith("file=")) { // Resolve gene IDs based on config file
            String filename=biomartURL.substring("file=".length());
            page=resolveFromLocalFile(filename, idformat, list, includeGO);               
        } else if (biomartURL.startsWith("webfile=")) { // use BioMart proxy on MotifLab web server
            String website=engine.getWebSiteURL();
            url=new URL(website+"biomartProxy.cgi"); // NOTE: It is not possible to use "www.motiflab.org" since the resulting redirection will for some reason change the request method from POST to GET at the server side :(
            database=biomartURL.substring("webfile=".length()); 
            xml=getXMLQueryString(idformat, database, useVirtualSchema, useConfigVersion, biomartAttributes, list, useTranscript, false, includeGO); 
            page=getPageUsingHttpPost(url,xml,engine.getNetworkTimeout());
        } else {
            url=new URL(biomartURL); 
            xml=getXMLQueryString(idformat, database, useVirtualSchema, useConfigVersion, biomartAttributes, list, useTranscript, isOldBiomart(url), includeGO);         
            page=getPageUsingHttpPost(url,xml,engine.getNetworkTimeout());
        }
        //System.err.println(xml);        
        return parseResults(page);        
    }
    
    private boolean isOldBiomart(URL url) {
        boolean isOld=false;
        String host=url.getHost();
        if (host.startsWith("plants.ensembl") || host.startsWith("fungi.ensembl") || host.startsWith("metazoa.ensembl") || host.startsWith("protists.ensembl")) isOld=true; // "grch37.ensembl" uses the new attribute names
        else if (host.contains(".archive.")) {
            String month=host.substring(0,3);
            String yearString=host.substring(3,7);
            try {
                int year=Integer.parseInt(yearString);
                if (year<2014) isOld=true; // all servers from before 2013 are old 
                else isOld=month.equals("feb"); // feb2014 is the only "old" in 2014
            } catch (NumberFormatException e) {}
        }
        return isOld;
    }
    
    /** Resolves a single identifier by looking up in the cache. Note that the identifier could possible have multiple matches in the cache */    
    private ArrayList<GeneIDmapping> resolveIDFromCache(String identifier, String format, int organism, String build, boolean includeGO) throws Exception {
        ArrayList<GeneIDmapping> result=null;
        if (cache==null) return new ArrayList<GeneIDmapping>();
        String key=identifier+"\t"+format+"\t"+organism+"\t"+build;
        result=cache.get(key);
        if (result!=null) {  // the cache contains mappings for this gene ID, but check that the GO information is also present if required (if not, remove the cache entry so that the resolver will contact the primary source again)
            if (includeGO) {
                Iterator<GeneIDmapping> iter=result.iterator();
                boolean updated=false;
                while (iter.hasNext()) {
                    GeneIDmapping entry=iter.next();
                    if (entry.GOterms==null || entry.GOterms.isEmpty()) {
                        iter.remove(); // remove cached mappings that do not include 
                        updated=true;
                    } 
                }
                if (updated) cacheUpdated=true;
            }
            return result;
        }
        else return new ArrayList<GeneIDmapping>(); // empty list
    }
    
    /** 
     * Parses the gene ID mapping table obtained from BioMart, and returns a list of GeneIDmappings
     * which might possibly be incomplete and contain duplicate entries (?)
     */    
    private ArrayList<GeneIDmapping> parseResults(ArrayList<String> table) throws ParseError {
        int numproperties=6;
        ArrayList<GeneIDmapping> result=new ArrayList<GeneIDmapping>(table.size());
        for (int i=0;i<table.size();i++) {
            String line=table.get(i);
            String[] elements=line.split("\t");
            // System.err.println(line);
            if (elements.length==1) {  // no tab separation probably means an error has occurred!
                if (line.trim().isEmpty()) { 
                    engine.logMessage("No gene ID matches found. Perhaps the settings are wrong");
                    throw new ParseError("No gene ID matches found. Perhaps the settings are wrong");                
                } else {
                    if (line.startsWith("ERROR:") || line.startsWith("Query ERROR:")) throw new ParseError(line);
                    else if (line.endsWith("NOT FOUND")) line="The selected ID type might not be applicable for the selected organism";
                    engine.logMessage("Unable to parse result: "+line);
                    //engine.logMessage("--- Full response ---",5);   
                    //logAll(table,5);
                    throw new ParseError("Unable to parse result: "+line);
                }
            }
            if (line.startsWith("ERROR:") || line.startsWith("Query ERROR:")) throw new ParseError(line);
            if (elements.length<numproperties) throw new ParseError("Expected "+numproperties+" columns in gene ID mapping table, got "+elements.length+": "+line); // allow for optional GO column(s)
            try {
                GeneIDmapping newmapping=null;
                int start=Integer.parseInt(elements[3]);
                int end=Integer.parseInt(elements[4]);
                int strand=Sequence.DIRECT;
                if (elements[5].startsWith("-") || elements[5].toLowerCase().startsWith("rev")) strand=Sequence.REVERSE;
                if (end<start) { // start after end marks reverse orientation
                    int swap=start; start=end; end=swap;
                    strand=Sequence.REVERSE;
                }
                String geneName=(elements[1].isEmpty())?elements[0]:elements[1];
                if (strand==Sequence.DIRECT) newmapping=new GeneIDmapping(elements[0], geneName, elements[2], start, end, strand);
                else newmapping=new GeneIDmapping(elements[0], geneName, elements[2], end, start, strand); // swap start and end coordinates
                String goTerms=null;
                if (elements.length>=7 && elements[6].startsWith("GO:")) { // several columns could include GO annotations. Merge them together
                    goTerms="";
                    for (int j=6;j<elements.length;j++) {
                        if (elements[j].startsWith("GO:")) goTerms=(goTerms+elements[j]+",");
                    }
                }
                if (goTerms!=null) { // since inclusion of GO terms can return the same mapping several times (with different GO) we must add the new GO to the existing mapping
                      GeneIDmapping present=getMappingFromList(result,newmapping);
                      if (present!=null) present.addGOterm(goTerms);
                      else result.add(newmapping);
                } else { // no goTerms
                    if (!containsMapping(result,newmapping)) result.add(newmapping);
                }
            } catch (NumberFormatException e) { throw new ParseError("Unable to parse expected numeric value: "+e.getLocalizedMessage()+" in gene ID mapping table");}
        }        
        return result;
    }

    private void logAll(ArrayList<String> lines, int level) {
        for (String line:lines) {
            engine.logMessage(line, level);
        }      
    }
    
    /** Retrieves a web page given an URL address using an HTTP POST-request */    
    private ArrayList<String> getPageUsingHttpPost(URL url, String message, int timeout) throws Exception {    
        ArrayList<String> document=new ArrayList<String>();
        InputStream inputStream = null;
        BufferedReader dataReader = null;
        PrintWriter writer = null;
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();
        connection.setConnectTimeout(timeout);

        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        connection.setRequestMethod("POST");
        //connection.setRequestProperty("Content-type", "text/xml; charset=UTF-8");  
        try {
            message="query="+message;
            writer = new PrintWriter(connection.getOutputStream());
            writer.println(message);
            writer.flush();
            //System.err.println("Contacting: "+url.toString()); System.err.println("\nQuery = "+message);
        } catch (Exception e) {throw e;}
        finally {
            if (writer!=null) writer.close();
        }  
        inputStream=connection.getInputStream();
        dataReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
           while ((line=dataReader.readLine())!=null){
              //System.err.println(line);
              document.add(line);
           }
        } catch (Exception e) {throw e;}
        finally {
            if (dataReader!=null) dataReader.close();
            if (inputStream!=null) inputStream.close();
        }    
        //System.err.println("\nResponse = "+document.toString());
        return document;
    }

    
    /** Retrieves a web page given an URL address using an http GET-request 
     *  This method has been deprecated as the use of POST-requests are preferred
     * @deprecated
     */
    private ArrayList<String> getPageUsingHttpGET(URL url, int timeout) throws Exception {
        ArrayList<String> document=new ArrayList<String>();
        InputStream inputStream = null;
        BufferedReader dataReader = null;
        URLConnection connection=url.openConnection();
        connection.setConnectTimeout(timeout);
        inputStream=connection.getInputStream();
        dataReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
           while ((line=dataReader.readLine())!=null){
              document.add(line);
           }
        } catch (Exception e) {throw e;}
        finally {
            if (dataReader!=null) dataReader.close();
            if (inputStream!=null) inputStream.close();
        }    
        return document;
    }
    
    
    
    /** Builds the XML query which is sent to the BioMart web service
     * @param IDformat
     * @param database
     * @param virtualSchema
     * @param configVersion
     * @param attributes An ordered list of BioMart attributes to return (in addition to gene ID)
     * @param IDs
     * @param transcript
     * @return 
     */
    private String getXMLQueryString(String IDformat, String database, String virtualSchema, String configVersion, String[] attributes, ArrayList<String> IDs, boolean transcript, boolean oldBioMartAttributes, boolean includeGO) {
        if (attributes==null || attributes.length==0) { // no explicit attributes? Use default
            attributes=(oldBioMartAttributes)?defaultBioMartAttributes_old:defaultBioMartAttributes_new;
        }
        if (virtualSchema==null || virtualSchema.isEmpty()) virtualSchema=defaultVirtualSchemaName;
        if (configVersion==null || configVersion.isEmpty()) configVersion=defaultDatasetConfigVersion;
        StringBuilder xml=new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<!DOCTYPE Query>");
        xml.append("<Query  virtualSchemaName = \"");
        xml.append(virtualSchema);
        xml.append("\" formatter = \"TSV\" header = \"0\" uniqueRows = \"0\" count = \"\" datasetConfigVersion = \"");			
        xml.append(configVersion);        
        xml.append("\" >");			      
        xml.append("<Dataset name = \"");
            xml.append(database);
            xml.append("\" interface = \"default\" >");
		xml.append("<Filter name = \"");
		xml.append(IDformat);
		xml.append("\" value = \"");
                for (int i=0;i<IDs.size()-1;i++) {
                    xml.append(IDs.get(i));
                    xml.append(",");
                }
                xml.append(IDs.get(IDs.size()-1));
		xml.append("\"/>");	               
                xml.append("<Attribute name = \"");
		xml.append(IDformat);
		xml.append("\" />");
                for (String attribute:attributes) {
                    if (attribute.startsWith("go_") && !includeGO) continue;
                    if (attribute.contains("/")) {
                       if (transcript) attribute=attribute.substring(attribute.indexOf('/')+1);
                       else attribute=attribute.substring(0,attribute.indexOf('/'));
                    }
                    xml.append("<Attribute name = \"");
                    xml.append(attribute);
                    xml.append("\" />");
                }                                                              
	   xml.append("</Dataset>");
       xml.append("</Query>");
       String xmlString=xml.toString();
       xmlString=xmlString.replaceAll(" ", "%20");
       return xmlString;
    }
    
    /** Stores a serialized version of an object to the specified file */
    private boolean storeObjectInFile(String filename, Object value) {
        ObjectOutputStream stream=null;
        try {
             stream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
             stream.writeObject(value);
             stream.close();         
        } catch (Exception e) {
                System.err.println("SYSTEM ERROR: GeneIDResolver.storeObjectInFile : "+e.getClass().toString()+"  : "+e.getMessage());
            return false;
        } finally {try {if (stream!=null) stream.close();} catch (Exception x){}}
        return true;
    }
    
    /** Fetches an object that has been serialized on file*/
    private Object getObjectFromFile(String filename) {
        ObjectInputStream stream=null;
        try {
             stream=new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)));
             Object value=stream.readObject();
             stream.close();
             return value;        
        } catch (Exception e) {
                System.err.println("SYSTEM ERROR: GeneIDResolver.getObjectFromFile :  "+e.getClass().toString()+"  : "+e.getMessage());
                return null;
        } finally {try {if (stream!=null) stream.close();} catch (Exception x){}}
    }
    
    
    /**
     * This method is used to "resolve" gene IDs based on information in a local file.
     * Each such file should contain information for only one genome build but it can
     * mix many different ID types. The format of the file is a simple TAB-delimited table
     * where each row has a number of column. The first column should be the ID type and
     * the second column should be the ID of a gene in that format. If the ID type in the first
     * column matches the given target format and the ID in the second column is one of the
     * IDs provided in the geneIDlist, the whole line (minus the first column) is added to 
     * the return set. This method does not consider the contents of the remaining fields, 
     * but to be compatible with the calling resolveIDlist() method, the columns should be in order
     * (including first column): gene ID type, gene ID, gene name, start, end, strand [, GO accessions list]
     * 
     * @param path
     * @param idformat
     * @param geneIDlist
     * @return
     * @throws Exception 
     */
    private ArrayList<String> resolveFromLocalFile(String path, String idformat, ArrayList<String> geneIDlist, boolean includeGO) throws Exception {    
        ArrayList<String> result=new ArrayList<String>(geneIDlist.size());
        File file=engine.getFile(path);
        if (!file.exists()) throw new ExecutionError("Gene information file not found: "+path);
        if (!file.canRead()) throw new ExecutionError("Unable to read gene information file: "+path);
        HashSet<String> ids=new HashSet<String>(geneIDlist);
        InputStream inputStream=null;
        BufferedReader dataReader=null;
        HashMap<String,Integer> columns=new HashMap<String, Integer>();
        String line;
        try {
           inputStream=MotifLabEngine.getInputStreamForFile(file);
           dataReader=new BufferedReader(new InputStreamReader(inputStream));
           line=dataReader.readLine();
           if (line!=null) { // the first line should be the header
               if (!(line.startsWith("#"))) throw new ExecutionError("Missing correct header at the start of gene information file");
               line=line.substring(1);
               if (line.endsWith("\t")) line=line+" "; // if the line ends with a tab (i.e. the last column in essentially empty), the split function below will not include this last column in the String[] so we add a space at the end to force the last column to be included anyway
               String[] fields=line.split("\t");
               for (int i=0;i<fields.length;i++) {
                   if (fields[i].equalsIgnoreCase("go") || fields[i].equalsIgnoreCase("go_id")) fields[i]="go";
                   columns.put(fields[i], i);
               }
               // check that we have the required fields
               String[] required=new String[]{"chr","start","end","gene name"};
               for (String req:required) {
                  if (!columns.containsKey(req)) throw new ExecutionError("Missing required column '"+req+"' in gene information file");
               }
               if (!columns.containsKey(idformat)) throw new ExecutionError("The selected organism has no information for ID type '"+idformat+"'");
           }
           // now process remaining lines
           boolean hasStrand=columns.containsKey("strand");
           boolean hasGO=(includeGO && columns.containsKey("go"));
           int idColumn=columns.get(idformat);
           while ((line=dataReader.readLine())!=null){ 
              if (line.isEmpty() || line.startsWith("#")) continue;
              String[] fields=line.split("\t");
              if (ids.contains(fields[idColumn])) {
                  StringBuilder builder=new StringBuilder();
                  builder.append(fields[idColumn]);
                  builder.append("\t");
                  builder.append(fields[columns.get("gene name")]);
                  builder.append("\t");
                  builder.append(fields[columns.get("chr")]);
                  builder.append("\t");
                  builder.append(fields[columns.get("start")]);
                  builder.append("\t");
                  builder.append(fields[columns.get("end")]);
                  builder.append("\t");
                  if (hasStrand) builder.append(fields[columns.get("strand")]);
                  else builder.append("1");
                  if (hasGO) builder.append(fields[columns.get("go")]);                  
                  // builder.append("\n"); // DO NOT ADD NEWLINE AS THIS WILL CAUSE PROBLEMS IN THE PARSER
                  result.add(builder.toString());
              }
           }
        } catch (Exception e) {throw e;}
        finally {
            if (dataReader!=null) dataReader.close();
            if (inputStream!=null) inputStream.close();
        }   
        return result;
    }
}