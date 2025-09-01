/*
 
 
 */

package org.motiflab.engine.datasource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.Organism;

/**
 * The DataTrack objects make up a set of precollected (feature) datatracks that
 * is available for use in the engine. The DataTrack objects itself provides a 
 * general description of the data, such as a descriptive (and unique) name, 
 * the data type (numeric, region, sequence) etc.
 * 
 * Associated with the each DataTrack object is a list of DataSource objects that specify
 * sources for this track for individual organisms (and/or mirror-sites). 
 * 
 * @author Kjetil Klepper
 */
public class DataTrack implements Cloneable {
    public static final String NUMERIC_DATA="numeric";
    public static final String REGION_DATA="region";
    public static final String SEQUENCE_DATA="sequence";
        
    private String name;
    private Class datatype; // RegionDataset.class, DNASequenceDataset.class or NumericDataset.class
    private String description;
    private String sourceSite;
    private String directivesProtocol;
    private ArrayList<DataSource> datasources; // 
    
    private static int counter=1;
    private int myindex=0;
    
    public DataTrack(String name,Class datatype,String sourceSite,String description) {
        this.name=name;
        this.datatype=datatype;
        this.sourceSite=sourceSite;
        this.description=description;
        this.directivesProtocol=null;
        datasources=new ArrayList<DataSource>();
        myindex=counter++;
    }
      
    
    /**
     * Returns the name for this DataTrack
     * @return
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the data type (as Class) for this DataTrack
     * @return
     */
    public Class getDataType() {
        return datatype;
    }
    
    /**
     * Returns a short description for this DataTrack
     * @return
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns a name of the source site for this Data Track (e.g. UCSC, NCBI, Ensembl)
     * @return
     */
    public String getSourceSite() {
        return sourceSite;
    }
    
    /**
     * Sets a new name for this DataTrack
     */
    public void setName(String newname) {
        name=newname;
    }
    
    /**
     * Sets a new data type for this DataTrack
     */
    public void setDataType(Class newclass) {
        datatype=newclass;
    }
    
    /**
     * Sets a short description for this DataTrack
     */
    public void setDescription(String newdescription) {
         description=newdescription;
    }
    
    /**
     * Sets a name of the source site for this Data Track (e.g. UCSC, NCBI, Ensembl)
     */
    public void setSourceSite(String newsource) {
        sourceSite=newsource;
    }
    
    
    @Override
    public String toString() {
        return name;
    }
    
    /** Associates a protocol script with this data track. By convention the protocol should be executed
     *  after the track has been loaded. Multiple commands can be used if separated by semi-colons.
     *  References to the new loaded track (whose name is not necessarily known) can be made by using
     *  the token '?'
     */
    public void setDisplayDirectivesProtocol(String protocol) {
        this.directivesProtocol=protocol;
    }
    
    public String getDisplayDirectivesProtocol() {
        return directivesProtocol;
    }
    
    public boolean hasDisplayDirectivesProtocol() {
        return (directivesProtocol!=null && !directivesProtocol.isEmpty());
    }    
    
    /**
     * Returns a list of available DataSource objects for the specified organism and genomebuild
     * or null if the organism/build is not supported
     * @param organism
     * @return
     */
    public DataSource[] getDataSources(int organism, String genomebuild) {
        int count=0;
        for (DataSource source:datasources) {
            if (source.getOrganism()==organism && source.getGenomeBuild().equals(genomebuild)) count++;
        }
        if (count==0) return null;
        DataSource[] sources=new DataSource[count];
        int i=0;
        for (DataSource source:datasources) {
            if (source.getOrganism()==organism && source.getGenomeBuild().equals(genomebuild)) {sources[i]=source;i++;}
        }
        return sources;
    }
         
    /**
     * Returns TRUE if this track contains a data source which is identical to the argument source
     * in terms of properties
     * @param source
     * @return 
     */
    public boolean containsDataSource(DataSource source) {
        for (DataSource s:datasources) {
            if (s.equals(source)) return true;
        }
        return false;
    }
    
