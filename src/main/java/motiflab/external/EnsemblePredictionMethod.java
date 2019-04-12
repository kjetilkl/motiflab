/*
 
 
 */

package motiflab.external;

import java.util.ArrayList;
import motiflab.engine.Parameter;

/**
 *
 * @author Kjetil
 */
public class EnsemblePredictionMethod extends ExternalProgram {

  /**
     *  This method returns true if this ensemble program returns a motif collection with discovered motifs
     *  and false if it only returns the a track with the binding sites.
     */
    public boolean returnsMotifResults() {
        for (int i=0;i<getNumberOfResultParameters();i++) {
            if (getNameForResultParameter(i).equals("Motifs")) return true;
        }
        return false;
    }
    
    /**
     *  This method returns true if this ensemble program returns a motif track with discovered binding sites
     *  and false if it only returns a collection with discovered motifs.
     */
    public boolean returnsSiteResults() {
        for (int i=0;i<getNumberOfResultParameters();i++) {
            if (getNameForResultParameter(i).equals("Result")) return true;
        }
        return false;
    }    
    
    /**
     *  Returns true if this ensemble program can return additional results besides a motif collection and TFBS track
     */
    public boolean hasAdditionalResults() {
        for (int i=0;i<getNumberOfResultParameters();i++) {
            String paramName=getNameForResultParameter(i);
            if (!(paramName.equals("Result") || paramName.equals("Motifs"))) return true;
        }
        return false;
    }     
    
    /**
     *  Returns 2 if this ensemble program returns both a motif collection and a TFBS track
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
     *  Returns the number of additional results that can be returned by this ensemble program
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
