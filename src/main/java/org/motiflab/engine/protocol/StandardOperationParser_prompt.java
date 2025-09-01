/*
 
 
 */

package org.motiflab.engine.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.operations.Operation_prompt;
import org.motiflab.engine.operations.PromptConstraints;
/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_prompt extends StandardOperationParser {
    

    
    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String message=(String)task.getParameter(Operation_prompt.PROMPT_MESSAGE);
        String msg="prompt for "+sourceName;   
        if (message!=null && !message.isEmpty()) msg+=" \""+MotifLabEngine.escapeQuotedString(message)+"\""; // the message could contain quotes or other stuff that must be escaped
        PromptConstraints constraint=(PromptConstraints)task.getParameter(Operation_prompt.PROMPT_CONSTRAINTS);
        if (constraint!=null) {
            String constraintString=constraint.getConstraintString();
            if (constraintString!=null && !constraintString.isEmpty()) msg+=" "+constraintString;
        }
        return msg;
    }

    
    /**
     * This is an explanation of the regex that is used to parse strings within double quotes:   "((?:\\\\|\\\w|\\"|[^"\\])*?)"
     * This expression is basically "(x*?)" where the x's that are gobbled up by the expression can be one of four alternatives: 
     * 1) A double backslash, i.e. an escaped backslash:   \\
     * 2) A backslash followed by a letter (or more). This is a normal escaped character: \t
     * 3) A backslash followed by a double quote. This is an escaped quote:  \"
     * 4) A single character which is neither a quote nor a singular backslash
     * 
     * The three first cases handles all situations where a backslash is actually allowed (used for escaping)
     * The last case makes sure that backslashes are not used in other situations where it is not used for escaping
     * and also makes sure the regex does not match other internal quotes that are not escaped.
     */
    @Override
    public OperationTask parse(String command) throws ParseError {
           String operationName="prompt";
           String sourceName=null;
           String message=null;
           String constraints=null;
           
           Pattern pattern=Pattern.compile("^prompt( for)? ([a-zA-Z_0-9-]+)?(\\s+\"((?:\\\\\\\\|\\\\\\w|\\\\\"|[^\"\\\\])*?)\")?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               sourceName=matcher.group(2);
               message=matcher.group(4);
               constraints=matcher.group(5);
               if (constraints!=null && !constraints.isEmpty()) {
                   constraints=constraints.trim();
               } else constraints=null;
               // for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing name for target data object");
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           
           PromptConstraints promptconstraints=(constraints==null)?null:new PromptConstraints(sourceclass, constraints);                      
           if (message!=null && message.contains("\\")) { // message contains escaped characters. Convert these to regular stuff
               message = MotifLabEngine.unescapeQuotedString(message);
           }
           task.addAffectedDataObject(sourceName, sourceclass);           

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.TARGET_NAME, sourceName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(Operation_prompt.PROMPT_MESSAGE, message);
           if (promptconstraints!=null) task.setParameter(Operation_prompt.PROMPT_CONSTRAINTS, promptconstraints);
           return task;
    }

}
