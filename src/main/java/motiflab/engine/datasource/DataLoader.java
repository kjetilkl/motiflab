/*
 
 
 */

package motiflab.engine.datasource;

import motiflab.engine.task.ExecutableTask;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import motiflab.engine.Cache;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.ParameterSettings;
import motiflab.engine.SystemError;
import motiflab.engine.data.*;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.protocol.ParseError;


/**
 * This class supports loading of registered feature datatracks from Data sources
 * or loading of other data items from data repositories (files, web and other databases)
 * @author kjetikl
 */
public class DataLoader {
    private MotifLabEngine engine;
    private HashMap<String,Server> servers; // the key is the serveraddress
    private HashMap<String,DataTrack> availableTracks; // the key is the datatrack name
    private DataConfiguration dataconfiguration;
    private Cache cache=null;
    private boolean useCache=true;
    private ExecutorService threadpool=null;
    private ExecutorService saveDatathreadpool=null; // this thread will be used to save data back to cache (independent of the downloading threads)
    
    public DataLoader(MotifLabEngine engine) {
        this.engine=engine;      
        servers=new HashMap<String, Server>();
        availableTracks=new HashMap<String, DataTrack>();
        dataconfiguration=new DataConfiguration(availableTracks, servers);
        saveDatathreadpool=Executors.newSingleThreadExecutor();
        DataSource.setDataLoader(this); // the DataLoader property is static in DataSource
    }
    
    /** Returns a reference to the file that contains the XML formatted file of available datatracks
     * (and other settings) which should be read into a DataConfiguration object
     */
    public File getDataConfigurationFile() {
        String filename=engine.getMotifLabDirectory()+File.separator+"DataTracks.xml";
        return new File(filename);
    }
    
    /** Specifies a cache directory to use for this dataloader */
    public void setCacheDirectory(String cacheDirectory) {
        cache=new Cache(engine, cacheDirectory);
    }
    
    public MotifLabEngine getEngine() {
        return engine;
    }
    
    /** Specifies whether this DataLoader should make use of a local disc cache to reduce connections to external DataSources */
    public void setUseCache(boolean flag) {
        useCache=flag;
    }
    
    /** Clears the installed Cache for this DataLoader
     * @return TRUE if the cached was successfully clear (or if no cached was installed) or
     * FALSE if the installed cached could not be successfully cleared
     */
    public boolean clearCache() {
        if (cache!=null) return cache.clearCache();
        else return true;
    }
    
    /** Sets the maximum number of data downloads that can be performed concurrently */
    public void setConcurrentDownloads(int max) throws SystemError {
        int upperlimit=DataLoader.getUpperLimitOnConcurrentDownloads();
        if (max<1 || max>upperlimit) throw new SystemError("The maximum number of concurrent downloads must be between 1 and "+upperlimit);
        if (threadpool!=null) {
            if (threadpool instanceof ThreadPoolExecutor) {
               int active=((ThreadPoolExecutor)threadpool).getActiveCount(); 
               if (active>0) throw new SystemError("The number of concurrent downloads cannot be changed while downloads are in progress");
            }
        }
        if (threadpool!=null) threadpool.shutdownNow(); // this will probably abort all downloads in progress
        // if (max==1) threadpool=null; // do not use threadpools for serial download (one at a time)
        threadpool=Executors.newFixedThreadPool(max);        
    }
    
    /** Returns an absolute upper limit on the number of concurrent downloads allowed.
     *  The user can select the number of concurrent downloads to be a number between 1
     *  and this upper limit with the setConcurrentDownloads() method
     */
    public static int getUpperLimitOnConcurrentDownloads() {
        return 20;
    }
        
    public void notifyDataSourceAccess(String serveraddress, Date timestamp) {
        if (serveraddress!=null) {
            Server server=servers.get(serveraddress); // if the server adress is e.g. a local file, the server can be NULL
            if (server!=null) {
                synchronized(server) {
                    server.setTimeOfLastAccess(timestamp);
                }
            }
        }
    }
    
    /** 
     * This method will return the number of milliseconds one should wait before accessing the 
     * given server again. The number is based on the time of last access (as registered by 
     * the last call to notifyDataSourceAccess(serveraddress)) and the "delay" property for this server
     */
    public int getWaitingPeriodForServer(String serveraddress) {
        if (serveraddress==null) return 0;
        Server server=servers.get(serveraddress);
        if (server==null) return 0;    
        Date lastDate=server.getTimeOfLastAccess();
        if (lastDate==null) return 0;
        long lasttime=lastDate.getTime();
        long timeout=lasttime+server.getServerDelay();
        long now=new Date().getTime();
        if (now>timeout) return 0; // no need to wait
        else return (int)(timeout-now);
    }    
    
