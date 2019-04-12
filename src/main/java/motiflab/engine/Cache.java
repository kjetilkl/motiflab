/*
 
 
 */

package motiflab.engine;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Sequence;


/**
 * This class represents a cache for storing downloaded Feature data locally so
 * that reconnecting to and downloading from  from internet source is not necessary
 * every time if the data has been used before
 * 
 * @author kjetikl
 */
public class Cache {
    private MotifLabEngine engine;
    private String cacheDirectory;
    private SegmentLocationComparator segmentComparator=new SegmentLocationComparator();
    
    public Cache(MotifLabEngine engine, String cacheDirectory) {
        this.engine=engine;
        this.cacheDirectory=cacheDirectory;
        
        // create cache directory if it does not exist
        File dir=new File(cacheDirectory);
        if (!dir.exists()) {
            try {
                if (!dir.mkdirs()) throw new IOException("Unable to create local data cache directory in '"+cacheDirectory+"'");
            } catch (Exception e) {engine.logMessage("SYSTEM ERROR: Data cache error: "+e.getMessage());}
        } 
    }
    
    private String getCacheDirectory(String trackName, int organism, String genomebuild, String chromosome) {
        return cacheDirectory+File.separator+trackName+File.separator+organism+"_"+genomebuild+File.separator+chromosome;
    }
    
    /** 
     * Stores data in the cache in the form of DataSegments.
     * By convention, the segments stored in the cache should never overlap (although segments with Region data can contain Regions that span across the boundaries of the segment)
     * so if the new sequence overlaps with existing cache data, the existing file will either be removed
     * or the new data will be cropped so that it does not overlap with existing files
     * (or maybe the sequence will not be saved at all because all the data is already in the cache)
     * 
     * NOTE: the entire saveData method has been synchronized to make sure that no overlapping cache segments are ever created 
     *       This will probably be "expensive" in terms of efficiency, but it will at least avoid problems with the cache being updated by concurrent threads
     * 
     * @return TRUE if the operation was successful 
     */
    @SuppressWarnings("unchecked")
    public synchronized boolean saveData(String trackName, int organism, String genomebuild, FeatureSequenceData data) throws SystemError {       
        String chromosome=data.getChromosome();        
        if (chromosome.equals("?")) return false; // do not store this data. We don't know where it is from
        int start=data.getRegionStart();
        int end=data.getRegionEnd();
        String targetDir=getCacheDirectory(trackName, organism, genomebuild, chromosome);
        File cacheDir=new File(targetDir);
        if (cacheDir.exists() && !cacheDir.isDirectory()) {
            engine.logMessage("Unable to save data to cache directory: "+targetDir+("   (Directory already exists as a file)"));
            return false;
        }
        //engine.logMessage("Saving data in cache: "+organism+"/"+genomebuild+", "+chromosome+":"+start+"-"+end);
        Object datavalue=null;
        ArrayList<String> toBeDeleted=new ArrayList<String>(); // list of smaller regions which will be replaced by the new sequence
        ArrayList<int[]> overlaps=lookupRegion(trackName, organism, genomebuild, chromosome, start, end); // get cached data that overlaps with new data
        if (overlaps!=null) { // New data overlaps existing files. Adjust start/end-positions of new segment to avoid overlaps
            for (int[] pos:overlaps) {
                if (pos[0]<=start && pos[1]>=end) {
                    return true; // the new data is completely covered by a segment already in the cache. No need to store again
                }
                if (pos[0]>=start && pos[1]<=end) { // an already cached segment lies fully within new data => Delete this smaller file and store the new one instead                 
                    toBeDeleted.add(pos[0]+"_"+pos[1]); //System.err.println("Deleting smaller covered region!");
                } else if (pos[0]<start && pos[1]<end) { // cached segment partially overlaps at left end
                    start=pos[1]+1; // set new start to position after cached end
                    //System.err.println("Left flank partially covered. Adjusting start to position="+start);
                } else if (pos[0]>start && pos[1]>end) { // cached segment partially overlaps at right end
                    end=pos[0]-1; // set new end to position before cached start
                    //System.err.println("Right flank partially covered. Adjusting end to position="+end);
                } else {
                    System.err.println("WARNING: Reached 'unreachable' else-clause in cache.saveData(): start="+start+", end="+end+", cachedstart="+pos[0]+", cachedend="+pos[1]);
                }
            }
            // Check once more just to be absolutely sure there are no overlaps (this is probably a lazy way of doing things...)
            for (int[] pos:overlaps) { // check if there are partial overlaps where the cached segment is not fully inside the new segment
                if (!(pos[1]<start || pos[0]>end) && !(pos[0]>=start && pos[1]<=end)) throw new SystemError("SYSTEM WARNING: Detected inconsistencies in data cache (error code:89413)");
            }
        }
        if (start>end) return true; // just in case (segment has been truncated into nothing)
        datavalue=data.getValueInGenomicInterval(start, end); // Note: in case of regions, these will be the originals!
        if (datavalue!=null) {
           if (datavalue instanceof ArrayList) datavalue=processRegions((ArrayList<Region>)datavalue, data.getRegionStart(), start, (RegionSequenceData)data, trackName); // this will make a cloned copy with updated reference frame (relative to start of DataSegment)
           DataSegment cacheddata=new DataSegment(trackName, organism, genomebuild, chromosome, start, end, datavalue);
           String filename=targetDir+File.separator+(start+"_"+end);
           if (toBeDeleted.contains(start+"_"+end)) return true; // The new trimmed region is apparently already stored. Just keep the old instance
           for (String deletefilename:toBeDeleted) { // delete files that lie within the new larger region
               File oldfile=new File(targetDir+File.separator+deletefilename);
               //System.err.println("Deleting smaller file: "+deletefilename);
               if (!oldfile.delete()) return false; // unable to delete old file. Abort caching to keep consistency intact
           }           
           return saveObjectToFile(filename, cacheddata);
        }
        return false;
    } 
    
