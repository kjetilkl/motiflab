/*
 
 
 */

package motiflab.engine.protocol;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_set extends StandardArithmeticsOperationParser {

    @Override
    public String getOperationName() {
        return "set";
    }

    @Override
    public String getPreposition() {
        return "to";
    }
}