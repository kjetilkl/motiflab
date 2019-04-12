/**
These "Location" DataFormat for specifying sequences can parse 3 different formats
Each line must contain either 10, 8 or 6 fields separated by tabs or commas (you can mix and match these formats within the file).


The 10-field format

[1] Name of sequence (this can be an arbitrary name)
[2] Organism taxonomy ID
[3] Genome build
[4] Chromosome
[5] Start of sequence region (genomic coordinate)
[6] End of sequence region  (genomic coordinate)
[7] Name of associated gene
[8] Transcription start site for the gene (genomic coordinate). This is optional and can be set to NULL
[9] Transcription end site for the gene (genomic coordinate). This is optional and can be set to NULL
[10] The strand the gene is taken from (DIRECT, +1, +) or (REVERSE, -1, -)

Examples
--------
NTNG1	9606	hg18	1	107482152	107484351	NTNG1	107484152	107827603	+
56475	9606	hg18	2	154043369	154045568	RPRM	154043568	154042098	-


The 8-field format

[1] Name of gene/sequence (this can be an arbitrary name)
[2] Genome build
[3] Chromosome
[4] Start of sequence region (genomic coordinate)
[5] End of sequence region  (genomic coordinate)
[6] Transcription start site for the gene (genomic coordinate). This is optional and can be set to NULL
[7] Transcription end site for the gene (genomic coordinate). This is optional and can be set to NULL
[8] The strand the gene is taken from (DIRECT, +1, +) or (REVERSE, -1, -)

Examples
--------
UNG	hg18	12	108017798	108019997	108019798	NULL	DIRECT
BRCA2	hg18	13	31785617	31787816	31787617	NULL	+


The 6-field format specifies the locations relative known genes

[1] Gene identifier (depending on format)
[2] Name of gene identifier format (eg. "Ensembl Gene", "Entrez Gene" or "HGNC Symbol")
[3] Genome build
[4] Start of sequence region relative to anchor point (see field #6)
[5] End of sequence region relative to anchor point (see field #6)
[6] Anchor point. This could either be "TSS" or "TES"

Examples
--------
NTNG1	HGNC Symbol	hg18	-2000	200	TSS
56475	Entrez Gene	hg18	-2000	200	TSS
ENSG00000111249	Ensembl Gene	hg18	-2000	200	TSS
ENSG00000187664	Ensembl Gene	hg18	-2000	200	TSS
ENSG00000196358	Ensembl Gene	hg18	-2000	200	TSS

 */

package motiflab.engine.dataformat;


import java.util.ArrayList;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.Data;
import motiflab.engine.data.Organism;
import motiflab.engine.data.OutputData;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.GeneIDmapping;
import motiflab.engine.GeneIDResolver;
import motiflab.engine.GeneIdentifier;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterCondition;




/**
 *
 * @author kjetikl
 */
public class DataFormat_Location extends DataFormat {
    public static int MANUAL_ENTRY_FIELDS_COUNT_10=10;    
    public static int MANUAL_ENTRY_FIELDS_COUNT_8=8;
    public static int MANUAL_ENTRY_FIELDS_COUNT_4=4;    
    public static int GENE_ID_FIELDS_COUNT=6;
  
    private String name="Location";
    private Class[] supportedTypes=new Class[]{Sequence.class, SequenceCollection.class};

    private static String OUTPUT_10_FIELDS="10 field format";
    private static String OUTPUT_8_FIELDS="8 field format";
    private static String OUTPUT_4_FIELDS="4 field format";
    private static String OUTPUT_4_FIELDS_RELATIVE="4 field format (relative)";
    private static String OUTPUT_CUSTOM="Custom format";
    private static String OUTPUT_BED="BED format";    
    private static String OUTPUT_STRAND_SIGN="+/-";
    private static String OUTPUT_STRAND_NUMBER="+1/-1";
    private static String OUTPUT_STRAND_TEXT="Direct/Reverse";