    /**
     * Returns TRUE if this track contains at least one of the given sources
     * @param source
     * @return 
     */
    public boolean containsDataSource(Collection<DataSource> sources) {
        for (DataSource s:sources) {
            if (containsDataSource(s)) return true;
        }
        return false;
    }    
    
    /**
     * Removes all data sources from this track which has identical properties
     * to the argument source
     * @param source 
     */
    public void removeIdenticalDataSources(DataSource source) {
        Iterator<DataSource> iter=datasources.iterator();
        while (iter.hasNext()) {
            DataSource s=iter.next();
            if (s.equals(source)) iter.remove();
        }      
    }
    
    /**
     * Removes all data sources from this track which has identical properties
     * to any of the argument sources
     * @param source 
     */
    public void removeIdenticalDataSources(Collection<DataSource> sources) {
        for (DataSource source:sources) {
           removeIdenticalDataSources(source); 
        }    
    }    
    
    
    /** Returns an ordered list of data sources available for this DataTrack */
    public ArrayList<DataSource> getDatasources() {
        return datasources;
    }
    
    /**
     * Adds a new DataSource for this DataTrack to the end of the current list.
     * If other data sources already exists for the same organism/genome build
     * the new source will be treated as a "mirror" (backup data source).
     * Note that the new source will not be added if it is considered to be 
     * identical to one of the existing sources
     * @param source
     * @return
     */
    public void addDataSource(DataSource source) {
        if (!containsDataSource(source)) {
            source.setDataTrack(this);
            datasources.add(source);
        }
    }
    
    /**
     * Adds new DataSources for this DataTrack to the end of the current list.
     * If other data sources already exists for the same organism/genome build
     * the new sources will be treated as "mirrors" (backup data source).
     * However, new sources which are in every way identical to existing sources
     * will not be added
     * @param source
     * @return
     */
    public void addDataSources(Collection<DataSource> sources) {
        if (containsDataSource(sources)) { // there are duplicates so trim the collection
            removeIdenticalDataSources(sources);
        }
        for (DataSource source:sources) {
           source.setDataTrack(this);
        }
        datasources.addAll(sources);
    }    
    
    /**
     * Adds a new DataSource for this DataTrack to the beginning of the current list.
     * If other data sources already exists for the same organism/genome build
     * the new source will be treated as the preferred data source and the previous
     * sources will be treated as "mirrors".
     * Note that the new source will not be added if it is considered to be 
     * identical to one of the existing sources
     * @param source
     * @return
     */
    public void addPreferredDataSource(DataSource source) {        
        if (!containsDataSource(source)) {
            source.setDataTrack(this);
            datasources.add(0,source);
        }
    }
    
    /**
     * Adds new DataSources for this DataTrack to the beginning of the current list.
     * If other data sources already exists for the same organism/genome builds
     * the new sources will be treated as the preferred data sources and the previous
     * sources will be treated as "mirrors".
     * If some of the new sources are in every way identical to any existing sources
     * the old sources will be removed and replaced by the identical ones 
     * (which will be inserted at the beginning of the list).
     * @param source
     * @return
     */
    public void addPreferredDataSources(Collection<DataSource> sources) {
        if (containsDataSource(sources)) { // there are duplicates so trim the collection
            removeIdenticalDataSources(sources);
        }
        for (DataSource source:sources) {
           source.setDataTrack(this); // set "parent" track, just in case
        }       
        datasources.addAll(0,sources);
    }
    
    /** Removes all current data sources that have the same origin (organism/genome build) as the given data source */
    public void removeDataSourcesWithSameOrigin(DataSource source) {
        Iterator<DataSource> iter=datasources.iterator();
        while (iter.hasNext()) {
            DataSource old=iter.next();
            if (old.hasSameOrigin(source)) iter.remove();
        }
    }    
        
    /**
     * Replaces a DataSource for this DataTrack
     * If the old source is not present in the list, it will not be removed and 
     * the new source will be added to the end of the list
     * @param source
     * @return
     */
    public void replaceDataSource(DataSource oldsource, DataSource newsource) {
        int i=datasources.indexOf(oldsource);
        if (i>=0) datasources.set(i, newsource);
        else datasources.add(newsource);
    }
    