    /**
     * Returns the "max sequence length" property for the given server
     * @param serveraddress
     * @return 
     */
    public int getMaxSequenceLengthForServer(String serveraddress) {
        Server server=servers.get(serveraddress);
        int maxSegmentSize=(server!=null)?server.getMaxSequenceLength():0; 
        return maxSegmentSize;
    }
    
    /**
     * Returns an array containing all servers that have specific settings
     * @return
     */
    public ArrayList<Server> getAvailableServersAsList() {
        ArrayList<Server> serverlist=new ArrayList<Server>(servers.size());
        Set<String> keys=servers.keySet();
        for (String key:keys) {
            serverlist.add(servers.get(key));
        }
        return serverlist;
    }
    
    /**
     * Returns the datatrack for the given name (or null of not such track is known)
     * @return a DataTrack
     */
    public DataTrack getDataTrack(String trackname) {
        return availableTracks.get(trackname);
    }    
    
    /**
     * Returns an array containing all datatracks that are available to the system
     * (according to a list of predefined datasets)
     * @return
     */
    public DataTrack[] getAvailableDatatracks() {
        DataTrack[] tracks=new DataTrack[availableTracks.size()];
        Set<String> keys=availableTracks.keySet();
        int i=0;
        for (String key:keys) {
            tracks[i]=availableTracks.get(key);
            i++;
        }
        return tracks;
    }
    /**
     * Returns an array containing all datatracks that are availble to the system
     * (according to a list of predefined datasets)
     * @return
     */
    public ArrayList<DataTrack> getAvailableDatatracksAsList() {
        ArrayList<DataTrack> tracks=new ArrayList<DataTrack>(availableTracks.size());
        Set<String> keys=availableTracks.keySet();
        for (String key:keys) {
            tracks.add(availableTracks.get(key));
        }
        return tracks;
    }
    
    /**
     * Returns an array containing all datatracks of the given type that are 
     * availble to the system (according to a list of predefined datasets)
     * @return
     */
    public DataTrack[] getAvailableDatatracks(Class datatype) {
        int size=0;
        Set<String> keys=availableTracks.keySet();
        for (String key:keys) {
            if (availableTracks.get(key).getDataType()==datatype) size++;
        }
        DataTrack[] tracks=new DataTrack[size];
        int i=0;
        for (String key:keys) {
            DataTrack track=availableTracks.get(key);
            if (track.getDataType()==datatype) {tracks[i]=track;i++;}
        }
        return tracks;
    }
    
    
    
    /** Replaces the current tracks database with the given configuration */
    public void installConfiguration(DataConfiguration newconfig) {
        dataconfiguration=newconfig;
        servers=newconfig.getServers();
        availableTracks=newconfig.getAvailableTracks();
    }

    /** Returns the current DataConfiguration (not a copy!) */
    public DataConfiguration getCurrentDataConfiguration() {
        return dataconfiguration;
    }
    
