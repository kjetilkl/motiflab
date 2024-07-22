/*
 
 
 */

package motiflab.engine.protocol;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import motiflab.engine.task.OperationTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import motiflab.engine.task.AddSequencesTask;
import motiflab.engine.task.CompoundTask;
import motiflab.engine.operations.*;
import motiflab.engine.task.ProtocolTask;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.SystemError;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.task.ConditionalTask;
import motiflab.engine.task.DisplaySettingTask;


/**
 * This class represents a protocol in the "standard protocol language".
 * The protocol is basically a text document containing the protocol script
 * with one command on each line. All or parts of the protocol script can be
 * "parsed" to return a ProtocolTask that can be executed.
 * 
 * 
 * @author kjetikl
 */
public final class StandardProtocol extends Protocol {
    private String name="protocol";
    private HashMap<String,OperationParser> operationParsers;
    private ParametersParser parametersParser=null;
    private DisplaySettingsParser displaySettingsParser=null;
    private DataTypeTable dataTypeLookupTable;
    private ArrayList<String[]> macros=null;
    
    private PlainDocument document;
    private String operationParserClassPrefix="motiflab.engine.protocol.StandardOperationParser_";
    private boolean isDirtyFlag=false;
    private String savedFileName=null;    
 
    /**
     * Creates a new empty instance of StandardProtocol
     * @param engine
     */
    public StandardProtocol(MotifLabEngine engine) {
         super(engine);
         initialize();
         document=new PlainDocument();      
         register();
    }
    
    /**
     * Creates a new instance of StandardProtocol
     * @param engine
     * @param text If this is not NULL the text will serve as the contents for the protocol
     */
    public StandardProtocol(MotifLabEngine engine, String text) {
         super(engine);
         initialize();
         document=new PlainDocument();
         if (text!=null) try {
             document.insertString(0, text, null);
         } catch (BadLocationException e) {}
         register();
    }
    
    /**
     * Creates a new instance of StandardProtocol
     * @param engine
     * @param text If this is not NULL the text will serve as the contents for the protocol
     */
    public StandardProtocol(MotifLabEngine engine, ArrayList<String> text) {
         super(engine);
         initialize();
         document=new PlainDocument();
         if (text!=null) try {
             for (String line:text) {
                if (!line.endsWith("\n")) line=line+"\n";
                document.insertString(document.getLength(), line, null);
             }         
         } catch (BadLocationException e) {}
         register();
    }    
    
    /**
     * Creates a new instance of StandardProtocol
     * @param engine
     * @param stream A stream which will be read to provide the initial contents of the protocol
     */
    public StandardProtocol(MotifLabEngine engine, InputStream stream) throws SystemError {
         super(engine);
         initialize();
         document=new PlainDocument();
         StringBuilder builder=new StringBuilder();
         java.io.BufferedReader inputStream=null;
         try {
            inputStream=new java.io.BufferedReader(new java.io.InputStreamReader(stream));
            String line;
            while((line=inputStream.readLine())!=null) {
                builder.append(line);
                builder.append("\n");
            }
            inputStream.close();
            stream.close();
          } catch (Exception e) {
            throw new SystemError(e.getClass().getSimpleName()+":"+e.getMessage());
          } finally {
           try {
               if (inputStream!=null) inputStream.close();
               if (stream!=null) stream.close();
           } catch (Exception ne) {}
         }         
         if (builder.length()>0) try {
             document.insertString(0, builder.toString(), null);
         } catch (BadLocationException e) {}
         register();
    }    
    
   /**
     * Creates a new instance of StandardProtocol based on a serialized object
     * @param engine
     * @param serializedProtocol
     */
    public StandardProtocol(MotifLabEngine engine, SerializedStandardProtocol serializedProtocol) {
        super(engine);
        initialize();
        setName(serializedProtocol.name);
        this.isDirtyFlag=serializedProtocol.isDirtyFlag;
        this.savedFileName=serializedProtocol.savedFileName;
        document=new PlainDocument();
        if (serializedProtocol.text!=null) try {
             document.insertString(0, serializedProtocol.text, null);
        } catch (BadLocationException e) {}
        register();     
    }
    
