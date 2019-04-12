/*
 
 
 */

package motiflab.engine.protocol;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_increase extends StandardArithmeticsOperationParser {

    @Override
    public String getOperationName() {
        return "increase";
    }

    @Override
    public String getPreposition() {
        return "by";
    }
}
