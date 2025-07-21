/*
 
 
 */

package org.motiflab.engine.protocol;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_divide extends StandardArithmeticsOperationParser {

    @Override
    public String getOperationName() {
        return "divide";
    }

    @Override
    public String getPreposition() {
        return "by";
    }

}
