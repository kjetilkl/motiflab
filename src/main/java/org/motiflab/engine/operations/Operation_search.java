/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.TextVariable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.Region;

/**
 *
 * @author kjetikl
 */
public class Operation_search extends Operation {
    private static final String name="search";
    private static final String description="Searches DNA sequences for matches to regular expressions, motif consensus patterns or tandem (and inverted) repeats";
    private Class[] datasourcePreferences=new Class[]{DNASequenceDataset.class};
    public static final String SEARCH_EXPRESSION="searchExpression"; // script representation of the search expression (literal regex or name of variable), or special value constant=SEARCH_PALINDROME
    public static final String SEARCH_REPEAT="#SEARCHREPEAT#"; //
    public static final String SEARCH_REPEAT_DIRECTION="searchRepeatDirection"; //
    public static final String SEARCH_REPEAT_DIRECT="direct"; //
    public static final String SEARCH_REPEAT_INVERTED="inverted"; //
    public static final String REPORT_SITE="reportSite"; //
    public static final String REPORT_SITE_FULL="full"; //
    public static final String REPORT_SITE_HALFSITE="halfsites"; //
    public static final String SEARCH_STRAND="searchStrand"; // search strand parameter
    public static final String STRAND_DIRECT="direct strand"; // search strand value
    public static final String STRAND_REVERSE="reverse strand"; // search strand
    public static final String STRAND_GENE="relative strand"; // search strand
    public static final String STRAND_OPPOSITE="opposite strand"; // search strand
    public static final String STRAND_BOTH="both strands"; // search strand
    public static final String MISMATCHES="mismatches"; // number of mismatches to allow
    public static final String MIN_HALFSITE_LENGTH="minHalfsiteLength";
    public static final String MAX_HALFSITE_LENGTH="maxHalfsiteLength";
    public static final String MIN_GAP_LENGTH="minGapLength";
    public static final String MAX_GAP_LENGTH="maxGapLength";
    private static final String SEARCH_DATA="searchData"; // resolved search expression. String or Text Variable or motif reference 
    private static final String IS_MOTIF_TRACK="isMotifTrack"; //
    public static final int MINIMUM_HALFSITE_SIZE=3;
    public static final int MAXIMUM_GAP_SIZE=999;

    @Override
    public String getOperationGroup() {
        return "Motif";
    }

    @Override
    public Class[] getDataSourcePreferences() {
        return datasourcePreferences;
    }

