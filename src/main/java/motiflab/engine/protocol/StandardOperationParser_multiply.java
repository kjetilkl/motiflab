/*
 
 
 */

package motiflab.engine.protocol;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_multiply extends StandardArithmeticsOperationParser {

    @Override
    public String getOperationName() {
        return "multiply";
    }

    @Override
    public String getPreposition() {
        return "by";
    }
}
