
package org.motiflab.engine.dataformat;

import java.util.ArrayList;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.protocol.ParseError;

/**
 * This interface can be implemented by Data Formats and signals that the data format can parse a file containing DNA sequences (e.g. FASTA and FASTQ)
 * Formats implementing this interface can be used by the "Add Sequence From File" option in the GUI
 * @author kjetikl
 */
public interface DNASequenceParser {
    
    /** This method parses a document (provided as a list of strings) containing DNA sequences and returns a DNASequenceDataset representing these sequences */ 
    public DNASequenceDataset parseDNASequenceDataset(ArrayList<String> input, String datasetname, ParameterSettings settings) throws ParseError, InterruptedException;
     
}