    @Override
    public boolean canUseAsSourceProxy(Data object) {
        return (object instanceof Motif || object instanceof MotifCollection || object instanceof TextVariable);
    } 
    @Override
    public boolean assignToProxy(Object proxysource, OperationTask operationtask) {
        Data proxy=null;
        if (proxysource instanceof Data) proxy=(Data)proxysource;
        else if (proxysource instanceof Data[] && ((Data[])proxysource).length>0) proxy=((Data[])proxysource)[0];
        if (proxy instanceof Motif || proxy instanceof MotifCollection || proxy instanceof TextVariable) {
          operationtask.setParameter(SEARCH_EXPRESSION,proxy.getName());
          return true;
        } else return false;
    }     
    
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }
    @Override
    public boolean isSubrangeApplicable() {return true;}
    
    @Override
    public String toString() {return name;}
    
    public void parseParameters(OperationTask task) throws Exception { 
        boolean isMotifTrack=false;
        ArrayList<Object[]> regexList=new ArrayList<Object[]>();  // first is "raw" regex, second is compiled Pattern, third is name of motif (if used) or null
        String rawSearchExpression=(String)task.getParameter(SEARCH_EXPRESSION);
        if (rawSearchExpression.equals(SEARCH_REPEAT)) {

        }
        else if (rawSearchExpression.startsWith("\"") && rawSearchExpression.endsWith("\"")) {
            rawSearchExpression=rawSearchExpression.substring(1, rawSearchExpression.length()-1);
            if (!rawSearchExpression.isEmpty()) addToRegexList(regexList,rawSearchExpression,null);
        } else {
            Data expressionData=null;
            expressionData=engine.getDataItem(rawSearchExpression);
            if (expressionData==null) throw new ExecutionError("Unrecognized token '"+rawSearchExpression+"' is not a data object",task.getLineNumber());
            if (expressionData instanceof TextVariable) {
                for (String value:((TextVariable)expressionData).getAllStrings()) {
                   if (!value.trim().isEmpty()) addToRegexList(regexList,value.trim(),null);
                }
            } else if (expressionData instanceof Motif) {
                String value=((Motif)expressionData).getConsensusMotif();
                if (!value.isEmpty()) addToRegexList(regexList,value,((Motif)expressionData).getName());
                isMotifTrack=true;
            } else if (expressionData instanceof MotifCollection) {
                for (Motif motif:((MotifCollection)expressionData).getAllMotifs(engine)) {
                    String value=motif.getConsensusMotif();
                    if (!value.isEmpty()) addToRegexList(regexList,value,motif.getName());
                }                                
                isMotifTrack=true;
            } 
            else throw new ExecutionError(rawSearchExpression+"("+expressionData.getTypeDescription()+") can not be used as a search expression",task.getLineNumber());
        }    
        task.setParameter(IS_MOTIF_TRACK, isMotifTrack);                      
        task.setParameter(SEARCH_DATA, regexList);    
    } 
    
    
    private void addToRegexList(ArrayList<Object[]> regexList,String expression, String motifname) {
            String resolvedexpression=convertIUPAC(expression.toUpperCase());
            Pattern pattern=Pattern.compile(resolvedexpression,Pattern.CASE_INSENSITIVE);
            regexList.add(new Object[]{expression,pattern,motifname});        
    }
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String sourceDatasetName=task.getSourceDataName();
        if (sourceDatasetName==null || sourceDatasetName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());          
        String targetDatasetName=task.getTargetDataName();
        FeatureDataset sourceDataset=(FeatureDataset)engine.getDataItem(sourceDatasetName);
        if (sourceDataset==null) throw new ExecutionError("Unknown data object '"+sourceDatasetName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceDataset)) throw new ExecutionError(sourceDatasetName+"("+sourceDataset.getTypeDescription()+") is of a type not supported by the '"+getName()+"' operation",task.getLineNumber());
        
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) condition.resolve(engine, task);        
        Condition_within within=(Condition_within)task.getParameter("within");
        if (within!=null) within.resolve(engine, task);        
        parseParameters(task);
        
        String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
        Data seqcol=engine.getDataItem(subsetName);
        if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
        if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
        SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
        int i=0;
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
        int size=sequences.size();  
        RegionDataset targetDataset=new RegionDataset(targetDatasetName); // Double-buffer.
        targetDataset.setupDefaultDataset(engine.getDefaultSequenceCollection().getAllSequences(engine));        
        
        if (isSubrangeApplicable() && within!=null) { // remove sequences with no selection windows (if within-condition is used)
            Iterator iter=sequences.iterator();
            while (iter.hasNext()) {
                Sequence seq = (Sequence) iter.next();
                if (!within.existsSelectionWithinSequence(seq.getName(), task)) iter.remove();
            }           
        }        
                
        TaskRunner taskRunner=engine.getTaskRunner();
        task.setProgress(0L,sequences.size());
        long[] counters=new long[]{0,0,sequences.size()}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences

        ArrayList<ProcessSearchSequenceTask> processTasks=new ArrayList<ProcessSearchSequenceTask>(sequences.size());
        for (Sequence sequence:sequences) processTasks.add(new ProcessSearchSequenceTask((DNASequenceDataset)sourceDataset, (RegionDataset)targetDataset, sequence.getName(), task, counters));
        List<Future<RegionSequenceData>> futures=null;
        int countOK=0;            
        try {
            futures=taskRunner.invokeAll(processTasks); // this call apparently blocks until all tasks finish (either normally or by exceptions or being cancelled)                             
            for (Future<RegionSequenceData> future:futures) {
                if (future.isDone() && !future.isCancelled()) {
                    future.get(); // this blocks until completion but the return value is not used
                    countOK++;
                }
            }
        } catch (Exception e) {  
           taskRunner.shutdownNow(); // Note: this will abort all executing tasks (even those that did not cause the exception), but that is OK. 
           if (e instanceof java.util.concurrent.ExecutionException) throw (Exception)e.getCause(); 
           else throw e; 
        } 
        // if (threadpool!=null) threadpool.shutdownNow();        
        if (countOK!=sequences.size()) {
            throw new ExecutionError("Some mysterious error occurred while scanning");
        }         
        
      
        targetDataset.setMotifTrack((Boolean)task.getParameter(IS_MOTIF_TRACK));
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        targetDataset.setIsDerived(true);
        try {engine.storeDataItem(targetDataset);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        return true;
    }
    
    /** Searches for regular expressions within a single sequence*/
    @SuppressWarnings("unchecked")
    private void searchInSequence(DNASequenceData sourceSequence, RegionSequenceData targetSequence, OperationTask task)  throws Exception {          
          String sequenceName=sourceSequence.getName();
          String dnasequence=sourceSequence.getSequenceAsString();
          String reversednasequence=null;
          boolean searchDirect=false;
          boolean searchReverse=false;
          Condition_position condition=(Condition_position)task.getParameter("where");
          Condition_within within=(Condition_within)task.getParameter("within");
          ArrayList<Object[]> regexList=(ArrayList<Object[]>)task.getParameter(SEARCH_DATA);   
          int mismatchesAllowed=0;
          Object mismatchObject=task.getParameter(MISMATCHES);
          if (mismatchObject!=null && mismatchObject instanceof Integer) mismatchesAllowed=((Integer)mismatchObject).intValue();
          int seqOrientation=sourceSequence.getStrandOrientation();
          String searchStrands=(String)task.getParameter(SEARCH_STRAND);
          if (searchStrands==null || searchStrands.equals(STRAND_BOTH)) {searchDirect=true;searchReverse=true;}
          else if (searchStrands.equals(STRAND_DIRECT)) searchDirect=true;
          else if (searchStrands.equals(STRAND_REVERSE)) searchReverse=true;
          else if (searchStrands.equals(STRAND_GENE) || searchStrands.equals("gene strand")) {
              if (seqOrientation==Sequence.DIRECT) searchDirect=true; else searchReverse=true;
          }
          else if (searchStrands.equals(STRAND_OPPOSITE)) {
              if (seqOrientation==Sequence.DIRECT) searchReverse=true; else searchDirect=true; 
          }   
          if (searchReverse) reversednasequence=MotifLabEngine.reverseSequence(dnasequence);
          int found=0; // number of matches found
          for (Object[] element:regexList) {
              String expression=(String)element[0];
              Pattern pattern=(Pattern)element[1];
              String motifname=(String)element[2];
              //System.err.println("Searching "+sequenceName+" with '"+expression+"' => "+pattern.toString());
              boolean trueRegex=expression.matches(".*[^a-zA-Z].*"); // regex contains regex operators like (not normal letters)
              if (trueRegex && mismatchesAllowed>0) throw new ExecutionError("No mismatches allowed for patterns with wildcards: "+expression);
              if (searchDirect && mismatchesAllowed==0) { // use regular expressions to search the direct strand
                  int startoffset=0;
                  Matcher matcher=pattern.matcher(dnasequence);
                  while (matcher.find(startoffset)) {
                      found++;
                      if (found%100==0) { // take a break for every 100 hits
                            task.checkExecutionLock(); // checks to see if this task should suspend execution
                            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                            Thread.yield();                          
                      }
                      String sequencematch=matcher.group();
                      int start=matcher.start();
                      int end=matcher.end()-1; //  
                      if (start+1<dnasequence.length()) startoffset=start+1;
                      double score=(trueRegex)?1f:expression.length();
                      addRegionIfSatisfied(sourceSequence, targetSequence, motifname, start, end, expression, Sequence.DIRECT, sequencematch, score, task, within, condition);
                  }
              }
              else if (searchDirect && mismatchesAllowed>0) { // use mismatch-algorithm to search direct strand
                  ArrayList<int[]>matches=mismatchSearch(dnasequence,expression,mismatchesAllowed,task);
                  for (int[] match:matches) {
                      found++;
                      if (found%100==0) { // take a break for every 100 hits
                            task.checkExecutionLock(); // checks to see if this task should suspend execution
                            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                            Thread.yield();                          
                      }
                      int start=match[0];
                      int end=match[1];
                      double score=match[2];
                      String sequencematch=dnasequence.substring(start, end+1);
                      addRegionIfSatisfied(sourceSequence, targetSequence, motifname, start, end, expression, Sequence.DIRECT, sequencematch, score, task, within, condition);
                  }
              }
              if (searchReverse && mismatchesAllowed==0) {
                  int startoffset=0;
                  Matcher matcher=pattern.matcher(reversednasequence);
                  while (matcher.find(startoffset)) {
                      found++;
                      if (found%100==0) { // take a break for every 100 hits
                            task.checkExecutionLock(); // checks to see if this task should suspend execution
                            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                            Thread.yield();                          
                      }                      
                      String sequencematch=matcher.group();
                      int start=(reversednasequence.length()-1)-(matcher.end()-1); // convert coordinates from reverse to direct strand
                      int end=(reversednasequence.length()-1)-matcher.start(); // convert coordinates from reverse to direct strand
                      if (matcher.start()+1<dnasequence.length()) startoffset=matcher.start()+1;
                      // correct coordinates back to direct strand
                      double score=(trueRegex)?1f:expression.length();
                      addRegionIfSatisfied(sourceSequence, targetSequence, motifname, start, end, expression, Sequence.REVERSE, sequencematch, score, task, within, condition);
                  }
              }
              else if (searchReverse && mismatchesAllowed>0) { // use mismatch-algorithm to search direct strand
                  ArrayList<int[]>matches=mismatchSearch(reversednasequence,expression,mismatchesAllowed,task);
                  for (int[] match:matches) {
                      found++;
                      if (found%100==0) { // take a break for every 100 hits
                            task.checkExecutionLock(); // checks to see if this task should suspend execution
                            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                            Thread.yield();                          
                      }
                      int start=(reversednasequence.length()-1)-match[1]; // convert coordinates from reverse to direct strand
                      int end=(reversednasequence.length()-1)-match[0]; // convert coordinates from reverse to direct strand                      
                      double score=match[2];
                      String sequencematch=reversednasequence.substring(match[0], match[1]+1);
                      addRegionIfSatisfied(sourceSequence, targetSequence, motifname, start, end, expression, Sequence.REVERSE, sequencematch, score, task, within, condition);
                  }
              }              
          }
   }

    /** Searches for tandem repeats or inverted repeats (palindromes) within a single sequence*/
    @SuppressWarnings("unchecked")
    private void searchRepeatsInSequence(DNASequenceData sourceSequence, RegionSequenceData targetSequence, OperationTask task)  throws Exception {
          String dnasequence=sourceSequence.getSequenceAsString();
          Condition_position condition=(Condition_position)task.getParameter("where");
          Condition_within within=(Condition_within)task.getParameter("within");
          int mismatchesAllowed=0;
          Object mismatchObject=task.getParameter(MISMATCHES);
          if (mismatchObject!=null && mismatchObject instanceof Integer) mismatchesAllowed=((Integer)mismatchObject).intValue();

          String direction=(String)task.getParameter(SEARCH_REPEAT_DIRECTION);
          boolean inverted=false;
          if (direction!=null && direction.equals(SEARCH_REPEAT_INVERTED)) inverted=true;

          String reportsite=(String)task.getParameter(REPORT_SITE);
          boolean reporthalfsite=false;
          if (reportsite!=null && reportsite.equals(REPORT_SITE_HALFSITE)) reporthalfsite=true;

          int minHalfsiteLength=0;
          Object minHalfsiteLengthObject=task.getParameter(MIN_HALFSITE_LENGTH);
          if (minHalfsiteLengthObject!=null && minHalfsiteLengthObject instanceof Integer) minHalfsiteLength=((Integer)minHalfsiteLengthObject).intValue();

          int maxHalfsiteLength=0;
          Object maxHalfsiteLengthObject=task.getParameter(MAX_HALFSITE_LENGTH);
          if (maxHalfsiteLengthObject!=null && maxHalfsiteLengthObject instanceof Integer) maxHalfsiteLength=((Integer)maxHalfsiteLengthObject).intValue();

          int minGapLength=0;
          Object minGapLengthObject=task.getParameter(MIN_GAP_LENGTH);
          if (minGapLengthObject!=null && minGapLengthObject instanceof Integer) minGapLength=((Integer)minGapLengthObject).intValue();

          int maxGapLength=0;
          Object maxGapLengthObject=task.getParameter(MAX_GAP_LENGTH);
          if (maxGapLengthObject!=null && maxGapLengthObject instanceof Integer) maxGapLength=((Integer)maxGapLengthObject).intValue();

          if (minGapLength<0) throw new ExecutionError("Minimum gap size must be equal to or greater than 0");
          if (maxGapLength<minGapLength) throw new ExecutionError("Maximum gap size must be equal to or greater than minimum gap size");
          if (minHalfsiteLength<3) throw new ExecutionError("Minimum halfsite size must be equal to or greater than 3");
          if (maxHalfsiteLength<minHalfsiteLength) throw new ExecutionError("Maximum halfsite size must be equal to or greater than minimum halfsite size");


          int found=0; // number of matches found
          ArrayList<RepeatMatch>matches=repeatsSearch(dnasequence,minHalfsiteLength,maxHalfsiteLength,mismatchesAllowed, minGapLength, maxGapLength, inverted, task);
          filterRepeats(matches, minHalfsiteLength, maxGapLength); // removes smaller, overlapping and more 'degenerate' matches
          for (RepeatMatch match:matches) {
              found++;
              if (found%100==0) { // take a break for every 100 hits
                    task.checkExecutionLock(); // checks to see if this task should suspend execution
                    if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                    Thread.yield();
              }
              // match[] contains start,end,halfsitelength,gaplength,#matching letters
              int start=match.start;
              int end=match.end;
              int halfsite=match.halfsitesize;
              int gap=match.gap;
              double score=(double)match.matches;
              String halfsiteSequence=dnasequence.substring(start, start+halfsite);
              addRepeatRegionIfSatisfied(sourceSequence, targetSequence, start, end, halfsite, gap, halfsiteSequence, score, reporthalfsite, inverted, task, within, condition);
          }

   }
    
   private void addRegionIfSatisfied(DNASequenceData sourceSequence, RegionSequenceData targetSequence,String motifname, int start, int end,  String expression, int orientation, String sequencematch, double score, OperationTask task, Condition_within within, Condition_position condition) throws Exception {
          String sequenceName=sourceSequence.getName();
          String type=(motifname!=null)?motifname:expression;
          Region newRegion=new Region(targetSequence, start, end, type, score, orientation);
          newRegion.setProperty("sequence", sequencematch);
          int genomicStart=sourceSequence.getGenomicPositionFromRelative(start);
          int genomicEnd=sourceSequence.getGenomicPositionFromRelative(end);
          if (within!=null && !regionWithinWindowCondition(within,genomicStart,genomicEnd,sequenceName,task)) return;
          if (condition==null || regionSatisfiesCondition(condition,genomicStart,genomicEnd,sequenceName,task)) targetSequence.addRegion(newRegion);
   }

   private void addRepeatRegionIfSatisfied(DNASequenceData sourceSequence, RegionSequenceData targetSequence, int start, int end,int halfsite,int gap, String halfsiteSequence, double score, boolean reportHalfsite, boolean invertedRepeat, OperationTask task, Condition_within within, Condition_position condition) throws Exception {
          String sequenceName=sourceSequence.getName();
          int genomicStart=sourceSequence.getGenomicPositionFromRelative(start);
          int genomicEnd=sourceSequence.getGenomicPositionFromRelative(end);
          if (within!=null && !regionWithinWindowCondition(within,genomicStart,genomicEnd,sequenceName,task)) return;
          if (condition==null || regionSatisfiesCondition(condition,genomicStart,genomicEnd,sequenceName,task)) {
              if (reportHalfsite) {
                  Region firstHalf=new Region(targetSequence, start, start+halfsite-1, halfsiteSequence, score, Region.DIRECT);
                  int secondHalfDirection=(invertedRepeat)?Region.REVERSE:Region.DIRECT;
                  Region secondHalf=new Region(targetSequence, start+halfsite+gap, start+halfsite+gap+halfsite-1, halfsiteSequence, score, secondHalfDirection);
                  targetSequence.addRegion(firstHalf);
                  targetSequence.addRegion(secondHalf);
              } else {
                   String type=((invertedRepeat)?"Inverted":"Direct")+" repeat : Halfsite="+halfsiteSequence+", Gap="+gap;
                   int sitedirection=(invertedRepeat)?Region.INDETERMINED:Region.DIRECT;
                   Region fullRegion=new Region(targetSequence, start, end, type, score, sitedirection);
                   targetSequence.addRegion(fullRegion);
              }
          }
   }


    
   private boolean regionSatisfiesCondition(Condition_position condition,int start, int end, String sequencename, OperationTask task) throws Exception { 
        for (int i=start;i<=end;i++) {
            if (!condition.isConditionSatisfied(sequencename, i, task)) return false;
        }    
        return true;
   }
   private boolean regionWithinWindowCondition(Condition_within condition,int start, int end, String sequencename, OperationTask task) throws Exception { 
        if (!condition.isConditionSatisfied(sequencename, start, end, task)) return false; 
        return true;
   }
   
   /** Converts any IUPAC ambiguous codes to regular expression syntax. i.e R=[AG], D=[AGT] N=. */
   private String convertIUPAC(String pattern) {
       pattern=pattern.replace("N",".");
       pattern=pattern.replace("X",".");
       pattern=pattern.replace("R","[AG]");
       pattern=pattern.replace("Y","[CT]");
       pattern=pattern.replace("S","[CG]");
       pattern=pattern.replace("W","[AT]");
       pattern=pattern.replace("K","[GT]");
       pattern=pattern.replace("M","[AC]");
       pattern=pattern.replace("B","[CGT]");
       pattern=pattern.replace("D","[AGT]");
       pattern=pattern.replace("H","[ACT]");
       pattern=pattern.replace("V","[ACG]");
       return pattern;
   }
   
   /** Searches for matches to an expression in a sequence allowing for a number of mismatches
    *  returns a list of matched regions (start,end,#matching letters) pairs
    */
   private ArrayList<int[]> mismatchSearch(String sequence, String expression, int maxmismatch, ExecutableTask task) throws InterruptedException {
       ArrayList<int[]>list=new ArrayList<int[]>(); // each entry is a pair of [start,end] coordinates
       int workingsize=sequence.length()-expression.length();
       char[] expressionChars=new char[expression.length()];
       for (int j=0;j<expression.length();j++) {expressionChars[j]=Character.toUpperCase(expression.charAt(j));}
       for (int i=0;i<=workingsize;i++) {
            if (i%500==0) { // take a break for every 500 positions
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                Thread.yield();
            }
            int miss=0;
            for (int j=0;j<expressionChars.length;j++) {
                char target=Character.toUpperCase(sequence.charAt(i+j));
                if (!charMatches(target, expressionChars[j])) miss++;    
                if (miss>maxmismatch) break; // too many mismatches already
            }   
            if (miss<=maxmismatch) list.add(new int[]{i,i+expressionChars.length-1,expressionChars.length-miss});
            //System.err.println("["+expression+"] => "+sequence.substring(i,i+expressionChars.length)+"  misses="+miss);
       }       
       return list;
   } 
   
   private boolean charMatches(char target, char expr) {
            if (target=='A' && (expr=='A' || expr=='R' || expr=='W' || expr=='M' ||expr=='D' || expr=='H' ||expr=='V' ||expr=='N' ||expr=='X')) return true;
       else if (target=='C' && (expr=='C' || expr=='Y' || expr=='S' || expr=='M' ||expr=='B' || expr=='H' ||expr=='V' ||expr=='N' ||expr=='X')) return true;
       else if (target=='G' && (expr=='G' || expr=='R' || expr=='S' || expr=='K' ||expr=='B' || expr=='D' ||expr=='V' ||expr=='N' ||expr=='X')) return true;
       else if (target=='T' && (expr=='T' || expr=='Y' || expr=='W' || expr=='K' ||expr=='B' || expr=='D' ||expr=='H' ||expr=='N' ||expr=='X')) return true;
       else return false;
   }

   /** Searches a sequence for occurrences of direct or inverted (palindromic) repeats
    *  returns a list of matched regions (start,end,halfsitelength,gaplength,#matching letters) pairs
    *  @param sequence The sequence to search
    *  @param minlength The minimum size of a halfsite
    *  @param maxlength The maximum size of a halfsite
    *  @param maxmismatch The maximum number of mismatches allowed in the "other" halfsite
    *  @param mingap The minimum spacing required between halfsites
    *  @param maxgap The maximum spacing allowed between halfsites
    *  @param inverted If true the repeats should be inverted with respect to each other (palindrome)
    */
   private ArrayList<RepeatMatch> repeatsSearch(String sequence, int minlength, int maxlength, int maxmismatch, int mingap, int maxgap, boolean inverted, ExecutableTask task) throws InterruptedException {
       ArrayList<RepeatMatch>list=new ArrayList<RepeatMatch>(); // each entry is a pair of [start,end] coordinates
       int workingsize=sequence.length()-minlength*2;
       int sequencelength=sequence.length();
       char[] expressionChars=new char[maxlength];
       for (int i=0;i<=workingsize;i++) {
            if (i%500==0) { // take a break for every 500 positions
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                Thread.yield();
            }
            searchpos:
            for (int halfsitesize=minlength;halfsitesize<=maxlength;halfsitesize++) {
                if (i+halfsitesize+halfsitesize>=sequencelength) break; // exceeding sequence limit
                // setup complement halfsite
                if (inverted) { // inverted repeat
                    for (int j=0;j<halfsitesize;j++) {
                        char c=Character.toUpperCase(sequence.charAt(i+j));
                             if (c=='A') expressionChars[(halfsitesize-1)-j]='T';
                        else if (c=='C') expressionChars[(halfsitesize-1)-j]='G';
                        else if (c=='G') expressionChars[(halfsitesize-1)-j]='C';
                        else if (c=='T') expressionChars[(halfsitesize-1)-j]='A';
                        else break searchpos; // skip positions where non-ACGT letters occur in the halfsite
                    }
                } else {  // direct repeat
                    for (int j=0;j<halfsitesize;j++) {
                        char c=Character.toUpperCase(sequence.charAt(i+j));
                        if (c!='A' && c!='C' && c!='G' && c!='T') break searchpos; // skip positions where non-ACGT letters occur in the halfsite
                        else expressionChars[j]=c;
                    }
                }
                for (int gap=mingap;gap<=maxgap;gap++) {
                    int miss=0;
                    if (i+halfsitesize+halfsitesize+gap>=sequencelength) break; // exceeding sequence limit:
                    for (int j=0;j<halfsitesize;j++) {
                        char c=Character.toUpperCase(sequence.charAt(i+halfsitesize+gap+j));
                        if (c!=expressionChars[j]) miss++;
                        if (miss>maxmismatch) break; // break this gap
                    }
                    if (miss<=maxmismatch) list.add(new RepeatMatch(i,i+halfsitesize+halfsitesize+gap-1,halfsitesize,gap,halfsitesize-miss));
                }
            }
             //System.err.println("["+expression+"] => "+sequence.substring(i,i+expressionChars.length)+"  misses="+miss);
       }
       return list;
   }
   
   /** Removes repeats from the list that are less specific than other repeats in the same list */
   private void filterRepeats(ArrayList<RepeatMatch> repeats, int minhalfsitelength, int maxgap) {
       ArrayList<RepeatMatch> toberemoved=new ArrayList<RepeatMatch>();
       for (RepeatMatch entry:repeats) {
           // entry[] contains start,end,halfsitelength,gaplength,#matching letters
           int start=entry.start;
           int end=entry.end;
           int halfsitesize=entry.halfsitesize;
           int gap=entry.gap;
           if (gap+2<=maxgap && halfsitesize-1>=minhalfsitelength) {// increased gap but same start/end
               toberemoved.add(new RepeatMatch(start,end,halfsitesize-1,gap+2,entry.matches-1));
               //System.err.println("Match"+entry+"  remove["+start+","+end+","+(halfsitesize-1)+","+(gap+2)+","+(entry.matches-1)+"]");
           }
            if (halfsitesize-1>=minhalfsitelength) {// same gap, smaller regions (increased start, decreased end)
               toberemoved.add(new RepeatMatch(start+1,end-1,halfsitesize-1,gap,entry.matches-1));
               //System.err.println("Match"+entry+"  remove["+(start+1)+","+(end-1)+","+(halfsitesize-1)+","+(gap)+","+(entry.matches-1)+"]");

            }
       }
       for (RepeatMatch entry:toberemoved) {
           repeats.remove(entry); // I don't know if this works
       }
   }

   private class RepeatMatch {
       int start=0;
       int end=0;
       int halfsitesize=0;
       int gap=0;
       int matches=0;

       public RepeatMatch(int start, int end, int halfsitesize, int gap, int matches) {
           this.start=start;
           this.end=end;
           this.halfsitesize=halfsitesize;
           this.gap=gap;
           this.matches=matches;
       }

       @Override
       public String toString() {
          return "["+start+","+end+","+halfsitesize+","+gap+","+matches+"]";
       }

       @Override
       public boolean equals(Object obj) {
           if (obj==null || !(obj instanceof RepeatMatch)) return false;
           RepeatMatch other=(RepeatMatch)obj;
           return (other.start==this.start && other.end==this.end && other.halfsitesize==this.halfsitesize && other.gap==this.gap && other.matches==this.matches);
       }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + this.start;
            hash = 29 * hash + this.end;
            hash = 29 * hash + this.halfsitesize;
            hash = 29 * hash + this.gap;
            hash = 29 * hash + this.matches;
            return hash;
        }
   }
   
   
     private class ProcessSearchSequenceTask implements Callable<RegionSequenceData> {
        final RegionDataset targetDataset;
        final DNASequenceDataset sourceDataset;
        final long[] counters; // NB: this array will be shared with other tasks since all tasks are given the same pointer
        final String sequencename;
        final OperationTask task;  
        
        public ProcessSearchSequenceTask(DNASequenceDataset sourceDataset, RegionDataset targetDataset, String sequencename, OperationTask task, long[] counters) {
           this.sequencename=sequencename;
           this.sourceDataset=sourceDataset;
           this.targetDataset=targetDataset; 
           this.counters=counters;
           this.task=task;
        }
         
        @Override
        @SuppressWarnings("unchecked")
        public RegionSequenceData call() throws Exception {
            synchronized(counters) {
               counters[0]++; // number of sequences started  
            }        
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            DNASequenceData sourceSequence=(DNASequenceData)sourceDataset.getSequenceByName(sequencename);
            RegionSequenceData targetSequence=(RegionSequenceData)targetDataset.getSequenceByName(sequencename);

            boolean repeats=((String)task.getParameter(SEARCH_EXPRESSION)).equals(SEARCH_REPEAT);
            if (repeats) searchRepeatsInSequence(sourceSequence, targetSequence, task);    
            else searchInSequence(sourceSequence,targetSequence, task);
            
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                task.setStatusMessage("Executing "+task.getOperationName()+":  ("+counters[1]+"/"+counters[2]+")");
                task.setProgress(counters[1],counters[2]);                
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();           
            return targetSequence;
        }   
    } 
     
}
