/*
 
 
 */

package motiflab.external;

import java.util.ArrayList;
import motiflab.engine.Parameter;

/**
 * This class represents programs for "de novo" motif discovery. 
 * Wrappers for individual Motif Discovery programs should subclass this class.
 * Subclasses should have names on the format "MotifDiscovery_X" where X is the
 * name of the program (e.g. MotifDiscovery_MEME or MotifDiscovery_Priority)
 * 
 * @author kjetikl
 */
public class MotifDiscovery extends ExternalProgram {

    /**
     *  This method returns true if this motif discovery program returns a motif collection with discovered motifs
     *  and false if it only returns the a track with the binding sites.
     */
    public boolean returnsMotifResults() {
        for (int i=0;i<getNumberOfResultParameters();i++) {
            if (getNameForResultParameter(i).equals("Motifs")) return true;
        }
        return false;
    }
    
    /**
     *  This method returns true if this motif discovery program returns a motif track with discovered binding sites
     *  and false if it only returns a collection with discovered motifs.
     */
    public boolean returnsSiteResults() {
        for (int i=0;i<getNumberOfResultParameters();i++) {
            if (getNameForResultParameter(i).equals("Result")) return true;
        }
        return false;
    }    
    
    /**
     *  Returns true if this motif discovery program can return additional results besides a motif collection and TFBS track
     */
    public boolean hasAdditionalResults() {
        for (int i=0;i<getNumberOfResultParameters();i++) {
            String paramName=getNameForResultParameter(i);
            if (!(paramName.equals("Result") || paramName.equals("Motifs"))) return true;
        }
        return false;
    }     
    
    /**
     *  Returns 2 if this motif discovery program returns both a motif collection and a TFBS track
     *  or 1 if it only returns one of these. (A result of 0 indicates that something is not right with the configuration)
     */
    public int getNumberOfRegularResults() {
        int count=0;
        for (int i=0;i<getNumberOfResultParameters();i++) {
            String paramName=getNameForResultParameter(i);
            if (paramName.equals("Result") || paramName.equals("Motifs")) count++;
            if (count==2) break;
        }
        return count;
    }     
    
    /**
     *  Returns the number of additional results that can be returned by this motif discovery program
     */
    public int getNumberOfAdditionalResults() {
        int count=0;
        for (int i=0;i<getNumberOfResultParameters();i++) {
            String paramName=getNameForResultParameter(i);
            if (!(paramName.equals("Result") || paramName.equals("Motifs"))) count++;
        }
        return count;
    } 
    
    public ArrayList<Parameter> getAdditionalResultsParameters() {
        ArrayList<Parameter> list=new ArrayList<Parameter>(getNumberOfAdditionalResults());
        for (int i=0;i<resultsParameterFormats.size();i++) {
            Parameter param=resultsParameterFormats.get(i);
            String paramName=param.getName();
            if (!(paramName.equals("Result") || paramName.equals("Motifs"))) list.add(param);
        }
        return list;
    }     
    
}