    public DataFormat_Location() {
        addParameter("Format", OUTPUT_10_FIELDS, new String[]{OUTPUT_10_FIELDS,OUTPUT_8_FIELDS,OUTPUT_4_FIELDS,OUTPUT_4_FIELDS_RELATIVE,OUTPUT_BED,OUTPUT_CUSTOM},"<html>Specifies which format to use for the output.<br><br>The 4-field format consists of the following TAB-separated fields:<br><br>[1] Name of sequence<br>[2] Chromosome<br>[3] Start of sequence region<br>[4] End of sequence region<br><br><tt>Examples of 4-field format</tt><br><tt>--------------------------</tt><br><tt>UNG     12   108017798   108019997</tt><br><tt>BRCA2   13    31785617    31787816</tt><br><br>The \"relative 4-field format\" is similar to the regular 4-field format with one exception:<br>if a sequence is associated with the reverse strand, the start and end coordinates will <br>be swapped so that the value in the 3rd column is larger than the value in the 4th column.<br><br><br>The 8-field format consists of the following TAB-separated fields:<br><br>[1] Name of sequence<br>[2] Genome build<br>[3] Chromosome<br>[4] Start of sequence region<br>[5] End of sequence region<br>[6] Transcription start site of the associated gene (can be NULL)<br>[7] Transcription end site of the associated gene (can be NULL)<br>[8] Strand orientation<br><br>The 10-field format is a slight extension of the 8-field format with<br>an additional taxonomy ID for the organism as column 2 and a \"gene name\"<br>(which can be different from the sequence name) as column 7.<br><br><tt>Examples of 8-field format</tt><br><tt>--------------------------</tt><br><tt>UNG&nbsp;&nbsp;&nbsp;&nbsp;hg18&nbsp;&nbsp;&nbsp;12&nbsp;&nbsp;&nbsp;108017798&nbsp;&nbsp;&nbsp;108019997&nbsp;&nbsp;&nbsp;108019798&nbsp;&nbsp;&nbsp;NULL&nbsp;&nbsp;&nbsp;DIRECT</tt><br><tt>BRCA2&nbsp;&nbsp;hg18&nbsp;&nbsp;&nbsp;13&nbsp;&nbsp;&nbsp;&nbsp;31785617&nbsp;&nbsp;&nbsp;&nbsp;31787816&nbsp;&nbsp;&nbsp;&nbsp;31787617&nbsp;&nbsp;&nbsp;NULL&nbsp;&nbsp;&nbsp;+</tt><br><br><tt>Examples of 10-field format</tt><br><tt>---------------------------</tt><br><tt>ENSG00000162631&nbsp;&nbsp;&nbsp;9606&nbsp;&nbsp;&nbsp;hg18&nbsp;&nbsp;&nbsp;1&nbsp;&nbsp;&nbsp;107482152&nbsp;&nbsp;&nbsp;107484351&nbsp;&nbsp;&nbsp;NTNG1&nbsp;&nbsp;&nbsp;107484152&nbsp;&nbsp;&nbsp;107827603&nbsp;&nbsp;&nbsp;+</tt><br><tt>56475&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;9606&nbsp;&nbsp;&nbsp;hg18&nbsp;&nbsp;&nbsp;2&nbsp;&nbsp;&nbsp;154043369&nbsp;&nbsp;&nbsp;154045568&nbsp;&nbsp;&nbsp;RPRM&nbsp;&nbsp;&nbsp;&nbsp;154043568&nbsp;&nbsp;&nbsp;154042098&nbsp;&nbsp;&nbsp;-</tt></html>");       
        addOptionalParameter("Custom format", "",null,"<html>If the 'Custom format' is selected you can specify the format manually in this box.<br><br>The following fields are recognized (case-insensitive):<br><br>&bull; Sequence name (or just 'name')<br>&bull; Gene name<br>&bull; Chromosome<br>&bull; Start<br>&bull; End<br>&bull; Relative start<br>&bull; Relative end<br>&bull; TSS<br>&bull; TES<br>&bull; Strand<br>&bull; Genome build (or just 'build')<br>&bull; Organism (or 'taxonomy')<br>&bull; Organism name<br>&bull; Organism latin name<br><br>Example: the custom format \"<tt>Sequence name,chromosome,start,end</tt>\"<br>will be similar to the standard 4-field format except that commas are used<br>instead of TABs to separate to fields (to insert TABs use the escape code: \\t ).</html>");
        addOptionalParameter("Add CHR prefix", "no", new String[]{"yes","no"},"<html>If selected, the prefix 'chr' will be added before the chromosome number.<br>E.g. chromosome '12' will be output as 'chr12'.</html>");
        addOptionalParameter("Strand notation", OUTPUT_STRAND_TEXT, new String[]{OUTPUT_STRAND_TEXT,OUTPUT_STRAND_NUMBER,OUTPUT_STRAND_SIGN},"The format to use for the strand orientation");        
        setParameterFilter("Add CHR prefix","output"); 
        setParameterFilter("Format","output");
        setParameterFilter("Custom format","output");
        setParameterFilter("Strand notation","output");
        try {
            ParameterCondition cond=new ParameterCondition("Format", "Format", "value=Custom format", false, "Custom format:show", "Custom format:hide");
            addParameterCondition(cond);
        } catch (Exception e) {
            System.err.println("SystemError: Unable to place condition on 'Format' parameter in 'Location' dataformat: "+e.getMessage());
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof Sequence || data instanceof SequenceCollection);
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(Sequence.class) || dataclass.equals(SequenceCollection.class));
    }

    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof Sequence || data instanceof SequenceCollection);
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(Sequence.class) || dataclass.equals(SequenceCollection.class));
    }


    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "txt";
    }
    @Override
    public String[] getSuffixAlternatives() {return new String[]{};}

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(5);
        String outputString="";
        String format=OUTPUT_10_FIELDS;
        String customformat="";
        boolean chrPrefix=false;
        String strandnotation=OUTPUT_STRAND_TEXT;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             //settings.applyConditions(defaults); // this must be called since we have conditions on the parameters. But since the only actions are "show" and "hide" this is not really necessary
             format=(String)settings.getResolvedParameter("Format",defaults,engine);
             customformat=(String)settings.getResolvedParameter("Custom format",defaults,engine);
             String chrString=(String)settings.getResolvedParameter("Add CHR prefix",defaults,engine);
             chrPrefix=(chrString.equalsIgnoreCase("yes") || chrString.equalsIgnoreCase("true"));
             strandnotation=(String)settings.getResolvedParameter("Strand notation",defaults,engine);
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           format=(String)getDefaultValueForParameter("Format");
           customformat=(String)getDefaultValueForParameter("Custom format");
           String chrString=(String)getDefaultValueForParameter("Add CHR prefix");
           chrPrefix=(chrString.equalsIgnoreCase("yes") || chrString.equalsIgnoreCase("true"));
           strandnotation=(String)getDefaultValueForParameter("Strand notation");           
        }        
        if (customformat==null) customformat="";
        if (customformat!=null) { // replace escape characters
            customformat=customformat.replace("\\\\", "\\"); // escaped \
            customformat=customformat.replace("\\t", "\t"); // escaped TAB
            customformat=customformat.replace("\\n", "\n"); // escaped newline           
        }         
        if (dataobject instanceof SequenceCollection) {
            SequenceCollection sequenceCollection=(SequenceCollection)dataobject;
            outputString=outputMultipleSequences(sequenceCollection, format, customformat, chrPrefix, strandnotation, task);
        } else if (dataobject instanceof Sequence){
            StringBuilder builder=new StringBuilder();
            outputSequence((Sequence)dataobject, format, customformat, chrPrefix, strandnotation, builder);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.trim()+"\n",getName());
        setProgress(100);
        return outputobject;
    }

    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(SequenceCollection collection, String format, String customformat, boolean chrPrefix, String strandnotation, ExecutableTask task) throws InterruptedException {
        StringBuilder outputString=new StringBuilder();
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence
              outputSequence(sequence, format, customformat, chrPrefix, strandnotation,outputString);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }
        return outputString.toString();
    }




    /** output formats a single sequence */
    private void outputSequence(Sequence sequence, String format, String customformat, boolean chrPrefix, String strandnotation, StringBuilder outputString) {
        if (format.equalsIgnoreCase(OUTPUT_10_FIELDS) || format.equalsIgnoreCase(OUTPUT_8_FIELDS)) {
            outputString.append(sequence.getName());
            outputString.append("\t");
            if (format.equalsIgnoreCase(OUTPUT_10_FIELDS)) {
                outputString.append(sequence.getOrganism());
                outputString.append("\t");
            }
            outputString.append(sequence.getGenomeBuild());
            outputString.append("\t");
            if (chrPrefix) outputString.append("chr");
            outputString.append(sequence.getChromosome());
            outputString.append("\t");
            outputString.append(sequence.getRegionStart());
            outputString.append("\t");
            outputString.append(sequence.getRegionEnd());
            outputString.append("\t");
            if (format.equalsIgnoreCase(OUTPUT_10_FIELDS)) {        
                outputString.append(sequence.getGeneName());
                outputString.append("\t");
            }
            Integer TSS=sequence.getTSS();
            outputString.append((TSS!=null)?TSS:"-");
            outputString.append("\t");
            Integer TES=sequence.getTES();
            outputString.append((TES!=null)?TES:"-");
            outputString.append("\t");
            int strand=sequence.getStrandOrientation();
            outputString.append(getStrand(strand,strandnotation));
            outputString.append("\n");
        } else if (format.equalsIgnoreCase(OUTPUT_4_FIELDS) || format.equalsIgnoreCase(OUTPUT_4_FIELDS_RELATIVE)) {
            boolean reverse=(format.equalsIgnoreCase(OUTPUT_4_FIELDS_RELATIVE) && sequence.getStrandOrientation()==Sequence.REVERSE);
            outputString.append(sequence.getName());
            outputString.append("\t");   
            if (chrPrefix) outputString.append("chr");
            outputString.append(sequence.getChromosome());            
            outputString.append("\t");
            outputString.append((reverse)?sequence.getRegionEnd():sequence.getRegionStart());
            outputString.append("\t");
            outputString.append((reverse)?sequence.getRegionStart():sequence.getRegionEnd());            
            outputString.append("\n");        
        } else if (format.equalsIgnoreCase(OUTPUT_BED)) {
            if (chrPrefix) outputString.append("chr");
            outputString.append(sequence.getChromosome());            
            outputString.append("\t");
            outputString.append(sequence.getRegionStart()-1); // convert to 0-indexed
            outputString.append("\t");
            outputString.append(sequence.getRegionEnd()); // "converted" to exclusive 0-indexed
            outputString.append("\t");  
            outputString.append(sequence.getName());           
            outputString.append("\t0\t");  // score !
            int strand=sequence.getStrandOrientation();            
            outputString.append(getStrand(strand,strandnotation));           
            outputString.append("\n");   
        } else { // assuming custom format
           outputSequenceInCustomFormat(sequence, customformat, chrPrefix, strandnotation, outputString);
        }
    }

    private void outputSequenceInCustomFormat(Sequence sequence, String customformat, boolean chrPrefix, String strandnotation, StringBuilder outputString) {
        String chromosomeString=sequence.getChromosome();
        if (chrPrefix) chromosomeString="chr"+chromosomeString;
        boolean reverse=(sequence.getStrandOrientation()==Sequence.REVERSE);
        String relativeStart=""+((reverse)?sequence.getRegionEnd():sequence.getRegionStart()); 
        String relativeEnd=""+((reverse)?sequence.getRegionStart():sequence.getRegionEnd()); 
        String strand=getStrand(sequence.getStrandOrientation(), strandnotation);
        int organism=sequence.getOrganism();        
        // remember to replace longer field codes that contains shorter field codes first (e.g. "Gene name" must be processed before "name")
        customformat=customformat.replaceAll("(?i)Chromosome",chromosomeString);
        customformat=customformat.replaceAll("(?i)Relative start",relativeStart);
        customformat=customformat.replaceAll("(?i)Relative end",relativeEnd);
        customformat=customformat.replaceAll("(?i)Start",""+sequence.getRegionStart());
        customformat=customformat.replaceAll("(?i)End",""+sequence.getRegionEnd());
        customformat=customformat.replaceAll("(?i)Strand",strand);
        customformat=customformat.replaceAll("(?i)Sequence name",sequence.getName());
        customformat=customformat.replaceAll("(?i)Gene name",sequence.getGeneName());
        customformat=customformat.replaceAll("(?i)name",sequence.getName());
        customformat=customformat.replaceAll("(?i)TSS",""+sequence.getTSS());
        customformat=customformat.replaceAll("(?i)TES",""+sequence.getTES());
        customformat=customformat.replaceAll("(?i)Genome build",""+sequence.getGenomeBuild());
        customformat=customformat.replaceAll("(?i)build",""+sequence.getGenomeBuild());
        customformat=customformat.replaceAll("(?i)Organism latin name",""+Organism.getLatinName(organism));
        customformat=customformat.replaceAll("(?i)Organism name",Organism.getCommonName(organism));
        customformat=customformat.replaceAll("(?i)Organism",""+organism);
        customformat=customformat.replaceAll("(?i)Taxonomy",""+organism);
        outputString.append(customformat); 
        outputString.append("\n"); 
    }
    
    private String getStrand(int strand, String notation) {
        if (notation.equalsIgnoreCase(OUTPUT_STRAND_SIGN)) {
            return (strand==Sequence.DIRECT)?"+":"-";
        } else if (notation.equalsIgnoreCase(OUTPUT_STRAND_NUMBER)) {
            return (strand==Sequence.DIRECT)?"+1":"-1";
        } else {
            return (strand==Sequence.DIRECT)?"Direct":"Reverse";
        }
    }
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) target=new SequenceCollection("temporary");           
        ArrayList<GeneIdentifier> geneIDs=new ArrayList<GeneIdentifier>();
        ArrayList<String[]> list=new ArrayList<String[]>();
        // locate all Gene ID entries, parse these and add GeneIdentifier object to list to be resolved
        for (int i=0;i<input.size();i++) {
            String line=input.get(i);
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] entry=splitLine(line.trim());
            list.add(entry);
            if (entry.length==GENE_ID_FIELDS_COUNT && !couldThisBeBED(entry)) { // line specifies a sequence using Gene ID
                geneIDs.add(processGeneIDLine(entry));
            }
        }
        ArrayList<GeneIDmapping> resolved=resolveGenesFromList(geneIDs);
        if (resolved==null) resolved=new ArrayList<GeneIDmapping>();
        // Process full list again, this time resolved Gene ID info should be available
        for (int i=0;i<list.size();i++) {
            String[] entry=list.get(i);
            if (entry.length==GENE_ID_FIELDS_COUNT && !couldThisBeBED(entry)) { // line specifies a sequence using Gene ID
                Object[] result=processGeneIDLineInFull(entry);
                GeneIdentifier geneid=(GeneIdentifier)result[0];
                int upstream=(Integer)result[1];
                int downstream=(Integer)result[2];
                String anchor=(String)result[3];
                ArrayList<Sequence> choice=resolveSequenceForGeneID(geneid,resolved,upstream,downstream,anchor); // There could be multiple matches to the same gene ID
                for (Sequence seq:choice) {
                   ((SequenceCollection)target).addSequenceToPayload(seq);
                }
            } else { // manual entry format (specifying coordinates not gene IDs)
                 Sequence seq=null;
                 try {
                          if (entry.length==MANUAL_ENTRY_FIELDS_COUNT_8) seq=Sequence.processManualEntryLine8(entry);
                     else if (entry.length==MANUAL_ENTRY_FIELDS_COUNT_10) seq=Sequence.processManualEntryLine10(entry);
                     else if (entry.length==MANUAL_ENTRY_FIELDS_COUNT_4) seq=Sequence.processManualEntryLine4(entry);
                     else if (couldThisBeBED(entry)) seq=Sequence.processBEDformat(entry);                    
                 } catch (ExecutionError e) {
                     throw new ParseError(e.getMessage());
                 }
                 if (seq!=null) {
                     String sequencename=seq.getName();
                     String fail=engine.checkSequenceNameValidity(sequencename, false);
                     if (fail!=null && engine.autoCorrectSequenceNames()) { // sequencename contains illegal characters that should be corrected  
                        String newsequencename=MotifLabEngine.convertToLegalSequenceName(sequencename);
                        seq.rename(newsequencename);                                 
                     }                       
                     if (seq.getSize()>engine.getMaxSequenceLength()) throw new ParseError("Warning: Size of sequence '"+sequencename+"' exceeds preset maximum ("+engine.getMaxSequenceLength()+" bp)");
                     else ((SequenceCollection)target).addSequenceToPayload(seq);
                 } else throw new ParseError("Wrong number of arguments in sequence file ("+entry.length+")",0);
            } 
        }
        return target;
    }
    
    /** Returns true if the first three fields is on the format: chrX start end */
    private boolean couldThisBeBED(String[] fields) {
        if (fields.length<3) return false;
        if (!fields[0].startsWith("chr")) return false;
        try {
            Integer.parseInt(fields[1]);
            Integer.parseInt(fields[2]);
            return true;
        } catch (NumberFormatException ne) {
            return false;
        }
    }

    private String[] splitLine(String line) throws ParseError {
        line=line.replace("\t", ",");
        String[] fields=line.split("\\s*,+\\s*");
//        if (fields.length==GENE_ID_FIELDS_COUNT || fields.length==MANUAL_ENTRY_FIELDS_COUNT_8 || fields.length==MANUAL_ENTRY_FIELDS_COUNT_10) {
//           return fields;
//        } else {
//           throw new ParseError("Number of fields on one line must be either "+GENE_ID_FIELDS_COUNT+", "+MANUAL_ENTRY_FIELDS_COUNT_8+" or "+MANUAL_ENTRY_FIELDS_COUNT_10+" (found "+fields.length+")\n=>"+line);
//        }
        return fields;
    }

    /** Processes a line with GENE_ID_FIELDS_COUNT String entries and returns a GeneIdentifier */
    private GeneIdentifier processGeneIDLine(String[] fields)  {
        String identifier=fields[0];
        String idFormat=fields[1];
        String build=fields[2];
        int organism=Organism.getOrganismForGenomeBuild(build);
        return new GeneIdentifier(identifier, idFormat, organism, build);
    }

    /**
     * Processes a line with GENE_ID_FIELDS_COUNT String entries and returns an object array
     * containing
     * [0] a GeneIdentifier,
     * [1] A 'from' (upstream) specification
     * [2] A 'to' (downstream) specification
     * [3] An anchor (eg. "TSS" or "TES")
     *
     */
    private Object[] processGeneIDLineInFull(String[] fields) throws ParseError{
        Object[] result=new Object[]{null,null,null,null};
        String identifier=fields[0];
        String idFormat=fields[1];
        String build=fields[2];
        int organism=Organism.getOrganismForGenomeBuild(build);
        result[0]=new GeneIdentifier(identifier, idFormat, organism, build);
        try {
            int from=Integer.parseInt(fields[3]);
            result[1]=new Integer(from);
        } catch (NumberFormatException ne) {
            throw new ParseError("Unable to parse expected numeric range-value for 'from' (value='"+fields[3]+"')");
        }
        try {
            int to=Integer.parseInt(fields[4]);
            result[2]=new Integer(to);
        } catch (NumberFormatException ne) {
            throw new ParseError("Unable to parse expected numeric range-value for 'to' (value='"+fields[4]+"')");
        }
        result[3]=fields[5];
        return result;
    }


    /** Resolves a list of GeneIdentifiers */
    private ArrayList<GeneIDmapping> resolveGenesFromList(ArrayList<GeneIdentifier> idlist) throws ParseError {
        GeneIDResolver idResolver=engine.getGeneIDResolver();
        ArrayList<GeneIDmapping> resolvedList=null;
        try {
            resolvedList=idResolver.resolveIDs(idlist);
        } catch (Exception e) {
            throw new ParseError("An error occurred while resolving gene IDs: "+e.getClass().getSimpleName()+": "+e.getMessage());
        }
        return resolvedList;
    }


    /**
     * Based on a GeneID specified by the user and a list of previously resolved gene IDs (among which the first argument geneid should hopefully be present)
     * the method returns a list of Sequence objects corresponding to the selected gene ID.
     * Note that when resolving Gene IDs, several different hits could be returned.
     * The user may be prompted to select the correct one (or several) and the ones the user selects are turned into Sequence objects
     * and returned as a list
     */
    private ArrayList<Sequence> resolveSequenceForGeneID(GeneIdentifier geneid, ArrayList<GeneIDmapping> resolvedList, int upstream, int downstream, String anchor) throws ParseError {
           ArrayList<Sequence> sequences=new ArrayList<Sequence>();
           ArrayList<GeneIDmapping> listForGene=getEntriesForID(resolvedList,geneid.identifier);
           if (listForGene.isEmpty()) {
               throw new ParseError("Unable to find information about "+Organism.getCommonName(geneid.organism).toLowerCase()+" "+geneid.format+" identifier: "+geneid.identifier);
           }
           for (GeneIDmapping mapping:listForGene) { // add all those mappings that are left for this ID
               Sequence sequence=new Sequence(mapping.geneID, new Integer(geneid.organism), geneid.build, mapping.chromosome, 0, 0, mapping.geneID, mapping.TSS, mapping.TES, mapping.strand);
               sequence.setUserDefinedPropertyValue(geneid.format, mapping.geneID);               
               fillInStartAndEndPositions(sequence, upstream, downstream, anchor);
               if (mapping.GOterms!=null && !mapping.GOterms.isEmpty()) {
                   try {sequence.setGOterms(mapping.GOterms);} catch (ParseError e) {} // The terms should have been checked many times already, so just ignore errors at this point
               }               
               if (sequence.getSize()>engine.getMaxSequenceLength()) {
                   throw new ParseError("Warning: Size of sequence '"+sequence.getName()+"' exceeds preset maximum ("+engine.getMaxSequenceLength()+" bp)");
               } if (sequence.getSize()<0) {
                   throw new ParseError("Warning: end position located prior to start position in sequence '"+sequence.getName()+"'");
               } else sequences.add(sequence);
           }
           return sequences;
    }


    /** Goes through a list of GeneIDmapping and returns only those entries that correspond to the given gene id */
    private ArrayList<GeneIDmapping> getEntriesForID(ArrayList<GeneIDmapping> list, String id) {
        ArrayList<GeneIDmapping> result=new ArrayList<GeneIDmapping>();
        for (GeneIDmapping entry:list) {
            if (entry.geneID.equals(id)) result.add(entry);
        }
        return result;
    }