    /**
     * Removes all existing data sources with the same origin (organism/genome build)
     * as the new argument data source and replaces them with the new data source
     * @param newsource 
     */
    public void replaceDataSource(DataSource newsource) {
        removeDataSourcesWithSameOrigin(newsource);
        datasources.add(newsource);
    }
    
    /**
     * Removes all existing data sources with the same origins (organism/genome builds)
     * as one of the new argument data sources and adds these new sources to the configuration
     * @param newsources 
     */    
    public void replaceDataSources(Collection<DataSource> newsources) {
        for (DataSource source:newsources) {
            removeDataSourcesWithSameOrigin(source);
        }        
        datasources.addAll(newsources);
    }
    
    /**
     * Removes all existing data sources associated with this track
     * and replaces them with the newly provided sources
     * @param newsources 
     */    
    public void replaceAllDataSources(Collection<DataSource> newsources) {
        datasources.clear();
        datasources.addAll(newsources);
    }    
    
    /**
     * Reorders the DataSources by moving the source currently at 'fromIndex' 
     * to the new location at 'toIndex'
     * @param fromIndex
     * @param toIndex 
     */
    public void moveDataSource(int fromIndex,int toIndex) {
        DataSource moved=datasources.remove(fromIndex);
        datasources.add(toIndex, moved);
    }
        

    
    /**
     * Returns true if this DataTrack is available for the specified Organism and genomebuild
     * @param organism Taxonomy ID for the organism (e.g. human=9606)
     */
    public boolean isSupported(int organism, String genomebuild) {
        for (DataSource source:datasources) {
            String sourcebuild=source.getGenomeBuild();
            if (source.getOrganism()==organism && sourcebuild!=null && sourcebuild.equals(genomebuild)) return true;
        }
        return false;
    }
    
    /**
     * Returns true if this DataTrack is available for the specified genomebuild
     * @param genomebuild A string specifying the genome build (e.g. "hg18" or "mm9")
     */
    public boolean isSupported(String genomebuild) {
        for (DataSource source:datasources) {
            String sourcebuild=source.getGenomeBuild();
            if (sourcebuild!=null && sourcebuild.equals(genomebuild)) return true;
        }
        return false;
    }
    
    
    /**
     * Returns true if this DataTrack is available for all the genome builds in the list
     * If the list is empty the return value is FALSE
     * @param builds List of genome builds for the organisms
     */
    public boolean isSupported(String[] builds) {
        if (builds==null || builds.length==0) return false;
        for (String build:builds) {
            if (!isSupported(build)) return false;
        }
        return true;
    }
    
    /**
     * Returns a list of taxonomy IDs for the organisms that are supported for this DataTrack
     * @return
     */
    public int[] getSupportedOrganisms() {
        if (datasources==null) return new int[0];
        int size=datasources.size();
        HashSet<Integer> map=new HashSet<Integer>(3);
        for (DataSource source:datasources) {
            map.add(new Integer(source.getOrganism()));
        }
        int[] result=new int[map.size()];
        int i=0;
        for (Integer integer:map) {
            result[i]=integer.intValue();
            i++;
        }
        return result;
    }
    
    public String getSupportedOrganismsAsString() {
        ArrayList<String> organisms=new ArrayList<String>();
        for (DataSource source:datasources) {
            String organism=Organism.getCommonName(source.getOrganism());
            if (organism!=null && !organisms.contains(organism)) organisms.add(organism);
        }        
        Collections.sort(organisms);
        return MotifLabEngine.splice(organisms, ",");
    }
    