   /**
     * Creates a new instance of StandardProtocol based on a serialized object and a PlainDocument
     * The PlainDocument should contain the protocol text 
     * @param engine
     * @param serializedProtocol
     */
    public StandardProtocol(MotifLabEngine engine, SerializedStandardProtocol serializedProtocol, PlainDocument usedocument) {
        super(engine);
        initialize();
        setName(serializedProtocol.name);
        this.isDirtyFlag=serializedProtocol.isDirtyFlag;
        this.savedFileName=serializedProtocol.savedFileName;
        document=usedocument;
        register();     
    }
    
   
    private void initialize() {
        operationParsers=new HashMap<String, OperationParser>();
        dataTypeLookupTable=new DataTypeTable(engine);        
    }
      
    
    /**
     * Initializes some necessary components that the protocol requires
     */
    private void register() {
         registerParametersParser(new StandardParametersParser(engine,this));
         registerDisplaySettingsParser(new StandardDisplaySettingsParser(this));
         for (Operation operation:engine.getAllOperations()) {
             String operationname=operation.getName();
             String classname=operationParserClassPrefix+operationname;
             try { 
                Class parserClass=Class.forName(classname);
                OperationParser parser=(OperationParser)parserClass.newInstance();
                parser.setEngine(engine);
                parser.setProtocol(this);
                registerOperationParser(operationname, parser);
           } catch(ClassNotFoundException cnfe) {
               System.err.println("SYSTEM ERROR: No StandardOperationParser found for operation: "+operationname);
           } catch(InstantiationException ie) {
               System.err.println("SYSTEM ERROR: InstantiationException for StandardOperationParser: "+operationname);
           } catch(IllegalAccessException iae) {
               System.err.println("SYSTEM ERROR: IllegalAccessException for StandardOperationParser: "+operationname);
           }             
         }            
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name=name;
    }
    
    @Override
    public DataTypeTable getDataTypeLookupTable() {
        return dataTypeLookupTable;
    }    
    
    
    @Override
    public Document getDocument() {
        return document;
    }    
    
    @Override
    public ProtocolTask parse() throws ParseError {    
        Element root=document.getDefaultRootElement();
        int linecount=root.getElementCount();
        ProtocolTask task=parse(0,linecount-1);
        task.setIsPartial(false);
        return task;
    }
    
    @Override
    public ProtocolTask parse(int start, int end) throws ParseError {
        getDataTypeLookupTable().clear(); 
        getDataTypeLookupTable().populateFromEngine();
        ProtocolTask scriptTask=new ProtocolTask(getName());
        scriptTask.setParameter("_protocol", StandardProtocol.this); // include a reference to the protocol, just in case
        Element root=document.getDefaultRootElement();
        int linecount=root.getElementCount();
        if (start<0 || start>=linecount) throw new ParseError("Start of protocol section out of range");
        if (end<0 || end>=linecount) throw new ParseError("End of protocol section out of range");
        if (start>0 || end<linecount-1) scriptTask.setIsPartial(true);
        macros=engine.getMacros(); 
        ConditionalTask flowControlTask=null; // this is used for nesting tasks when the protocol contains flow control statements
        for (int i=start;i<=end;i++) {
           Element element=root.getElement(i);           
           String line;
           int linenumber=(i+1); // line numbering starts at 1
           try {
              line=document.getText(element.getStartOffset(),element.getEndOffset()-element.getStartOffset());
           } catch(BadLocationException ble) {throw new ParseError(ble.getMessage(),linenumber);}
           line=preprocess(line); // this will trim the line and remove comments
           if (line.isEmpty()) continue; // this also applies to comment lines starting with # since they are converted to blanks in the preprocessing step          
           ExecutableTask newtask=null; // 'newtask' represents a single command line (or a compound resulting from an expanded macro)
           
           // --- check if this line is a control statement               
           if (line.startsWith("if ")) { // start new condition
                Condition_basic condition=getFlowControlCondition(line, linenumber);
                ConditionalTask currentTask=new ConditionalTask("conditional task"); // this is a new "top level" block
                currentTask.setLineNumber(linenumber);
                CompoundTask runningBlock=new CompoundTask(line);    
                runningBlock.setLineNumber(linenumber); // Note that this line number now starts with the flow-control element
                currentTask.addConditionalTask(condition, runningBlock); // The subsequent lines will be added to the runningBlock compound until another flow control statement is encountered
                if (flowControlTask!=null) { // we already have a flowControlTask. This must mean that we have a nested if-statement
                    flowControlTask.addTaskToLastConditionBlock(currentTask);
                    flowControlTask=currentTask;
                } else flowControlTask=currentTask;
                continue;
           } else if (line.startsWith("else if") || line.equals("else")) { // these are really the same except that 'else' has condition==null. Add new blocks to current condition
                if (flowControlTask==null) throw new ParseError("Encountered 'else' without 'if'",linenumber); // the flowControlTask is instantiated when encountering 'if' 
                if (line.equals("else") && flowControlTask.hasEmptyCondition()) throw new ParseError("Encountered second 'else' clause for same 'if'",linenumber);
                Condition_basic condition=getFlowControlCondition(line, linenumber);
                CompoundTask runningBlock=new CompoundTask(line);   
                runningBlock.setLineNumber(linenumber); // Note that this line number now starts with the flow-control element       
                flowControlTask.addConditionalTask(condition, runningBlock); // The subsequent lines will be added to the runningBlock compound until another flow control statement is encountered                  
                continue;
           } else if (line.equals("end if")) {
                if (flowControlTask==null) throw new ParseError("Encountered 'end if' without 'if'",linenumber);
                ExecutableTask parentTask=flowControlTask.getParentTask();               
                if (parentTask==null) { // this is the toplevel task. We must add it to the script
                    scriptTask.addTask(flowControlTask);
                    flowControlTask=null; // the scripTask has now taken over the previous object, so we can reset this in case we need it again;
                } else if (parentTask instanceof CompoundTask) { // this flowControlTask is nested, we must unravel it one level
                    ExecutableTask grandParent=parentTask.getParentTask();
                    if (grandParent instanceof ConditionalTask) flowControlTask=(ConditionalTask)grandParent;
                    else throw new ParseError("I lost my way while parsing :-(    (Error: 241)",linenumber);
                } else throw new ParseError("I lost my way while parsing :-(    (Error: 243)",linenumber);
                continue;
           } 
           // regular line (not flow control). Check for macros
           else if (containsMacros(line, macros)) { 
                ArrayList<String> lines=processMacros(line, macros); // this will expand known macros in the line, but not alter the current macro definitions. The parseCommandString calls coming later can add/replace macros, however.
                if (lines.size()>1) { // macro expansion resulted in multiple lines
                   newtask=new CompoundTask("compound task");
                   for (String cline:lines) {
                      ExecutableTask nestedtask=parseCommandString(cline, linenumber); // note that all of these expanded commands will still reference the same line number in the protocol
                      ((CompoundTask)newtask).addTask(nestedtask);
                   }
                } else { // just a single command line
                   newtask=parseCommandString(lines.get(0), linenumber);
                }
           } else { // regular line. no macros
                newtask=parseCommandString(line, linenumber);        
           }
           if (flowControlTask!=null) flowControlTask.addTaskToLastConditionBlock(newtask); // we are currently in a nested block
           else scriptTask.addTask(newtask); // 
        } // end: for each protocol line
        
        if (flowControlTask!=null) throw new ParseError("Missing end for flow control block starting on line "+flowControlTask.getLineNumber());
        return scriptTask;
    }
    