/** Fills in upstream and downstream coordinates based on user selections and gene orientation */
    private void fillInStartAndEndPositions(Sequence sequence, int upstream, int downstream, String anchor) throws ParseError {
        if (upstream>0) upstream--; // to account for direct transition from -1 to +1 at TSS
        if (downstream>0) downstream--; // to account for direct transition from -1 to +1 at TSS
        int tss=sequence.getTSS();
        int tes=sequence.getTES();
        if (anchor.equalsIgnoreCase("Transcription Start Site") || anchor.equalsIgnoreCase("TSS")) {
            if (sequence.getStrandOrientation()==Sequence.DIRECT) {
               sequence.setRegionStart(tss+upstream);
               sequence.setRegionEnd(tss+downstream);
            } else { // Reverse Strand
               sequence.setRegionStart(tss-downstream);
               sequence.setRegionEnd(tss-upstream);
            }
        } else if (anchor.equalsIgnoreCase("Transcription End Site") || anchor.equalsIgnoreCase("TES")) {
            if (sequence.getStrandOrientation()==Sequence.DIRECT) {
               sequence.setRegionStart(tes+upstream);
               sequence.setRegionEnd(tes+downstream);
            } else { // Reverse Strand
               sequence.setRegionStart(tes-downstream);
               sequence.setRegionEnd(tes-upstream);
            }
        } else if (anchor.equalsIgnoreCase("gene") || anchor.equalsIgnoreCase("full gene") || anchor.equalsIgnoreCase("transcript")) {
            if (sequence.getStrandOrientation()==Sequence.DIRECT) {
               sequence.setRegionStart(tss+upstream);
               sequence.setRegionEnd(tes+downstream);
            } else { // Reverse Strand
               sequence.setRegionStart(tes-downstream);
               sequence.setRegionEnd(tss-upstream);
            }
        } else {
            throw new ParseError("Unsupported anchor site: "+anchor,0);
        }
    }




}