    /** 
     * Returns a sorted list of 
     * @param organism
     * @return 
     */
    public ArrayList<String> getSupportedBuildsForOrganism(int organism) {  
        ArrayList<String> builds=new ArrayList<String>();
        for (DataSource source:datasources) {
            if (source.getOrganism()==organism) {
                String sourcebuild=source.getGenomeBuild();
                if (sourcebuild!=null && !builds.contains(sourcebuild)) builds.add(sourcebuild);
            }
        }
        Collections.sort(builds);
        return builds;        
    }
    
    
    /** Returns a sorted list of all genome builds that are supported for this DataTrack */
    public String[] getSupportedGenomeBuilds() {
        ArrayList<String> supported=new ArrayList<String>();
        for (DataSource source:datasources) {
            String sourcebuild=source.getGenomeBuild();
            if (sourcebuild!=null && !supported.contains(sourcebuild)) supported.add(sourcebuild);
        }        
        Collections.sort(supported);
        String[] ar=new String[supported.size()];
        return supported.toArray(ar);
    }
    
    /**
     * Returns a String tooltip describing all organisms and genome builds supported for this DataTrack
     * The tooltip string contains a HTML-formatted and sorted list of all organisms
     * with the supported genome builds in parenthesis behind the organism name
     * E.g.
     *       Human (hg18, hg19)
     *       Mouse (mm8,mm9)
     *       Rat   (rn3)
     * 
     * @param organism If provided, only the organisms in that list will be included.
     *                 If this parameter is null, all supported organisms will be included
     * @return 
     */
    public String getSupportedGenomeBuildsTooltip(int[] organisms) {
        if (organisms==null) organisms=getSupportedOrganisms();
        ArrayList<String> list=new ArrayList<String>(organisms.length);
        for (int i=0;i<organisms.length;i++) {
           String commonName=Organism.getCommonName(organisms[i]);
           ArrayList<String> builds=getSupportedBuildsForOrganism(organisms[i]);
           list.add(commonName+" ("+MotifLabEngine.splice(builds, ",")+")");
        }
        Collections.sort(list);
        StringBuilder tooltip=new StringBuilder();
        tooltip.append("<html>");
        for (String string:list) {
            tooltip.append(string);
            tooltip.append("<br>");
        }
        tooltip.append("</html>");     
        return tooltip.toString();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public DataTrack clone() {
        DataTrack copy=new DataTrack(this.name, this.datatype, this.sourceSite, this.description);
        copy.directivesProtocol=this.directivesProtocol;
        copy.datasources=new ArrayList<DataSource>(this.datasources.size());
        for (DataSource source:this.datasources) {
            DataSource clonedsource=(DataSource)source.clone();
            clonedsource.dataTrack=copy;
            copy.datasources.add(clonedsource);
        }
        return copy;
    }
    
    /** Returns a copy of this track but replaces the current datasources with the ones provided */
    public DataTrack cloneWithNewSources(ArrayList<DataSource> newsources) {
        DataTrack copy=new DataTrack(this.name, this.datatype, this.sourceSite, this.description);   
        copy.directivesProtocol=this.directivesProtocol;
        for (DataSource source:newsources) {
            source.dataTrack=copy;
            copy.datasources.add(source);
        }
        return copy;        
    }

     /** Returns an XML element for this DataTrack that can be included in DataConfiguration XML-documents */
    public org.w3c.dom.Element getXMLrepresentation(org.w3c.dom.Document document) {
        org.w3c.dom.Element element = document.createElement("DataTrack");
        String tracktype="";
             if (datatype==DNASequenceDataset.class) tracktype=SEQUENCE_DATA;
        else if (datatype==NumericDataset.class) tracktype=NUMERIC_DATA;
        else if (datatype==RegionDataset.class) tracktype=REGION_DATA;
        element.setAttribute("name", name);
        element.setAttribute("type", tracktype);
        element.setAttribute("source", sourceSite);
        element.setAttribute("description", description);
        for (DataSource source:datasources) {
            org.w3c.dom.Element child=source.getXMLrepresentation(document);
            element.appendChild(child);
        }
        if (directivesProtocol!=null) {
            org.w3c.dom.Element child=document.createElement("DisplayDirectives");
            child.setTextContent(directivesProtocol);                  
            element.appendChild(child);            
        }
        return element;
    }      
    
    
    public void debug() {
        System.err.println("Track["+myindex+"]: "+name+" ["+datatype.toString()+"] {"+sourceSite+"} "+description);
          for (DataSource source:datasources) {
              source.debug();
          }
        
    }
    
}