    private Condition_basic getFlowControlCondition(String line, int lineNumber) throws ParseError {
        String conditionExpression="";
        if (line.startsWith("if ")) conditionExpression=line.substring("if ".length()).trim();
        else if (line.startsWith("else if ")) conditionExpression=line.substring("else if ".length()).trim();
        else if (line.equals("else")) return null; // NULL condition is always satisfied
        if (conditionExpression.isEmpty()) throw new ParseError("Missing condition expression",lineNumber);
        StandardOperationParser opParser=(StandardOperationParser)getOperationParser("filter"); // the condition parser is in the (abstract) StandardOperationParser superclass. I  just use the one in the filter operation parser
        if (opParser==null) throw new ParseError("SystemError: missing parser for flow control condition"); // should not happen
        Condition_basic condition=opParser.parseBasicCondition(conditionExpression);        
        return condition;
    }
    
    /**
     * Parses a single line in the protocol and returns an ExecutableTask for the command (if it can be executed).
     * This method is called when the user wants to execute (or edit) a single line from the protocol
     */
    @Override    
    public ExecutableTask parseCommand(int linenumber, boolean clearTypeTable) throws ParseError { // 
        Element root=document.getDefaultRootElement();
        int linecount=root.getElementCount();
        if (linenumber>linecount) return null;
        if (clearTypeTable) {
            getDataTypeLookupTable().clear(); 
            getDataTypeLookupTable().populateFromEngine();
            for (int i=0;i<linenumber-1;i++) { // this will populateFromEngine the DataTypeTable correctly up to the line in question
              Element e=root.getElement(i);  
              parseCommandAtNthLine(e);
            }
        }
        Element element=root.getElement(linenumber-1);           
        String line;
        try {
           line=document.getText(element.getStartOffset(),element.getEndOffset()-element.getStartOffset());
        } catch(BadLocationException ble) {throw new ParseError(ble.getMessage());}
        //System.err.println("PARSING line: "+line);
        line=preprocess(line);   
        if (line.isEmpty() || isControlStatement(line)) return null;
        macros=engine.getMacros();
        if (containsMacros(line, macros)) {
            ArrayList<String> lines=processMacros(line, macros);
            if (lines.size()>1) { // macro expansion resulted in multiple lines
               CompoundTask compoundtask=new CompoundTask("compound task");
               compoundtask.setParameter("_protocol", StandardProtocol.this); // include a reference to the protocol, just in case
               for (String cline:lines) {
                  ExecutableTask nestedtask=parseCommandString(cline, linenumber); 
                  compoundtask.addTask(nestedtask);
               }               
               return compoundtask; 
            } else { // just a single command line
               ExecutableTask task=parseCommandString(lines.get(0), linenumber);
               task.setParameter("_protocol", StandardProtocol.this); // include a reference to the protocol, just in case
               return task;  
            }
        } else { // no macros
            ExecutableTask task=parseCommandString(line, linenumber);
            task.setParameter("_protocol", StandardProtocol.this); // include a reference to the protocol, just in case
            return task;            
        }
        
    }
    