    /**
     * Loads the selected Data Track from available Data Sources and returns a FeatureDataset
     * If caching is used, the method first examines if any parts of the data
     * can be found in the cache. Data which is not available from cache is then
     * loaded from primary source(s). 
     * @param track
     * @return
     * @throws ExecutionError
     */
    @SuppressWarnings("unchecked")
    public FeatureDataset loadDataTrack(String datatrackName, ExecutableTask task) throws Exception {       
        DataTrack datatrack=availableTracks.get(datatrackName);
        if (datatrack==null) throw new ExecutionError("Unknown DataTrack: "+datatrackName);
        SequenceCollection allSequences=engine.getDefaultSequenceCollection();
        ArrayList<Sequence>list=allSequences.getAllSequences(engine);
        FeatureDataset dataset=null;
             if (datatrack.getDataType()==DNASequenceDataset.class) dataset=new DNASequenceDataset(datatrackName);
        else if (datatrack.getDataType()==NumericDataset.class) dataset=new NumericDataset(datatrackName);
        else if (datatrack.getDataType()==RegionDataset.class) dataset=new RegionDataset(datatrackName);
        else throw new ExecutionError("Unknown DataTrack type: "+datatrack.getDataType());
        task.setProgress(0);
        if (threadpool!=null) {// obtain data for sequences concurrently (parallel download)
            int[] counters=new int[]{0,0,list.size()}; // counters[0]=#downloads started, [1]=#downloads completed, [2]=#total number of sequences
            ArrayList<DownloadSequenceDataTask<FeatureSequenceData>> downloadtasks=new ArrayList<DownloadSequenceDataTask<FeatureSequenceData>>(list.size());
            for (Sequence sequence:list) downloadtasks.add(new DownloadSequenceDataTask(datatrack, sequence, task, counters)); 
            List<Future<FeatureSequenceData>> futures=null;
            try {
                futures=threadpool.invokeAll(downloadtasks); // this call apparently blocks until all tasks finish (either normally, by exceptions or being cancelled)
            } catch (Exception e) {
               throw e;
            }
            int countOK=0;
            for (Future<FeatureSequenceData> future:futures) {
                if (future.isDone() && !future.isCancelled()) {
                    try {
                        dataset.addSequence(future.get());
                        countOK++;
                    } catch (java.util.concurrent.ExecutionException e) {
                        Throwable cause=e.getCause();
                        if (cause instanceof Exception) throw (Exception)cause;
                        else if (cause instanceof OutOfMemoryError) throw new ExecutionError("Out of memory error", cause);
                        else throw new ExecutionError(cause.getMessage(), cause);
                    } catch (Error ex) {
                        throw new ExecutionError(ex.getMessage(), ex.getCause()); 
                    }
                }
            }
            if (countOK!=list.size()) throw new ExecutionError("Some mysterious error occurred during data download");
        } else { // obtain data for each sequence in turn (serial download)
            int counter=0;
            int size=list.size();
            for (Sequence sequence:list) { 
                counter++;
                FeatureSequenceData sequencedata=getSequenceData(datatrack, sequence, datatrackName, task, counter, size);           
                dataset.addSequence(sequencedata);
                task.setProgress(counter,size);
            }        
        }
        // try converting Region Datasets to Motif or Module track if possible
        if (dataset instanceof RegionDataset) { 
             DNASequenceDataset DNAtrack=null;
             Object standard=engine.getDataItem("DNA");
             if (standard instanceof DNASequenceDataset) DNAtrack=(DNASequenceDataset)standard;
             else {
                 ArrayList<Data> allDNAtracks=engine.getAllDataItemsOfType(DNASequenceDataset.class);
                 if (allDNAtracks.size()==1) DNAtrack=(DNASequenceDataset)allDNAtracks.get(0);
             }
             boolean  ok=engine.convertRegionTrackToMotifTrack((RegionDataset)dataset, null, DNAtrack, false); // "false" = Do not force conversion but check the track first
             if (!ok) ok=engine.convertRegionTrackToModuleTrack((RegionDataset)dataset, DNAtrack, false);  // "false" = Do not force conversion but check the track first
             if (!ok) ok=engine.convertRegionTrackToNestedTrack((RegionDataset)dataset);
        }        
        return dataset;        
    }
    
    /**
     * Obtains and returns the FeatureSequenceData for the given track and sequence
     * by querying the available data sources for that track
     * @param datatrack
     * @param sequence
     * @param datatrackName
     * @param task
     * @param counter A running-number for this sequence relative to the total number of sequences to download in one task (used for progress reporting)
     * @param size The total number of sequences to download in one task (used for progress reporting)
     * @return
     * @throws Exception 
     */
    private FeatureSequenceData getSequenceData(DataTrack datatrack, Sequence sequence, String datatrackName, ExecutableTask task, int counter, int size) throws Exception { 
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) { throw new InterruptedException();}           
        task.setStatusMessage("Loading "+datatrackName+" for "+sequence.getName()+" ("+counter+"/"+size+")");
        engine.logMessage("["+counter+"/"+size+"]  Loading "+datatrackName+" for "+sequence.getName(),5);
        int organism=sequence.getOrganism();
        String genomebuild=sequence.getGenomeBuild();
        FeatureSequenceData sequencedata=null;
             if (datatrack.getDataType()==DNASequenceDataset.class) sequencedata=new DNASequenceData(sequence,'N');
        else if (datatrack.getDataType()==NumericDataset.class) sequencedata=new NumericSequenceData(sequence,0);
        else if (datatrack.getDataType()==RegionDataset.class) sequencedata=new RegionSequenceData(sequence);
        DataSource[] sources=getDataSourcesWithMirrors(datatrack,organism,genomebuild);
        if (sources==null || sources.length==0) throw new ExecutionError("Unsupported organism/genomebuild '"+Organism.getCommonName(organism)+":"+genomebuild+"' (sequence "+sequence.getName()+") for DataTrack="+datatrackName);            
        
        // start with one segment for the whole sequence, then obtain data or split into smaller segments as necessary
        ArrayList<DataSegment> segments=new ArrayList<DataSegment>();   
        segments.add(new DataSegment(datatrackName, organism, genomebuild, sequence.getChromosome(), sequence.getRegionStart(), sequence.getRegionEnd(), null));

        boolean cacheAlreadyAccessed=false; // only ever access the cache once if necessary.
        boolean oneHitWonder=false; // this will be set to true if the entire sequence is obtained as a single segment from the cache. In this case it will not be necessary to write it back again to the cache
        
        int sourceIndex=0; // try sources one by one
        
