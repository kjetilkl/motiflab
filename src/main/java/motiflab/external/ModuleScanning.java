/*
 
 
 */

package motiflab.external;

import java.util.ArrayList;
import motiflab.engine.Parameter;

/**
 * This class represents programs for module discovery using known module models ("module scanning").
 * Wrappers for individual Module Scanning programs should subclass this class.
 * Subclasses should have names on the format "ModuleScanning_X" where X is the
 * name of the program
 *
 * @author kjetikl
 */
public class ModuleScanning extends ExternalProgram {

    /**
     *  Returns true if this module scanning program can return additional results besides a module sites track
     */
    public boolean hasAdditionalResults() {
        for (int i=0;i<getNumberOfResultParameters();i++) {
            String paramName=getNameForResultParameter(i);
            if (!(paramName.equals("Result"))) return true;
        }
        return false;
    }     
    
    /**
     *  Returns the number of additional results that can be returned by this module scanning program
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