    /**
     * Goes through a list of Regions and makes a deep copy of the list but clears
     * the parent sequences from the Regions. It also adjusts the coordinates
     * of the regions (if need be) so that they are relative to newStart rather than oldStart
     * @param list
     * @param oldStart
     * @param newStart
     * @return 
     */
    private ArrayList<Region> processRegions(ArrayList<Region> list, int oldStart, int newStart, RegionSequenceData parent, String trackName) {
        // first make a deep clone of the list
        ArrayList<Region> newList=new ArrayList<Region>(list.size());
        for (Region reg:list) {
            Region newreg=reg.clone();
            newreg.setParent(null);
            newList.add(newreg);
        }
        if (oldStart==newStart) return newList; // no need to update reference frame
        // now update all coordinates since the frame of reference has been altered
        for (Region reg:newList) {
            reg.updatePositionReferenceFrame(oldStart, newStart); // this is "recursive" for nested regions
        }
        return newList;
    }
    
    
    
    /** Saves an object to file */
    private boolean saveObjectToFile(String filename,Object object) {
        //engine.logMessage("Saving new cache data to: "+filename);
        ObjectOutputStream outputStream = null;
        try { 
            File file=new File(filename);
            File directory=file.getParentFile();
            if (directory==null) throw new IOException("Cache directory error");
            synchronized(this) { // synchronize this because the mkdirs will return false if the directory has been created by another Thread in the meantime
                if (!directory.exists()) {
                   if (!directory.mkdirs()) throw new IOException("Unable to create cache directory: "+directory.getAbsolutePath());
                }
            }
            outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            outputStream.writeObject(object);
            outputStream.close();                
        } catch (Exception e) {
            engine.logMessage("WARNING: Unable to store data in cache: "+e.getMessage());
            return false;
        } finally {
            try {if (outputStream!=null) outputStream.close();} catch (Exception x) {engine.logMessage("SYSTEM WARNING: Unable to close ObjectOutputStream in cache: "+x.getMessage());}
        }          
       return true;        
    }
    
