/*
 
 
 */

package motiflab.external;

import java.util.ArrayList;
import motiflab.engine.Parameter;

/**
 * This class represents programs for module discovery using known single motif representations ("module discovery").
 * Wrappers for individual Module Discovery programs should subclass this class.
 * Subclasses should have names on the format "ModuleDiscovery_X" where X is the
 * name of the program
 *
 * @author kjetikl
 */
public class ModuleDiscovery extends ExternalProgram {


    /**
     *  This method returns true if this module discovery program returns a module collection with discovered modules
     *  and false if it only returns the a track with the module sites.
     */
    public boolean returnsModuleResults() {
        for (int i=0;i<getNumberOfResultParameters();i++) {
            if (getNameForResultParameter(i).equals("Modules")) return true;
        }
        return false;
    }
    
    /**
     *  This method returns true if this module discovery program returns a module track with discovered module sites
     *  and false if it only returns a collection with discovered modules.
     */
    public boolean returnsSiteResults() {
        for (int i=0;i<getNumberOfResultParameters();i++) {
            if (getNameForResultParameter(i).equals("Result")) return true;
        }
        return false;
    }    
    
    /**
     *  Returns true if this module discovery program can return additional results besides a module collection and module sites track
     */
    public boolean hasAdditionalResults() {
        for (int i=0;i<getNumberOfResultParameters();i++) {
            String paramName=getNameForResultParameter(i);
            if (!(paramName.equals("Result") || paramName.equals("Modules"))) return true;
        }
        return false;
    }     
    
    /**
     *  Returns 2 if this module discovery program returns both a module collection and a region track
     *  or 1 if it only returns one of these. (A result of 0 indicates that something is not right with the configuration)
     */
    public int getNumberOfRegularResults() {
        int count=0;
        for (int i=0;i<getNumberOfResultParameters();i++) {
            String paramName=getNameForResultParameter(i);
            if (paramName.equals("Result") || paramName.equals("Modules")) count++;
            if (count==2) break;
        }
        return count;
    }      
    
    /**
     *  Returns the number of additional results that can be returned by this module discovery program
     */
    public int getNumberOfAdditionalResults() {
        int count=0;
        for (int i=0;i<getNumberOfResultParameters();i++) {
            String paramName=getNameForResultParameter(i);
            if (!(paramName.equals("Result") || paramName.equals("Modules"))) count++;
        }
        return count;
    } 
    
    public ArrayList<Parameter> getAdditionalResultsParameters() {
        ArrayList<Parameter> list=new ArrayList<Parameter>(getNumberOfAdditionalResults());
        for (int i=0;i<resultsParameterFormats.size();i++) {
            Parameter param=resultsParameterFormats.get(i);
            String paramName=param.getName();
            if (!(paramName.equals("Result") || paramName.equals("Modules"))) list.add(param);
        }
        return list;
    }       
    
}
