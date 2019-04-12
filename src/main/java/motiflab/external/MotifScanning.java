/*
 
 
 */

package motiflab.external;

import java.util.ArrayList;
import motiflab.engine.Parameter;

/**
 * This class represents programs for motif discovery using known motif models ("motif scanning"). 
 * Wrappers for individual Motif Scanning programs should subclass this class.
 * Subclasses should have names on the format "MotifScanning_X" where X is the
 * name of the program (e.g. MotifScanning_MATCH or MotifScanning_MotifScanner)
 * 
 * @author kjetikl
 */
public class MotifScanning extends ExternalProgram {

    
    /**
     *  Returns true if this motif scanning program can return additional results besides a TFBS track
     */
    public boolean hasAdditionalResults() {
        for (int i=0;i<getNumberOfResultParameters();i++) {
            String paramName=getNameForResultParameter(i);
            if (!(paramName.equals("Result"))) return true;
        }
        return false;
    }     
    
    /**
     *  Returns the number of additional results that can be returned by this motif scanning program
     */
    public int getNumberOfAdditionalResults() {
        int count=0;
        for (int i=0;i<getNumberOfResultParameters();i++) {
            String paramName=getNameForResultParameter(i);
            if (!(paramName.equals("Result"))) count++;
        }
        return count;
    } 
    
    public ArrayList<Parameter> getAdditionalResultsParameters() {
        ArrayList<Parameter> list=new ArrayList<Parameter>(getNumberOfAdditionalResults());
        for (int i=0;i<resultsParameterFormats.size();i++) {
            Parameter param=resultsParameterFormats.get(i);
            String paramName=param.getName();
            if (!(paramName.equals("Result"))) list.add(param);
        }
        return list;
    }  
    
    
}
