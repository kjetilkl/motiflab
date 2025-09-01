/*
 
 
 */

package org.motiflab.engine.protocol;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_decrease extends StandardArithmeticsOperationParser {

    @Override
    public String getOperationName() {
        return "decrease";
    }

    @Override
    public String getPreposition() {
        return "by";
    }

}