        while (!allSegmentsComplete(segments)) {
            //engine.logMessage("DEBUG: sequence["+sequence.getName()+"] segments completed "+countCompletedSegments(segments)+" of "+segments.size());
            if (useCache && cache!=null && sources[sourceIndex].useCache() && !cacheAlreadyAccessed) { // try to retrieve data from cache if the source recommends it. (But only do this once!)
                // The cache will fill in data for empty segments if it is available.
                // If only parts of a segment is covered by the cache, the segment will be split up.
                // (Hence, the provided segments-list can be altered by the method).
                try {cache.loadCachedData(sequencedata, segments, datatrackName, organism, genomebuild);} // this can fail, but it does so "gracefully" (as if the cache did not contain relevant data)
                catch (SystemError e) {engine.logMessage(e.toString(),30);}                                
                int completed=countCompletedSegments(segments);
                oneHitWonder=(segments.size()==1 && completed==1); // the entire sequence was retrieved as one segment from the cache. No need to save the data back again to the cache
                cacheAlreadyAccessed=true; // do not allow the cache to be used more than once per sequence
                // perhaps the new segments should be optimized here?
                mergeEmptySegments(segments);                
            }
            // check if some of the remaining empty segments need to be split because they are too long for the server
            int maxSegmentSize=getMaxSequenceLengthForServer(sources[sourceIndex].getServerAddress());
            if (maxSegmentSize>0) splitEmptySegments(segments, maxSegmentSize); // this can change the current segments list
            
            // now go through each remaining (empty) segment in turn and try to obtain it from the current data source. If a source fails, go to the next source and try the whole process again
            for (DataSegment segment:segments) {
                if (segment.containsData()) continue; // skip segments already completed
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException(); // user has requested to abort the task
                try {
                    obtainDataSegmentFromSource(segment, datatrack, sources[sourceIndex], counter, size, sequence.getName(), task);
                    if (segment.isEmpty()) { // The segment is empty but no errors occurred, perhaps because the segment has no data associated with it
                        segment.initializeDefault(datatrack.getDataType()); // just fill in with default data so it will not be processed again
                    } 
                 } catch (Exception e) {
                   if (e instanceof InterruptedException) throw e; // the user tries to abort. Do not ignore these! 
                   // Something went wrong when obtaining data from the current data source. 
                   // If we have alternative sources left we will try those, else we throw an exception
                   sourceIndex++;
                   if (sourceIndex==sources.length) throw e; // we have exhausted the available data sources. Nothing more we can do now than throw an exception
                   engine.logMessage("["+counter+"/"+size+"]  Loading "+datatrackName+" for "+sequence.getName()+":  trying mirror site at "+sources[sourceIndex].getServerAddress(),10);
                   mergeEmptySegments(segments); // merge back empty segments that might have been split up for the benefit of the previous data source
                   break; // this break will continue from the outer while-loop, possibly querying the cache (if it has not been accessed already) or splitting the segments if necessary for the next source
                }
            }
            
        }   
        // transfer the data from the segments to the sequence
        for (DataSegment segment:segments) { 
            if (segment.containsData()) segment.copyData(sequencedata);
        }        
        // Save the obtained data in the cache if applicable. Note that data will not be saved if something has gone wrong (if an exception has been thrown this point will not be reached)
        if (useCache && cache!=null && shouldSaveToCache(segments) && !oneHitWonder) {
            saveDatathreadpool.execute(new saveDataRunnable(datatrackName, organism, genomebuild, sequencedata)); // save on separate thread
            // cache.saveData(datatrackName, organism, genomebuild, sequencedata); // save in this thread
        } 
        return sequencedata;
    }
            
    /** Returns TRUE if the feature data for this track/sequence should be stored in the cache 
      */
    private boolean shouldSaveToCache(ArrayList<DataSegment> segments) {
        for (DataSegment segment:segments) {
            if (segment.shouldSaveToCache()) return true; // store the whole sequence in the cache if one segment requests it
        }
        return false;
    }
    
    private boolean allSegmentsComplete(ArrayList<DataSegment> segments) {
        for (DataSegment segment:segments) {
            if (segment.isEmpty()) return false;
        }
        return true;
    }  
    
    private int countCompletedSegments(ArrayList<DataSegment> segments) {
        int count=0;
        for (DataSegment segment:segments) {
            if (segment.containsData())count++;
        }
        return count;
    }      
    
    public static void debugSegments(ArrayList<DataSegment> segments) {
        for (int i=0;i<segments.size();i++) {
            DataSegment segment=segments.get(i);
            System.err.println("SEGMENT["+(i+1)+"/"+segments.size()+"] "+segment.toString());
        }        
    }
    
    
    /** 
     * Optimizes a list of DataSegments that spans a genomic region.
     * Some of the segments might represent data loaded from cache while others
     * just contain empty placeholder for which data must be obtained from 
     * original DataSources. This method prunes the list by possibly deleting
     * cached segments and merging flanking empty segments into single larger
     * segments in order to reduce the number of DataSource connections that
     * has to be made (while possibly increasing the amount of data that has to
     * be loaded anew, since it is generally considered to be more efficient to 
     * load a larger amount of data in fewer attempts rather than to load many 
     * small segments one at a time). Note that the argument ArrayList might be
     * changed as a result of this optimization. Note also that the optimization
     * might not be "optimal" in the classic sense of the word.  
     * The present implementation simply finds the first "empty" segment (which
     * must be loaded from original DataSource) and the last empty segment in 
     * the list and (provided that they are not the same) merge these two and
     * all segments in between into a single continuous empty segment
     */
    private void optimizeSegments(ArrayList<DataSegment> segments) {
        int first=-1; int last=-1;
        for (int i=0;i<segments.size();i++) {
            DataSegment segment=segments.get(i);
            if (segment.containsData()) continue;
            else {
                if (first<0) first=i;
                else last=i;
            }
        }
        // merge a region starting and ending with empty segments (possibly discarding data in the middle)
        if (first>0 && last>0) { // two non-identical empty segments have been found
            int endcoordinate=segments.get(last).getSegmentEnd();
            segments.get(first).setSegmentEnd(endcoordinate);
            int len=last-first;
            for (int j=0;j<len;j++) {segments.remove(first+1);} // remove "same" coordinate j times 
        }
    }
    
    /**
     * Goes through the list of segments and checks the size of all currently
     * empty segments (those already containing data are skipped). If an empty segment
     * is larger than the given maxLength, it is split into several smaller segments
     * of about equal size (each one no longer than maxLength).
     * 
     * @param segments A list of segments which may be updated by the method
     * @param maxLength 
     */
    private void splitEmptySegments(ArrayList<DataSegment> segments, int maxLength) {     
        for (int i=0;i<segments.size();i++) {
            DataSegment segment=segments.get(i);
            if (segment.containsData()) continue; // don't split segments that have already been filled with data
            if (segment.getSize()>maxLength) { // this empty segment should be split up
                int segmentSize=segment.getSize();
                int parts=(int)Math.ceil((double)segmentSize/(double)maxLength);
                int subsegmentsize=(int)Math.ceil((double)segmentSize/(double)parts);
                //engine.logMessage("Splitting segment of size ("+segmentSize+") into "+parts+" segments of size "+subsegmentsize);
                segments.remove(i);
                int j=i;
                int offset=0;
                for (;j<i+parts;j++) {
                    DataSegment subsegment=segment.getEmptySubSegment(offset, subsegmentsize);
                    segments.add(j, subsegment);
                    offset+=subsegmentsize;
                }
                i=j; 
            }
        }
    }    
    
    /**
     * Goes through the list of segments and checks if it contains two or more consecutive empty segments. 
     * If such segments are encountered, they will be merged.
     * @param segments A list of segments which may be updated by the method
     * @param maxLength 
     */
    private void mergeEmptySegments(ArrayList<DataSegment> segments) {     
        for (int i=0;i<segments.size();i++) {
            DataSegment segment=segments.get(i);
            if (segment.containsData()) continue; // don't merge segments that have already been filled with data
            int mergeUpTo=i;
            for (int j=i+1;j<segments.size();j++) {
                DataSegment nextsegment=segments.get(j);
                if (nextsegment.containsData()) break;
                mergeUpTo=j;         
            }
            if (mergeUpTo>i) {
                DataSegment lastSegment=segments.get(mergeUpTo);
                segment.setSegmentEnd(lastSegment.getSegmentEnd()); // update the end of the first segment to the end of the last (so that the first segment spans all the segments)
                int numberOfSegmentsToRemove=mergeUpTo-i;
                for (int j=0;j<numberOfSegmentsToRemove;j++) {
                    segments.remove(i+1); // remove the same segment index each time (since the list is altered)
                }
            }
        }
    }       
    
    /** 
     * Obtains (uncached) DataTrack data for a segment using 
     * any one of the available applicable sources for that DataTrack 
     * If a data source throws an exception this is suppressed and the next source
     * in line is tried instead. However, if the source is the last in line so the
     * data could not be obtained, an exception is thrown
     * @param segment
     * @param datatrack
     * @param organism
     * @param genomebuild
     * @param counter
     * @param size
     * @param sequenceName
     * @param task
     * @return
     * @throws Exception 
     */
    @Deprecated
    private DataSegment obtainDataSegment(DataSegment segment, DataTrack datatrack, int organism, String genomebuild, int counter, int size, String sequenceName, ExecutableTask task) throws Exception {
        //engine.logMessage("Loading '"+datatrack+"' from DataSource in "+segment.getChromosome()+":"+segment.getSegmentStart()+"-"+segment.getSegmentEnd()); 
        DataSource[] sources=getDataSourcesWithMirrors(datatrack,organism,genomebuild);
        String datatrackName=datatrack.getName();
        if (sources==null || sources.length==0) throw new ExecutionError("Unsupported organism/genomebuild '"+Organism.getCommonName(organism)+":"+genomebuild+"' (sequence "+sequenceName+") for DataTrack="+datatrackName);            
        boolean ok=false;
        int i=0;
        while(!ok && i<sources.length) { // try to obtain sequence data from different sources (mirrors)            
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) { throw new InterruptedException();}           
            DataSource source=sources[i];
            if (i>0) engine.logMessage("["+counter+"/"+size+"]  Loading "+datatrackName+" for "+sequenceName+":  trying mirror site at "+source.getServerAddress(),20);
            try {
                segment=source.loadDataSegment(segment, task); // this will wait a required time if necessary after previous access to server
                ok=true; // datatrack has been loaded for this sequence. Proceed with next sequence
            } catch (ParseError e) { // data was loaded but could not be parsed
                engine.logMessage("["+counter+"/"+size+"]  Loading "+datatrackName+" for "+sequenceName+":  ERROR: Unable to parse data file: "+e.getMessage(),30);                
                if (i==sources.length-1) {throw new ExecutionError("Unable to parse data file:\n"+e.getMessage(), e);}
                // just try next source/mirror
            } catch (SocketTimeoutException e) {
                engine.logMessage("["+counter+"/"+size+"]  Loading "+datatrackName+" for "+sequenceName+":  ERROR: Network timeout!",30);
                if (i==sources.length-1) {throw new ExecutionError("Network timeout",e);}
                // just try next source/mirror
            } catch (InterruptedException e) {
                throw e; // user aborted
            } catch (Exception e) {
                if (e instanceof NullPointerException) e.printStackTrace(System.err);
                engine.logMessage("["+counter+"/"+size+"]  Loading "+datatrackName+" for "+sequenceName+":  ERROR: "+e.getClass().getSimpleName()+":"+e.getMessage(),30);
                if (i==sources.length-1) {throw new ExecutionError(e.getMessage(), e);}
                // just try next source/mirror
            } 
            i++; // try next source/mirror
        }
        return segment;
    }
    
    /**
     * Tries to obtain the given data segment from a single data source
     * @param segment
     * @param datatrack
     * @param source
     * @param counter
     * @param size
     * @param sequenceName
     * @param task
     * @param ignoreErrors
     * @throws Exception 
     */
    private void obtainDataSegmentFromSource(DataSegment segment, DataTrack datatrack, DataSource source, int counter, int size, String sequenceName, ExecutableTask task) throws Exception {
        String datatrackName=datatrack.getName();
        try {
            // engine.logMessage("DEBUG: Obtaining segment data from: "+source.getServerAddress());
            source.loadDataSegment(segment, task); // this will wait a required time if necessary after previous access to server
            segment.setSaveToCache(source.useCache());
        } catch (ParseError e) { // data was loaded but could not be parsed
            engine.logMessage("["+counter+"/"+size+"]  Loading "+datatrackName+" for "+sequenceName+":  ERROR: Unable to parse data file: "+e.getMessage(),30);
            throw new ExecutionError("Unable to parse data file:\n"+e.getMessage(), e);
        } catch (SocketTimeoutException e) {
            engine.logMessage("["+counter+"/"+size+"]  Loading "+datatrackName+" for "+sequenceName+":  ERROR: Network timeout!",30);
            throw new ExecutionError("Network timeout",e);
        } catch (InterruptedException e) {
            throw e; // user aborted the task. Do not ignore this exception
        } catch (Exception e) {
            if (e instanceof NullPointerException || e instanceof StringIndexOutOfBoundsException) { // probably due to bad coding :-|
                //e.printStackTrace(System.err);
                engine.reportError(e);
            }
            engine.logMessage("["+counter+"/"+size+"]  Loading "+datatrackName+" for "+sequenceName+":  ERROR: "+e.getClass().getSimpleName()+":"+e.getMessage(),30);
            throw new ExecutionError(e.getMessage(), e);
        }   
    }    
    
    /** Returns a list of DataSources for the given DataTrack and genome build (including sources based on "mirror servers")
     *  The sources will be listed in their preferred order as specified in the data sources configuration for the track.
     *  However, if a data source is associated with a server that has registered mirrors (according to the Server configuration)
     *  the source will repeated multiple times in a row (one for each mirror including the original server), before the next
     *  independent data source.
     */
    private DataSource[] getDataSourcesWithMirrors(DataTrack datatrack, int organism, String genomebuild) {
        DataSource[] sources=datatrack.getDataSources(organism,genomebuild);
        if (sources==null) return null;
        ArrayList<DataSource> list=new ArrayList<DataSource>();
        for (DataSource source:sources) {
            // list.add(source); // I will not add the original source first to the list, since if it is present in the mirrors list that could be a sign that other mirrors could be preferred over the original source 
            String serveraddress=source.getServerAddress();
            if (servers.containsKey(serveraddress)) { 
                Server server=servers.get(serveraddress);
                ArrayList<String> mirrors=server.getMirrorSites();
                if (mirrors!=null) { // the source has mirrors
                    if (!mirrors.contains(serveraddress)) list.add(source); // original server is not listed in mirrors. Add original source first. Note that if the original server IS listed among the mirrors it will be included in the loop below
                    for (String mirror:mirrors) {
                        DataSource mirrorsource=source.clone();
                        mirrorsource.setServerAddress(mirror);
                        list.add(mirrorsource);
                    }
                } else list.add(source); // no mirrors. just add the original source
            } else list.add(source); // no mirrors. just add the original source
        }
        DataSource[] allSources=new DataSource[list.size()];
        allSources=list.toArray(allSources);
        return allSources;
    }
    
    /** This method should be called before DataLoader is used 
     *  It will install a DataConfiguration based on an XML-file description of available data tracks
     *   
     */
    public void setup() throws SystemError {
        engine.logMessage("Importing Data Tracks");
        File dataTracksFile=getDataConfigurationFile();
        if (!dataTracksFile.exists()) revertSettings();
        DataConfiguration config=new DataConfiguration();
        config.loadConfigurationFromFile(dataTracksFile);
        availableTracks=config.getAvailableTracks();
        servers=config.getServers();
        dataconfiguration=config;
    }
    
    /** This should be called when the system exits */
    public void shutdown() {
        if (threadpool!=null) threadpool.shutdownNow();   
        if (saveDatathreadpool!=null) saveDatathreadpool.shutdown();
    }
        
    
    /** 
     * Copies the 'immutable' DataTracks.xml that came with the installation into the working directory 
     * where it can be manipulated by the user. 
     */
    private void revertSettings() {
        BufferedReader inputStream=null;
        PrintWriter outputStream=null; 
        File outfile=getDataConfigurationFile();
        try {
            inputStream=new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/motiflab/engine/datasource/DataTracks.xml")));
            outputStream=new PrintWriter(outfile);
            String line;
            while((line=inputStream.readLine())!=null) {outputStream.println(line);}
        } catch (IOException e) { 
            engine.errorMessage("An error occurred when reverting datatrack settings: ["+e.getClass().toString()+"] "+e.getMessage(),0);
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
                if (outputStream!=null) outputStream.close();
            } catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader or PrintWriter in DataLoader.revertSettings(): "+ioe.getMessage());}
        }       
    }
    

    /**
     * Initializes a Data object based on data read from a file.
     * The type of data to return is determined by the 'target' data object.
     * If the target object is NULL, the datatype String can be used to specify
     * a datatype and a new object of this type will then be returned
     * @param source A String (filename or URL) or a File object or an URL-object pointing to a file which contains the data
     * @param datatype The type of data to return (only needed if 'target' is NULL)
     * @param target The data object that should be initialized with the data read (if NULL a new object will be created)
     * @param The format of the data in the file
     * @param settings Additional settings for parameters of the data format
     */
     public Data loadData(Object source, String datatype, Data target, DataFormat format, ParameterSettings settings, ExecutableTask task) throws Exception {
        if (source instanceof String) { // convert to other type (the string may contain the name of the data repository)
            source=engine.getDataSourceForString((String) source); // resolve filename to a proper source
        }     
        if (source instanceof File) {
            File file=(File)source;           
            if (!file.exists()) throw new ExecutionError("File does not exist: '"+file.getAbsolutePath()+"'");
            if (!file.canRead()) throw new ExecutionError("Unable to read file: '"+file.getAbsolutePath()+"'");       
        } else if (source instanceof OutputData || source instanceof TextVariable) {
            // I got nothing yet...
        } else if (source instanceof URL) {
            // ... this is OK
        } else throw new ExecutionError("SYSTEM ERROR: Unrecognized source object for loadData. Expected String, File, URL or Data object but got '"+((source==null)?"null":source.getClass().toString())+"'");

        if (target==null) {
            if (datatype==null || datatype.isEmpty()) throw new ExecutionError("Missing data type specification");          
            Class classtype=engine.getDataClassForTypeName(datatype);
            if (classtype==null) throw new ExecutionError("Unknown or unapplicable data type for 'load from file': "+datatype);
            target=engine.createDataObject(classtype, "temporary");         
        } else {
            if (target instanceof DataGroup) ((DataGroup)target).clearAll(engine);
        }        
        if (!format.canParseInput(target)) {
           throw new ExecutionError("Data format '"+format.getName()+"' can not be used to parse "+datatype);
        }
        try {
            if (target instanceof OutputData) ((OutputData)target).setSourceFile(source); 
            if (format.canOnlyParseDirectlyFromLocalFile()) {
               if (!(source instanceof File)) throw new ExecutionError("Data format '"+format.getName()+"' can only be used to read local files."); 
               String filename=((File)source).getAbsolutePath();
               target=format.parseInput(filename, target, settings, task);
            }
            else if (source instanceof OutputData || source instanceof TextVariable) { // import from other object
                ArrayList<String> inputdata=null;
                if (source instanceof TextVariable) inputdata=((TextVariable)source).getAllStrings();
                else if (source instanceof OutputData) inputdata=((OutputData)source).getContentsAsStrings();
                target=format.parseInput(inputdata, target, settings, task);
            } else { // import from DataRepository, File or URL
                InputStream inputStream=MotifLabEngine.getInputStreamForDataSource(source);
                BufferedInputStream buffer = new BufferedInputStream(inputStream);
                target=format.parseInput(buffer, target, settings, task);
                buffer.close();// maybe this is redundant
            }
        }
        catch (InterruptedException ie) {throw ie;}
        catch (Exception e) {
            if (e instanceof NullPointerException) e.printStackTrace(System.err);
            String exceptionText=e.getClass().getSimpleName();
            if (exceptionText.contains("ParseError")) exceptionText="";
            else exceptionText+=":";
            throw new ExecutionError(exceptionText+e.getMessage(), e);
        }
        if (target instanceof RegionDataset) { // try converting Region Datasets to Motif or Module track if possible (or nested track)
             DNASequenceDataset DNAtrack=null;
             Object standard=engine.getDataItem("DNA");
             if (standard instanceof DNASequenceDataset) DNAtrack=(DNASequenceDataset)standard;
             else {
                 ArrayList<Data> allDNAtracks=engine.getAllDataItemsOfType(DNASequenceDataset.class);
                 if (allDNAtracks.size()==1) DNAtrack=(DNASequenceDataset)allDNAtracks.get(0);
             }
             boolean  ok=engine.convertRegionTrackToMotifTrack((RegionDataset)target, null, DNAtrack, false); // "false" = Do not force conversion but check the track first
             if (!ok) ok=engine.convertRegionTrackToModuleTrack((RegionDataset)target, DNAtrack, false);
             if (!ok) ok=engine.convertRegionTrackToNestedTrack((RegionDataset)target);
        }  
        return target;
    }
     
     
    /** This class is used to schedule concurrent downloads */
    private class DownloadSequenceDataTask<FeatureSequenceData> implements Callable<FeatureSequenceData> {
        Sequence sequence;
        final int[] counters;
        String datatrackName;
        ExecutableTask task;
        DataTrack datatrack;
        
        public DownloadSequenceDataTask(DataTrack datatrack, Sequence sequence, ExecutableTask task, int[] counters) {
           this.sequence=sequence;
           this.counters=counters;
           this.datatrack=datatrack;
           this.task=task;           
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public FeatureSequenceData call() throws Exception {
            synchronized(counters) {
                counters[0]++; // number of downloads started
            }
            FeatureSequenceData sequencedata=(FeatureSequenceData)getSequenceData(datatrack, sequence, datatrack.getName(), task, counters[0], counters[2]);           
            synchronized(counters) {
                counters[1]++; // number of downloads finished
                task.setProgress(counters[1], counters[2]);             
            }   
            return sequencedata;
        }   
    } 
    
    private class saveDataRunnable implements Runnable {
        String trackname=null;
        int organism=0;
        String genomebuild=null;
        FeatureSequenceData data=null;
        
        public saveDataRunnable(String trackname, int organism, String genomebuild, FeatureSequenceData data) {
            this.trackname=trackname;
            this.organism=organism;
            this.genomebuild=genomebuild;
            this.data=data;
        }
        
        @Override
        public void run() {
            try {                      
                cache.saveData(trackname, organism, genomebuild, data);
            } catch (SystemError e) {
                engine.logMessage(e.getMessage(),30);
            }
        }        
    }    
    
    
    /** Reads and outputs a stream to STDERR. Used for debugging purposes */
    public static void debugRead(InputStream input) {
        BufferedReader inputStream=null;
        try {
            inputStream=new BufferedReader(new InputStreamReader(input));
            String line;
            while((line=inputStream.readLine())!=null) {
                System.err.println(line);
            }
        } catch (IOException e) { 
            System.err.println("*ERROR* IOException:"+e.getMessage());
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {}
        }        
    }
    
}