    /**
     * Parses a single command line provided as argument and returns an ExecutableTask representing this command
     * This method is called by all the other parsing methods (besides those that only parse to validate the protocol or to populate data tables)
     * @param line The single command line to be executed (containing no remaining unexpanded macros)
     * @param linenumber This number of the line in the protocol that the command originates from (used for error reporting)
     * @return
     * @throws ParseError 
     */
    private ExecutableTask parseCommandString(String line, int linenumber) throws ParseError {
       try {
           ExecutableTask task=null;
           if (line.startsWith("@") || line.startsWith("$") || line.startsWith("!")) {
                 task=getDisplaySettingsParser().parse(line);
                 if (((DisplaySettingTask)task).getSettingName().equalsIgnoreCase("macro")) { // command defines a macro: add it to the current set
                     String macroname=((DisplaySettingTask)task).getTargets();
                     String macrodefinition=(String)((DisplaySettingTask)task).getValue();
                     boolean force=((DisplaySettingTask)task).shouldForce();
                     if (force || !isMacroDefined(macroname)) {
                         replaceCurrentMacro(macroname, macrodefinition); // this adds the macro to the protocol's internal set used for subsequent pre-processing, but the macro will not be added to the "global" macro set until this protocol command is actually executed
                     }
                 }
           } 
           else if (isControlStatement(line)) return null;
           else {
               String operationName=null;
               String[] splitOnWithinRange=line.split(" within\\s?\\[");
               if (splitOnWithinRange.length>2) throw new ParseError("Multiple within-clauses",linenumber);
               String cmdline=splitOnWithinRange[0];           
               Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9.+-]+?|\\[([a-zA-Z_0-9,\\s.+-]+?)\\])\\s*=\\s*)?(\\w+)");
               Matcher matcher=pattern.matcher(cmdline);
               if (matcher.find()) {
                   operationName=matcher.group(4);
               } else throw new ParseError("Unable to parse line "+linenumber+":\n"+line);
               Operation operation=engine.getOperation(operationName);
               if (operation==null) throw new ParseError("Unrecognized operation '"+operationName+"'",linenumber);
               OperationParser opparser=getOperationParser(operationName);
               if (opparser==null) throw new ParseError("SYSTEM ERROR: Missing parser for '"+operationName+"'",linenumber);
               task=opparser.parse(cmdline);
               if (splitOnWithinRange.length>1) {
                    if (!splitOnWithinRange[1].endsWith("]")) throw new ParseError("Within-clause missing end bracket",linenumber);
                    if (splitOnWithinRange[1].length()<=1) throw new ParseError("Empty within-clause",linenumber);
                    String withinclause=splitOnWithinRange[1].substring(0, splitOnWithinRange[1].length()-1);            
                    if (!operation.isSubrangeApplicable()) throw new ParseError("The '"+operationName+"' operation can not be applied to window selections",linenumber);
                    ((StandardOperationParser)opparser).parseWithinCondition(withinclause,(OperationTask)task); // this will set the within condition in the task
               }                             
           }
           task.setLineNumber(linenumber);
           return task;
       } catch (ParseError vperror) {
           throw new ParseError(vperror.getMessage(),linenumber);
       }       
    }
    
    
    /** 
     * This method is called by parseCommand(N) in order to parse previous lines (from 1 to N-1) 
     * in the script and populateFromEngine the DataTypeTable so that the type-"state" of all variables in 
     * DataTypeTable corresponds to the environment at line N
     * The method does not produce any OperationTasks
     */
    private void parseCommandAtNthLine(Element element) {          
        String line;
        try {
           line=document.getText(element.getStartOffset(),element.getEndOffset()-element.getStartOffset());
        } catch(BadLocationException ble) {return;}
        line=preprocess(line);   
        if (line.isEmpty()) return;
        if (line.startsWith("@") || line.startsWith("$") || line.startsWith("!")) return; // this is a "display setting" command not an operation
        if (isControlStatement(line)) return;
        String operationName=null;
        Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9.+-]+?|\\[([a-zA-Z_0-9,\\s.+-]+?)\\])\\s*=\\s*)?(\\w+)");
        Matcher matcher=pattern.matcher(line);
        if (matcher.find()) {
            operationName=matcher.group(4);
        } else return;
        Operation operation=engine.getOperation(operationName);
        if (operation==null) return;
        OperationParser opparser=getOperationParser(operationName);
        if (opparser==null) return;
        try {opparser.parse(line);} catch (ParseError e) {} // this call can throw a ParseError        
    }

    
    @Override
    public DataTypeTable getDataTypeStateAtCommand(int commandNumber) {
        DataTypeTable typetable=getDataTypeLookupTable();
        typetable.clear(); 
        typetable.populateFromEngine();
        Element root=document.getDefaultRootElement();
        int linecount=root.getElementCount();
        if (commandNumber>linecount) commandNumber=linecount;
        for (int i=0;i<commandNumber;i++) { 
             Element e=root.getElement(i);  
             parseCommandAtNthLine(e);
        }   
        return typetable;
    }
  
    /** Returns TRUE if the line represents a control statement (such as "if-else" construct) rather than a regular operation, comment or display setting */
    private boolean isControlStatement(String line) {
        return (line.startsWith("if ") || line.startsWith("else if ") || line.equals("else") || line.equals("end if"));
    }
    
    @Override
    public boolean appendOperation(ExecutableTask task) {
        try {
            String cmd=getCommandString(task)+"\n";
            document.insertString(document.getLength(), cmd, null);
            return true;
        } catch(Exception e) {
            System.err.println("WARNING: exception in protocol.appendOperation: "+e.getClass().getSimpleName()+" => "+e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }



    @Override
    public boolean insertOperationAt(ExecutableTask task, int pos) {
        try {
            String cmd=getCommandString(task)+"\n";
            document.insertString(pos, cmd, null);
            return true;
        } catch(Exception e) {
            System.err.println("WARNING: exception in protocol.insertOperationAt: "+e.getClass().getSimpleName()+" => "+e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    public boolean insertStringAt(String string, int pos) {
        try {
            document.insertString(pos, string, null);
            return true;
        } catch(Exception e) {
            System.err.println("WARNING: exception in protocol.insertStringAt: "+e.getClass().getSimpleName()+" => "+e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }   
    
    /** Returns the text between the start and end position in the protocol */
    public String getText(int start, int end) {
        try {
            return document.getText(start, (end-start+1));
        } catch(Exception e) {
            return null;
        }
    } 
    /** Replaces the text with given length starting at the given position int the protocol with the replacement text */
    public boolean replaceText(int start, int length, String replacement) throws BadLocationException {
          document.replace(start, length, replacement, null);
          return true;
    }      
    
    
    @Override
    public String getCommandString(ExecutableTask task) {
        if (task instanceof CompoundTask) {
            CompoundTask block = (CompoundTask)task;
            StringBuilder protocolstring=new StringBuilder();
            ArrayList<ExecutableTask> list=block.getTaskList();
            for (ExecutableTask ex:list) {
                protocolstring.append(ex.getCommandString(this));
                protocolstring.append("\n"); // no block delimiters at this time...
            }
            return protocolstring.toString().trim();
        } else if (task instanceof OperationTask) {
            Operation op=((OperationTask)task).getOperation();
            OperationParser opparser=getOperationParser(op.getName());
            String command=opparser.getCommandString(((OperationTask)task));
            Condition_within within=(Condition_within)((OperationTask)task).getParameter("within");
            if (within!=null) {
                command+=((StandardOperationParser)opparser).getCommandString_condition(within);
            }
            return command;
        } else if (task instanceof AddSequencesTask) {
            if (((AddSequencesTask)task).getFilename()!=null) return getAddSequencesFromFileCommandString((AddSequencesTask)task);
            else { // add sequences one by one
                StringBuilder builder=new StringBuilder();
                int size=((AddSequencesTask)task).getNumberofSequencesToAdd();
                for (int i=0;i<size;i++) {
                    Sequence seq=((AddSequencesTask)task).getSequence(i);
                    builder.append(seq.getName());
                    builder.append(" = new Sequence(");
                    builder.append(seq.getGeneName());
                    builder.append(",");
                    builder.append(seq.getGenomeBuild());
                    builder.append(",");
                    builder.append(seq.getChromosome());
                    builder.append(",");
                    builder.append(seq.getRegionStart());
                    builder.append(",");
                    builder.append(seq.getRegionEnd());
                    builder.append(",");
                    builder.append((seq.getTSS()!=null)?seq.getTSS():"");
                    builder.append(",");
                    builder.append((seq.getTES()!=null)?seq.getTES():"");
                    builder.append(",");
                    builder.append((seq.getStrandOrientation())==Sequence.DIRECT?"DIRECT":"REVERSE");
                    builder.append(")\n");
                }
                return builder.toString().trim();
            }
        } else return "SYSTEM ERROR: no protocol command for task";
    }
    
    public String getAddSequencesFromFileCommandString(AddSequencesTask task) {
        String targetName=engine.getDefaultSequenceCollectionName();
        Operation operation=engine.getOperation("new");      
        OperationTask newSequenceCollectionTask=new OperationTask("new "+SequenceCollection.getType());
        newSequenceCollectionTask.setParameter(OperationTask.OPERATION, operation);
        newSequenceCollectionTask.setParameter(OperationTask.OPERATION_NAME, operation.getName());
        newSequenceCollectionTask.setParameter(OperationTask.TARGET_NAME, targetName);
        newSequenceCollectionTask.setParameter(OperationTask.SOURCE_NAME, targetName);
        newSequenceCollectionTask.setParameter(Operation_new.DATA_TYPE, SequenceCollection.getType());    
        newSequenceCollectionTask.setParameter(Operation_new.PARAMETERS,Operation_new.FILE_PREFIX); // the FILE_PREFIX part of the parameter is necessary for proper recognition by Operation_new
        newSequenceCollectionTask.setParameter(Operation_new.FILENAME,task.getFilename());
        newSequenceCollectionTask.setParameter(Operation_new.DATA_FORMAT,task.getDataFormat());
        newSequenceCollectionTask.setParameter(Operation_new.DATA_FORMAT_SETTINGS,task.getDataFormatSettings());  
        return getCommandString(newSequenceCollectionTask);
    }

    /**
     * Performs preprocessing of a line by removing comments and excessive whitespace 
     * @param line The part of the text to preprocess
     * @return The prep
     */
    private String preprocess(String line) {
        line=line.trim();
        line=line.replaceAll("\\s+", " "); // collapse runs of whitespace into single spaces. (Is this a good idea?)
        if (line.startsWith("#")) return ""; // comment line. just treat it as a blank line
        return line;
    }
       
    /** Returns TRUE if this protocol contains any of the supplied terms */
    public boolean contains(Set<String> terms) {
        if (document==null) return false;
        Element root=document.getDefaultRootElement();
        int linecount=root.getElementCount();
        for (int i=0;i<=linecount-1;i++) {
           Element element=root.getElement(i);           
           String line;
           try {
              line=document.getText(element.getStartOffset(),element.getEndOffset()-element.getStartOffset());
           } catch(BadLocationException ble) {
               ble.printStackTrace(System.err);
               return false;
           }
           line=preprocess(line);
           if (line.isEmpty()) continue;
           for (String term:terms) {
               if (line.contains(term)) return true;
           }
        }      
        return false;
    }
    
    
    
    /**
     * Sets the status of the "dirty" flag for this Protocol.
     * Protocol scripts should be marked as dirty if the contents of the Protocol
     * has been changed compared to the last saved version.
     */ 
    public void setDirtyFlag(boolean dirty) {
        isDirtyFlag=dirty;
    }
    
    /**
     * Returns true if this Protocol is marked as "dirty"
     */ 
    public boolean isDirty() {
        return isDirtyFlag;
    }
    
    
    @Override
    public boolean isEmpty() {
        if (document.getLength()==0) return true; 
        String text="";
        try {
            text=document.getText(0, document.getLength());
        } catch (BadLocationException e) {System.err.println("SYSTEM ERROR: BadLocationException in method Protocol.isEmpty()");}
        return text.trim().isEmpty();
    }
    
    /**
     * Returns the filename of the file currently associated with this Protocol script
     * @return
     */
    public String getFileName() {
        return savedFileName;
    }
    
    /**
     * Sets a filename to be associated with this Protocol script.
     * The Protocol script should already have been saved to a file with the given name
     * @return
     */
    public void setFileName(String filename) {
         savedFileName=filename;
    }
            
    /** Exports a serialized version of this protocol, which can be loaded into a protocol through import() */ 
    public SerializedStandardProtocol getSerializedProtocol() {
        return new SerializedStandardProtocol(this);
    }
    
    
    @Override
    public void registerOperationParser(String operationName, OperationParser opparser) {
        operationParsers.put(operationName, opparser);
    }
    
    @Override
    public void registerDisplaySettingsParser(DisplaySettingsParser vsparser) {
        this.displaySettingsParser=vsparser;
    }
    
    @Override
    public void registerParametersParser(ParametersParser parametersParser) {
        this.parametersParser=parametersParser;
    }
    
    @Override
    public OperationParser getOperationParser(String operationName) {
        return operationParsers.get(operationName);
    }
    
    @Override
    public ParametersParser getParametersParser() {
        return parametersParser;
    }

    @Override
    public DisplaySettingsParser getDisplaySettingsParser() {
        return displaySettingsParser;
    }    
     
    private static boolean containsMacros(String line, ArrayList<String[]> macros) {
        if (macros==null || macros.isEmpty()) return false;
        for (int i=0;i<macros.size();i++) {
            String macro=macros.get(i)[0];
            if (line.toLowerCase().startsWith("$macro(") || line.toLowerCase().startsWith("!macro(")) {
                int pos=line.indexOf(")"); 
                if (pos<0) continue; // parenthesis has not been closed. This is probably just a temporary problem, but we should not go on at this point.
                line=line.substring(pos); // do not consider the macroname, only its definition. 
            }            
            if (line.contains(macro)) {
                return true; 
            }
        }       
        return false;
    }    
    
    private static boolean containsMacro(ArrayList<String> lines, String macro) {
        if (lines==null || lines.isEmpty()) return false;
        for (String line:lines) {
            if (line.toLowerCase().startsWith("$macro(") || line.toLowerCase().startsWith("!macro(")) {
                int pos=line.indexOf(")"); 
                if (pos<0) continue; // parenthesis has not been closed. This is probably just a temporary problem, but we should not go on at this point.                
                line=line.substring(pos); // do not consider the macroname, only its definition
            }
            if (line.contains(macro)) {
                return true; 
            }
        }       
        return false;
    }   
    
    private boolean isMacroDefined(String macroname) {
        if (macros==null || macroname==null) return false;
        for (String[] macro: macros) {
            if (macroname.equals(macro[0])) return true;
        }
        return false;
    }
    
    /**
     * This method will process a line and replace all macros found within
     * the line with the corresponding macro definition. The process will be repeated
     * (recursively) until no more macros are found (hence, circular definitions in macros should be avoided). 
     * If the line contains "list macros", these can result in multiple lines being returned
     * 
     * @param commandline
     * @return 
     * @throws ParseError 
     */
    private static ArrayList<String> processMacros(String commandline, ArrayList<String[]> macros) throws ParseError {
        ArrayList<String> result=new ArrayList<String>();
        result.add(commandline); // start with single line       
        boolean canContainMoreMacros=true;
        int loopcounter=0; // used to avoid infinite looping due to circular macros
        while (canContainMoreMacros) {
            canContainMoreMacros=false; // this can be changed later to signal that an additional pass through all the macros is required
            for (int i=0;i<macros.size();i++) { // for each macro
                String[] macro=macros.get(i);
                String macroname=macro[0];
                String definition=macro[1];
                if (containsMacro(result, macroname)) { // the set of lines contains this macro so it must be processed
                    ArrayList<String> newresult=new ArrayList<String>(result.size());
                    for (String line:result) {
                        newresult.addAll(expandMacro(line, macroname, definition));
                    }    
                    result=newresult;
                    canContainMoreMacros=true;
                }
            }
            loopcounter++;
            if (loopcounter>20) throw new ParseError("Macro expansion nests too deep. The defined macros could possibly contain circularities");
        }      
        return result;
    }    
    
//    private ArrayList<String> getMacroValue(String macrodefinition) {
//        TextVariable data=(TextVariable)engine.getDataItem(macrodefinition, TextVariable.class);
//        ArrayList<String> result=new ArrayList<String>();
//        if (data==null) {  // not a reference to a text variable, just a single value. but possibly multi-line?
//            if (macrodefinition.contains("|")) result.addAll(Arrays.asList(macrodefinition.split("\\|"))); // allow specification of multi-line macros
//            else result.add(macrodefinition);
//        } else {
//            result.addAll(data.getAllStrings());
//        }
//        return result;              
//    }
    
//    private String getMacroValue(String macrodefinition) {
//        TextVariable data=(TextVariable)engine.getDataItem(macrodefinition, TextVariable.class);
//        if (data==null) {  // not a reference to a text variable, just a single value. but possibly multi-line?
//            return macrodefinition;
//        } else {
//            return MotifLabEngine.splice(data.getAllStrings(), ","); // multi-line macro-values are a hassle. Just splice with comma
//        }            
//    }    
    
    /** Performs a single round of macro-expansion for the given macro. This can lead to multiple result lines */
    private static ArrayList<String> expandMacro(String line, String name, String definition) throws ParseError {
        ArrayList<String> result=new ArrayList<String>();
        if (line.contains(name)) {
            String preline="";
            String lowercaseLine=line.toLowerCase();
            if (lowercaseLine.startsWith("$macro(") || lowercaseLine.startsWith("!macro(")) { // skip the first part (macro name), but check if the macro definition contains a reference to this macro
                int pos=line.indexOf(")");
                if (pos<0) {result.add(line);return result;} // parenthesis has not been closed. We should not dare to proceed at this point
                preline=line.substring(0,pos);
                line=line.substring(pos); // the line is henceforth only the definition of the macro
            }
            if (definition.startsWith("[") && definition.endsWith("]")) {
                String[] list=expandListMacro(definition);
                for (int i=0;i<list.length;i++) { 
                    result.add(preline+line.replace(name, list[i]));
                }                   
            } else { // not a list-macro, just replace with verbatim definition or value of text variable
                if (definition.startsWith("\\")) definition=definition.substring(1); // remove the escape character from the macro definition
                // definition=getMacroValue(definition); // resolve text variable reference. ==> Using Text Variable as macro definition does not really make sense since macros are expanded before executing the protocol (and the value of the Text Variable might not be determined at this point)
                result.add(preline+line.replace(name, definition));
            }
        } else result.add(line); // no occurrences of the macro on this line
        return result;
    }

    /** 
     * If the given macro value is a valid "list macro", the method will return a list
     * of the values within. If not, a value of NULL will be returned
     * @param range The range can be defined as a comma-separated list of integer, double or String values
     *              or ranges of integer values specified as "start:end" or a combination of all of these
     *              if just a single number X is provided this will be interpreted as
     *              the range 1:X
     * @return 
     */
    private static String[] expandListMacro(String range) throws ParseError {
        if (range.startsWith("[")) range=range.substring(1);
        if (range.endsWith("]")) range=range.substring(0, range.length()-1);
        String[] parts=range.trim().split("\\s*,\\s*");
        if (parts.length==1 && !range.contains(":")) { // just a single value. This should then be an integer
             try  {
              int end=Integer.parseInt(range);   
              String[] values=new String[end];
              for (int i=0;i<end;i++) {
                  values[i]=""+(i+1);
              }
              return values;
            } catch (NumberFormatException e) {
                throw new ParseError("Macro error: Not a valid integer value in range: "+range);
            }           
        }
        ArrayList<String> values=new ArrayList<String>();
        for (String part:parts) {
            if (part.contains(":")) { // range within the list
                try  {
                  String[] pair=part.split(":");
                  if (pair.length!=2) throw new ParseError("Macro error: Not a valid range: "+part);
                  int start=Integer.parseInt(pair[0]);
                  int end=Integer.parseInt(pair[1]);                    
                  if (start==end) values.add(""+start);
                  else if (start<end) {
                      for (int i=start;i<=end;i++) values.add(""+i);
                  } else if (start>end) {
                      for (int i=start;i>=end;i--) values.add(""+i);
                  }                 
                } catch (NumberFormatException e) {
                    throw new ParseError("Macro error: Invalid integer value in range: "+part);
                }                               
            } else { // single value within the list, just add it
//                try  {
//                    values.add(Integer.parseInt(part));
//                } catch (NumberFormatException e) {
//                    throw new ParseError("Macro error: Not a valid integer value: "+part);
//                }      
                values.add(part);
            }
        }
        String[] list=new String[values.size()];
        for (int i=0;i<values.size();i++) {
            list[i]=values.get(i);
        }
        return list;
    }
    
    /**
     * Returns a copy of this protocol but with all current macros expanded.
     * This method is mainly used by the "expand macros" preview functionality
     * in the GUI's protocol editor. It is not used directly by any of the parsing methods
     * (these expand macros line by line using the processMacros() method).
     * @return 
     */
    public StandardProtocol expandAllMacros() throws ParseError {
        ArrayList<String> newtext=new ArrayList<String>();
        Element root=document.getDefaultRootElement();
        int linecount=root.getElementCount();
        int start=0;
        int end=linecount-1;
        macros=engine.getMacros(); // returns a (copy of) the currently defined macros    
        
        for (int i=start;i<=end;i++) { // go through protocol line by line, add to/replace macro definitions if defined in the protocol and expand macros if found inside lines
           Element element=root.getElement(i);           
           String line;
           try {
              line=document.getText(element.getStartOffset(),element.getEndOffset()-element.getStartOffset());
           } catch(BadLocationException ble) {throw new ParseError(ble.getMessage());}
           if (line.isEmpty() || line.startsWith("#")) {
               newtext.add(line);
               continue;
           }      
           if (line.startsWith("@") || line.startsWith("$") || line.startsWith("!")) {
                 DisplaySettingTask task=getDisplaySettingsParser().parse(line);
                 if (task.getSettingName().equalsIgnoreCase("macro")) { // command defines a macro, add it to the current set (internal)
                     String macroname=task.getTargets();
                     String macrodefinition=(String)task.getValue();
                     boolean force=task.shouldForce();
                     if (force || !isMacroDefined(macroname)) {
                         replaceCurrentMacro(macroname,macrodefinition); // this adds the macro to the protocol's internal set used for pre-processing, but the macro will not be added to the "global" macro set unless this protocol command is actually executed
                     }
                 }
           }
           if (containsMacros(line, macros)) {
                ArrayList<String> lines=processMacros(line, macros);
                newtext.addAll(lines);
           } else { // no macros on this line
                newtext.add(line);        
           }
        } // end: for each protocol line
        return new StandardProtocol(engine, newtext);     
    }
    
    
    /**
     * Expands macros in a body of text which is not a regular protocol
     * @param usemacros The macro definitions to use 
     * @return 
     */    
    public static ArrayList<String> expandAllMacros(ArrayList<String> input, ArrayList<String[]> usemacros)  throws ParseError {
        ArrayList<String> newtext=new ArrayList<String>();
        for (String line:input) {
           if (containsMacros(line, usemacros)) {
                ArrayList<String> lines=processMacros(line, usemacros);
                newtext.addAll(lines);
           } else { // no macros on this line. Just add it as is
                newtext.add(line);        
           }
        } // 
        return newtext;     
    }            
    
    
    
    
    private void replaceCurrentMacro(String macroname, String macrodefinition) {
         if (macros==null) macros=new ArrayList<String[]>();
         if (isMacroDefined(macroname)) {
             for (int i=0;i<macros.size();i++) {
                 String[] macro=macros.get(i);
                 if (macro[0].equals(macroname)) {macro[1]=macrodefinition;break;}
             }
         } else macros.add(new String[]{macroname,macrodefinition});
      
    }
    
    @Override
    public List<String[]> getMacros() {
        if (macros==null) return new ArrayList<String[]>();
        else return macros;
    }
}