    /** Loads an object from a file */
    private Object loadObjectFromFile(String filename) {
        //engine.logMessage("Loading cache data from: "+filename);
        ObjectInputStream inputStream = null;
        try { 
            inputStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)));
            Object value=inputStream.readObject();
            inputStream.close();  
            return value;
        } catch (InvalidClassException e) {
            // this can happen if a data class has been updated in a never version compared to 
            // the version of the data object stored in the cache
            try {if (inputStream!=null) inputStream.close();} catch (Exception x){}
            File file=new File(filename);
            file.delete();
            return null;
        } catch (Exception e) {
            engine.logMessage("WARNING: Unable to load data from cache: "+e.getMessage());
            return null;
        } finally {
            try {if (inputStream!=null) inputStream.close();} catch (Exception x) {engine.logMessage("SYSTEM WARNING: Unable to close ObjectInputStream in cache: "+x.getMessage());}
        }                 
    }

    
    /**
     * Given a sequence and a list of data segments (which will be discarded right away
     * but is used to return the results), this method will fill in any segments found in the cache
     * and add empty segments covering missing parts of the sequence. Note that the first and last 
     * segments might cross the boundaries of the sequence.
     */
    @Deprecated
    private void replaceWithCachedSegments(FeatureSequenceData sequence, ArrayList<DataSegment> segments, String trackName, int organism, String genomebuild) throws SystemError {
        String chromosome=sequence.getChromosome(); 
        int sequenceStart=sequence.getRegionStart();
        int sequenceEnd=sequence.getRegionEnd();
        segments.clear();
        ArrayList<int[]> overlaps=lookupRegion(trackName, organism, genomebuild, chromosome, sequenceStart, sequenceEnd);
        if (overlaps!=null){ 
            final String subdir=getCacheDirectory(trackName, organism, genomebuild, chromosome);
            for (int[] pos:overlaps) {               
                  String filename=subdir+File.separator+(pos[0]+"_"+pos[1]);
                  Object saved=loadObjectFromFile(filename);
                  if (saved!=null && saved instanceof DataSegment) segments.add((DataSegment)saved);                        
            }
            Collections.sort(segments);
        }
        if (segments.size()>0) { // got cached segments, now fill in empty slots for missing parts
            ArrayList<DataSegment> inbetween=new ArrayList<DataSegment>();
            int firststart=segments.get(0).getSegmentStart();
            if (firststart>sequenceStart) inbetween.add(new DataSegment(trackName, organism, genomebuild, chromosome, sequence.getRegionStart(), firststart-1, null));
            int lastend=segments.get(segments.size()-1).getSegmentEnd();
            if (lastend<sequenceEnd) inbetween.add(new DataSegment(trackName, organism, genomebuild, chromosome, lastend+1, sequence.getRegionEnd(), null));
            for (int i=0;i+1<segments.size();i++) {
                DataSegment first=segments.get(i);
                DataSegment second=segments.get(i+1);
                if (second.getSegmentStart()-first.getSegmentEnd()>1) inbetween.add(new DataSegment(trackName, organism, genomebuild, chromosome, first.getSegmentEnd()+1, second.getSegmentStart()-1, null));
            }     
            segments.addAll(inbetween);
            Collections.sort(segments);
        } else { // no cached segments, just add one segment that spans the whole sequence
            segments.add(new DataSegment(trackName, organism, genomebuild, sequence.getChromosome(), sequence.getRegionStart(), sequence.getRegionEnd(), null));
        }        
    }
    
    /**
     * Given a sequence and a list of data segments that together spans the sequence, 
     * this method will fill in any data from the cache that falls within the sequence.
     * Note that the original segments list can be altered as old segments can be
     * split up, merged or simply replaced with new segments depending on which portions
     * of sequence have available data in the cache.
     * @param sequence
     * @param segments
     * @param trackName
     * @param organism
     * @param genomebuild 
     */
    public void loadCachedData(FeatureSequenceData sequence, ArrayList<DataSegment> segments, String trackName, int organism, String genomebuild) throws SystemError {
        String chromosome=sequence.getChromosome(); 
        int start=sequence.getRegionStart();
        int end=sequence.getRegionEnd();        
        ArrayList<int[]> overlaps=lookupRegion(trackName, organism, genomebuild, chromosome, start, end); // find all cache files that overlap with the region
        if (overlaps!=null){ 
            final String subdir=getCacheDirectory(trackName, organism, genomebuild, chromosome);
            //engine.logMessage("Found overlap in cache");
            for (int i=0;i<segments.size();i++) { // process segments one by one
                //engine.logMessage("Cache:["+i+"] Processing segment "+(i+1)+" of "+segments.size()+": "+segments.get(i).toString());
                DataSegment segment=segments.get(i);
                if (segment.containsData()) continue;
                ArrayList<int[]> overlapsSegment=getOverlapping(overlaps, segment.getSegmentStart(), segment.getSegmentEnd());
                if (!overlapsSegment.isEmpty()) {
                    // split up this segment based on which segments are available in the cache
                    ArrayList<DataSegment> splits=splitSegment(segment, overlapsSegment);
                    if (splits==null) {
                        engine.logMessage("SYSTEM WARNING: Detected problems while processing cached data (error code:19431)");
                        return;
                    }     
                    // now fill in the (exact) segments that are in the cache
                    for (int[] sub:overlapsSegment) {
                        DataSegment subsegment=findMatchingSegment(splits, sub);
                        if (subsegment!=null) {
                            boolean ok=fillSegmentWithDataFromCache(subsegment, sub, subdir);
                            if (ok) subsegment.setSaveToCache(true); // save back to cache (but only if the new sequence region covers a greater region than currently cached segments)
                        } else engine.logMessage("SYSTEM WARNING: Detected potential problems while processing cached data (error code:19432)");
                    }
                    // replace the old segments with the new and adjust loop-pointer
                    segments.remove(i);
                    segments.addAll(i, splits);
                    //engine.logMessage("Replacing segment ["+i+"] with "+splits.size()+" new segments");
                    i+=(splits.size()-1); // i will increased by +1 more when the loop restarts
                }
            } // end for each segment
        }
        String error=checkSegments(segments, start, end);
        if (error!=null) {
            engine.logMessage("SYSTEM WARNING: Detected problems while processing cached data (error code:"+error+")");
            // DataLoader.debugSegments(segments);
        }         
    }
    
    /** Returns the matching segment from the list where either:
     *  1) The start of the segment equals cachePos[0] and cachePos[1] is equal to or greater than segmentEnd
     *  2) The end of the segment equals cachePos[1] and cachePos[0] is equal to or smaller than segmentStart
     *  Or in short: one of the edges of the segment must correspond with one of the edges in cachePos
     *               and the cachePos range must cover the whole segment.
     */
    private DataSegment findMatchingSegment(ArrayList<DataSegment> segments, int[] cachePos) {
        for (DataSegment segment:segments) {
            boolean match=((segment.getSegmentStart()>=cachePos[0] && segment.getSegmentEnd()<=cachePos[1]) || (segment.getSegmentEnd()==cachePos[1] && segment.getSegmentStart()>=cachePos[0]));
            if (match) return segment;
        }
        return null;
    }
    
    /** Given an empty segment and the location of a segment that should exist in the cache
     *  this method will read the data in the cache and insert it in the segment
     */
    private boolean fillSegmentWithDataFromCache(DataSegment segment, int[] cachePos, String basedir) {
       String filename=basedir+File.separator+(cachePos[0]+"_"+cachePos[1]);
       Object saved=loadObjectFromFile(filename);
       if (saved==null || !(saved instanceof DataSegment)) return false;              
       String error=segment.importData((DataSegment)saved);
       if (error!=null) {
          engine.logMessage("SYSTEM WARNING: Detected problems while processing cached data (error code:"+error+")");
          segment.setSegmentData(null); // this will clear the data
          return false;
       } // else engine.logMessage("DEBUG: imported data with no error from: "+filename);
       return true;
    }
    
    /**
     * Given a single segment and a list of overlapping locations (given as start+end pairs)
     * this method will split the segment into multiple segments that correspond to the overlapping
     * locations and any missing bits inbetween
     * @param segment
     * @param overlapping A list of locations overlapping the segment. These location should be themselves non-overlapping and sorted in ascending order
     * @return 
     */
    private ArrayList<DataSegment> splitSegment(DataSegment segment, ArrayList<int[]> overlapping) {
        int start=segment.getSegmentStart();
        int end=segment.getSegmentEnd();
        ArrayList<DataSegment> newlist=new ArrayList<DataSegment>();
        for (int[] sub:overlapping) {
            DataSegment subsegment=segment.getEmptySubSegmentGenomicPositions(sub[0], sub[1]);
            if (subsegment!=null) newlist.add(subsegment); else return null;
//          engine.logMessage("Segment ("+segment.getLocationAsString()+") overlaps with ["+sub[0]+","+sub[1]+"]. Creating new subsegment => "+subsegment.toString());
            Collections.sort(newlist);            
        }
        // now fill in missing bits
        fillInMissingSegments(newlist, start, end, segment.getDatatrackName(), segment.getOrganism(), segment.getGenomeBuild(), segment.getChromosome());
        // one last check...
        String error=checkSegments(newlist, start, end);
        if (error!=null) engine.logMessage("SYSTEM WARNING: Detected problems while processing cached data (error code:"+error+".2)"); 
        return newlist;
    }
    
    
    private void fillInMissingSegments(ArrayList<DataSegment> segments,int start, int end, String trackName, int organism, String genomebuild, String chromosome) {
        if (segments.isEmpty()) { // no segments yet. Just add one segment that spans the whole sequence
            segments.add(new DataSegment(trackName, organism, genomebuild, chromosome, start, end, null));
        } else { // fill in between existing segments
            ArrayList<DataSegment> inbetween=new ArrayList<DataSegment>();
            int firststart=segments.get(0).getSegmentStart();
            if (firststart>start) inbetween.add(new DataSegment(trackName, organism, genomebuild, chromosome, start, firststart-1, null));
            int lastend=segments.get(segments.size()-1).getSegmentEnd();
            if (lastend<end) inbetween.add(new DataSegment(trackName, organism, genomebuild, chromosome, lastend+1, end, null));
            for (int i=0;i+1<segments.size();i++) {
                DataSegment first=segments.get(i);
                DataSegment second=segments.get(i+1);
                if (second.getSegmentStart()-first.getSegmentEnd()>1) inbetween.add(new DataSegment(trackName, organism, genomebuild, chromosome, first.getSegmentEnd()+1, second.getSegmentStart()-1, null));
            }     
            segments.addAll(inbetween);
            Collections.sort(segments);
        }       
    }
    
    /** Given a list of segments (represented as coordinate pairs)
     *  this method will return a list containing those segments that overlaps
     *  with the region from "start" to "end".
     */
    private ArrayList<int[]> getOverlapping(ArrayList<int[]> list, int start, int end) {
        ArrayList<int[]> result=new ArrayList<int[]>();
        for (int[] seg:list) {
            if (!(start>seg[1] || end<seg[0])) result.add(seg);
        }
        return result;
    }
    
    
    private boolean allSegmentsEmpty(ArrayList<DataSegment> segments) {
        for (DataSegment segment:segments) {
            if (segment.containsData()) return false;
        }
        return true;
    }
    /** 
     * Returns a list of cache regions that covers data originating from the given 
     * DataTrack and genomic segment. Each entry in the return list
     * is an int[] with two positions (denoting the start and end position of a segment region in the cache).
     * The segment regions are ordered according to ascending start position
     * and (by convention) segments stored in the cache should never overlap.
     * @throws SystemError If inconsistencies are found in the cache (usually meaning that overlapping segments are found)
     */
    private ArrayList<int[]> lookupRegion(String trackName, int organism, String genomebuild, String chromosome, final int start, final int end) throws SystemError {
        // each track has its own subdir. Within this subdir each genome build has its own subdir, and below that each chromosome has its own subdir
        final String subdir=getCacheDirectory(trackName, organism, genomebuild, chromosome);
        File dir=new File(subdir);
        if (!dir.exists() || !dir.isDirectory()) return null;
        ArrayList<int[]> result=new ArrayList<int[]>();
        String[] fileslist=dir.list();
        if (fileslist==null) return null;
        for (String name:fileslist) {
            int pos=name.indexOf('_');
            if (pos<0) continue; // this should not happen if the files are consistent
            int segmentstart=Integer.parseInt(name.substring(0,pos)); // first element
            int segmentend=Integer.parseInt(name.substring(pos+1)); // last element
            if (!(start>segmentend || end<segmentstart)) result.add(new int[]{segmentstart,segmentend});            
        }
        Collections.sort(result, segmentComparator);
        boolean ok=checkCachedRegions(result); // Check that the cached regions are non-overlapping
        if (!ok) {
            throw new SystemError("SYSTEM WARNING: Detected inconsistencies in data cache (error code:89412). Disabling cache!");
        }
        return result;
     }    
    
    /**
     * Given a list of locations, this method checks if the locations are non-overlapping
     * and sorted in ascending order
     * @param list
     * @return TRUE if the list of OK, else FALSE (if some are overlapping or out of order)
     */  
      private boolean checkCachedRegions(ArrayList<int[]> list) {
          int lastend=Integer.MIN_VALUE;
          for (int[] loc:list) {
              if (loc[0]>loc[1]) return false; // this will probably not happen
              if (loc[0]<=lastend) return false; // next segment prior to end of last (or overlapping even 1 pos)
              lastend=loc[1];
          }
          return true;
      }
      
      /**
       * Checks a list of segments against a given location and returns NULL iff
       * the segments together spans the complete location, the segments are in
       * sorted order and non of the segments overlap each other.
       * If something is wrong with the list an error code will be returned
       */
      private String checkSegments(ArrayList<DataSegment> list, int start, int end) {
          if (list.isEmpty()) return "38020";
          if (list.get(0).getSegmentStart()!=start) return "38021";
          if (list.get(list.size()-1).getSegmentEnd()!=end) return "38022";
          int lastend=start-1;         
          for (int i=0;i<list.size();i++) {
              DataSegment segment=list.get(i);
              if (segment.getSegmentStart()!=lastend+1) return "38023["+(i+1)+"/"+(list.size())+"]";
              lastend=segment.getSegmentEnd();
              if (lastend>end) return "38024["+(i+1)+"/"+(list.size())+"]";
          }
          return null;
      }      
    
    
    /** 
     * Clears all the contents in the cache
     * @return TRUE if all cached files were successfully deleted, else FALSE
     */
    public boolean clearCache() {
        File directory=new File(cacheDirectory);
        File[] files=directory.listFiles();
        boolean alldeleted=true;
        for (File file:files) {
            if (!engine.deleteTempFile(file)) { // this will delete files recursively for directories
               engine.logMessage("SYSTEM ERROR: Unable to delete cache for: "+file.getAbsolutePath());
               alldeleted=false;       
            }
        }
        return alldeleted;
    }
     
    private void debugSegments(ArrayList<DataSegment> segments) {
        for (int i=0;i<segments.size();i++) {
            DataSegment segment=segments.get(i);
            engine.logMessage("SEGMENT["+(i+1)+"/"+segments.size()+"] "+segment.toString());
        }        
    }    
    
    private class SegmentLocationComparator implements Comparator<int[]> {
        @Override
        public int compare(int[] o1, int[] o2) {
            return o1[0]-o2[0];
        }
        
    }
    
}
