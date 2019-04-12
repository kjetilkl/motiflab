/*
 
 
 */

package motiflab.external;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterExporter;
import motiflab.engine.MotifLabEngine;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import motiflab.engine.ParameterCondition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.SystemError;
import motiflab.engine.data.*;
import motiflab.engine.dataformat.DataFormat;

/**
 * This superclass is the top-level class for wrapping external programs such
 * as motif discovery or motif scanning tools
 * @author kjetikl
 */
public class ExternalProgram implements ParameterExporter {
    public static final String SOURCES="SOURCES";
    
    protected ArrayList<Parameter> regularParameterFormats=new ArrayList<Parameter>(); 
    protected ArrayList<Parameter> sourceParameterFormats=new ArrayList<Parameter>(); 
    protected ArrayList<Parameter> resultsParameterFormats=new ArrayList<Parameter>(); 
    protected MotifLabEngine engine;
    protected String name;     
    protected String programclass;
    protected String serviceType;
    protected String location;
    protected String[] commands=null;
    private HashMap<String,Object> properties=new HashMap<String,Object>();
    private ArrayList<String> allArguments=new ArrayList<String>();
    private HashMap<String,String> argumentSwitches=new HashMap<String,String>();
    private HashMap<String,String> argumentSwitchesSeparator=new HashMap<String,String>();
    private HashMap<String,String> argumentTypes=new HashMap<String,String>();
    private HashMap<String,String> dataFormatNames=new HashMap<String,String>();
    private HashMap<String,ParameterSettings> dataFormatSettings=new HashMap<String,ParameterSettings>();
    private ArrayList<String[]> executableSources=null; // each source is described by an array of five elements {OS, version, URL, compression-type and relative path of target executable within ZIP (when applicable)}
    private ArrayList<String> requirements=null; // These are just information strings that will be displayed to the user during installation
    private HashMap<String,String> implicitArguments=new HashMap<String,String>(); // contains 'values' of implicit arguments
    private HashMap<String,String> explicitArguments=new HashMap<String,String>(); // contains 'values' of explicit arguments
    private HashMap<String,String> redirectedArguments=new HashMap<String,String>(); // contains STDIN or STDOUT flags for arguments with redirection
    private ArrayList<String[]> referenceLinks=new ArrayList<String[]>(); // contains information about crossreferences that must be resolved
    private HashMap<String,HashMap<String,String>> optionValueMap=null; // keeps information about String parameters that have different 'display values' and 'actual values'
    private HashSet<String> temporaryfilenames=new HashSet<String>(); // keeps track of files that are explicitly registered as being temporary (in the XML-config)
    private HashSet<String> softlinks=new HashSet<String>(); // keeps tracks of which parameters are "soft links". Soft links refers to the same data object as another parameter but does not use the same file (for complex types)
    private HashSet<String> skipIfDefault=new HashSet<String>(); // keeps tracks of which parameters are "soft links". Soft links refers to the same data object as another parameter but does not use the same file (for complex types)
    private String configurationfile=null;
    private boolean cygwin=false; // set true if the program runs in Windows under Cygwin
    private String commandseparator=null; // character or string used to split up command lines (usually semicolon)
    private String quotepath="auto";
    private boolean appliesToSequences=true; // set to true if this program applies to sequence collections (and possibly subsets)
        
    private ArrayList<Report> reportExpressions=null; // a list of <Pattern,short,String> tuples that describe expressions which should be monitored in the programs STDOUT/STDERR when run
    
    public String getName() {
        return name;
    }
   
    public String getDescription() {
        return (String)properties.get("description");
    }
    
    public String getProgramClass() {
        return programclass;
    }
    
    /** Returns a value for the given property (not parameter)
     *  or NULL if no such property exists. Common properties for ExternalPrograms
     *  are e.g. 'author','citation' ...
     *  @return the property value should be either a single String or an ArrayList<String>
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    /** Sets a value for the given property (not parameter)
     *  Common properties for ExternalPrograms are e.g. 'author','citation' ...
     *  the property value should be either a single String or an ArrayList<String>
     *  if a property with the given name already exists the new value will be added
     *  to the existing list unless overwrite is set to TRUE
     */
    @SuppressWarnings("unchecked")
    public void setProperty(String key, Object value, boolean overwrite) {
        if (overwrite) properties.put(key,value);
        else {
            Object current=getProperty(key);
            if (current==null) {
                properties.put(key,value);
            } else if (current instanceof ArrayList) {
                ((ArrayList)current).add(value);
            } else { // the property is currently a single value, replace with a list containing both the old and new values
                ArrayList list=new ArrayList();
                list.add(current);
                list.add(value);
                properties.put(key,list);
            }
        }
    }
 
    /** Removes all registered properties for this program */
    public void clearProperties() {
        properties.clear();
    }
    
    /** Returns true if the program was made for Unix/Linux and must run in Cygwin to work on Windows*/
    public boolean isRunInCygwin() {
        return cygwin;
    }
    
    /** Specifies if the program must be run in Cygwin in order to work on Windows */
    public void setRunInCygwin(boolean runInCygwin) {
        cygwin=runInCygwin;
    }
    
    
    
    /** Returns the type of service this ExternalProgram represents
     *  e.g. "bundled" for (Java) programs that are included with MotifLab 
     *  or   "plugin" for (Java) programs that are installed as MotifLab plugins
     *  or   "local" for locally installed executable programs 
     *  or   "soap" for web services accessed via SOAP protocol  
     */
    public String getServiceType() {
        return serviceType;
    }
    
    /** Returns the location of this ExternalProgram
     *  Depending on the service type this could be the full pathname of a file
     *  on the local filesystem, a JAVA class (for bundled software and plugins) or an internet address
     */
    public String getLocation() {
        return location;
    }
    
    /** Sets the location of this external program
     *  Depending on the service type this could be the full pathname of a file
     *  on the local filesystem, a JAVA class (for bundled software and plugins) or an internet address     
     */
    public void setLocation(String location) {
        this.location=location;
    }
    
    /** Returns the command(s) to use in order to execute this program
     */
    public String[] getCommands() {
        if (commands==null) return null; 
        else return commands.clone(); // these can be changed later by argument replacements so I return a clone
    }
    
    /** Sets a reference to the configuration file for this program */
    public void setConfigurationFile(String filename) {
        configurationfile=filename;
    }
    
    /** Returns a reference to the configuration file for this program */
    public String getConfigurationFile() {
        return configurationfile;
    }
    
    
    /**
     * Returns the directory associated with program (as long as the program is installed locally)
     * If getServiceType() is not "local" the method will return an empty string;
     * @return
     */
    public String getApplicationDirectory() {
        if (!getServiceType().equals("local")) return "";
        File appfile=new File(getLocation());        
        String dir=appfile.getParent();
        return (dir!=null)?dir:"";
    }
    
    /** Returns a list of String arrays describing available sources
     *  from which the executable program can be downloaded. 
     *  Each String[] contains 
     *      [0] the Operating system 
     *      [1] program version
     *      [2] an URL (as String)
     *      [3] A String specifying the compression format (e.g. "ZIP") or null if the file was not compressed
     *      [4] An (optional) String specifying the relative path to the target executable file if this is located somewhere inside a ZIP-file
     */
    public ArrayList<String[]> getExecutableSources() {
        return executableSources;
    }
    
    /**
     * This will return a list of requirements listed for the program
     * (or null if no requirements are specified). A requirement could 
     * for instance be that the program relies on Java 1.7 or higher being
     * available on the system or that this particular configuration was
     * meant to be used with version 4.2 of the program.
     * A special requirement is in the form "MotifLab version X" where X 
     * is a version number (e.g. "1.1"). This means that the configuration 
     * relies on functionality that only exists in this version of MotifLab 
     * (or more recent versions).
     * @return 
     */
    public ArrayList<String> getRequirements() {
        return requirements;
    }  
    /**
     * This will return a list of requirements listed for the program
     * except any requirements pertaining to MotifLab version
     * @return 
     */
    public ArrayList<String> getRequirementsBesidesMotifLabVersion() {
        if (requirements==null) return null;
        ArrayList<String> other=new ArrayList<String>(requirements.size());
        for (String req:requirements) {
            if (!req.toLowerCase().startsWith("motiflab version")) other.add(req);
        }
        if (other.isEmpty()) return null; else return other;
    }      
    
    public boolean MotifLabVersionOK() {
        String reqVersion=getRequiredMotifLabVersion();
        if (reqVersion==null) return true;
        int res=MotifLabEngine.compareVersions(reqVersion);
        return (res>=0); // Returns TRUE if this version of MotifLab OK
    }  
    
    /**
     * Returns a MotifLab version string if this program requires a certain
     * version of MotifLab or null if no such requirements are made.
     * @return 
     */
    public String getRequiredMotifLabVersion() {
        if (requirements==null) return null;
        for (String req:requirements) {
            if (req.toLowerCase().startsWith("motiflab version")) {
                return req.substring("MotifLab version".length()).trim();                
            }
        }  
        return null;
    }
    
    /**
     * This method should return TRUE if the program can be applied to subsets of the
     * current sequences. If this method returns TRUE, the execute operation dialog shown 
     * when setting the running parameters for this program will contain a drop-down menu
     * allowing the user to select the sequences to apply the program to. 
     * If this method returns FALSE, this SequenceCollection menu will not be shown.
     */    
    public boolean isApplicableToSequenceCollections() {
        return appliesToSequences;
    }
    
    /**
     * This method sets the "applicableToSequenceCollections" flag of this external program.
     * If this flag is set to TRUE, the execute operation dialog shown 
     * when setting the running parameters for this program will contain a drop-down menu
     * allowing the user to select the sequences to apply the program to. 
     * If the flag is FALSE, this SequenceCollection menu will not be shown.
     */   
    public void setApplicableToSequenceCollections(boolean applicable) {
        appliesToSequences=applicable;
    }    
    
    /** Returns true if the parameter with the given name represents a 'source' parameter */
    public boolean isSourceParameter(String parameterName) {
        for (Parameter param:sourceParameterFormats) {
            if (param.getName().equals(parameterName)) return true;
        }
        return false;
    }
    /** Returns true if the parameter with the given name represents a 'results' parameter */
    public boolean isResultsParameter(String parameterName) {
        for (Parameter param:resultsParameterFormats) {
            if (param.getName().equals(parameterName)) return true;
        }
        return false;        
    }

    /** Returns the number of data objects produced by this ExternalProgram */
    public int getNumberOfResultParameters() {
        return resultsParameterFormats.size();
    }

    /** Returns the class type for the result parameter with the given index number (starting at 0) */
    public Class getTypeForResultParameter(int resultParameterNumber) {
        if (resultParameterNumber<0 || resultParameterNumber>=resultsParameterFormats.size()) return null;
        return resultsParameterFormats.get(resultParameterNumber).getType();
    }

   /** Returns the name of the result parameter with the given index number (starting at 0) */
    public String getNameForResultParameter(int resultParameterNumber) {
        if (resultParameterNumber<0 || resultParameterNumber>=resultsParameterFormats.size()) return null;
        return resultsParameterFormats.get(resultParameterNumber).getName();
    }
    
    /**
     * Returns a 2D table with a [String,Class] entry for each result parameter
     * where the element [i][0] is the name of result parameter i and [i][1] is 
     * the corresponding type for that result parameter.
     * @return 
     */
    public Object[][] getNamesAndTypesForAllResultParameters() {
        Object[][] result=new Object[resultsParameterFormats.size()][2];
        for (int i=0;i<resultsParameterFormats.size();i++) {
            result[i][0]=resultsParameterFormats.get(i).getName();
            result[i][1]=resultsParameterFormats.get(i).getType();            
        }
        return result;
    }
    
    /**
     * Adds a regular parameter to the External Program. This is used for initialization of External Program objects and should only be called in a constructor or similar setup method
     */
    protected void addParameter(String parameterName, Class type, Object defaultValue, Object[] allowedValues, String description, boolean required, boolean hidden, boolean advanced, ArrayList<ParameterCondition> conditions) {
        regularParameterFormats.add(new Parameter(parameterName,type,defaultValue,allowedValues,description,required,hidden,advanced,conditions));
    }    
    protected void addParameter(String parameterName, Class type, Object defaultValue, Object[] allowedValues, String description, boolean required, boolean hidden, boolean advanced) {
        regularParameterFormats.add(new Parameter(parameterName,type,defaultValue,allowedValues,description,required,hidden,advanced));
    }
    protected void addParameter(String parameterName, Class type, Object defaultValue, Object[] allowedValues, String description, boolean required, boolean hidden) {
        regularParameterFormats.add(new Parameter(parameterName,type,defaultValue,allowedValues,description,required,hidden));
    }
    /**
     * Adds a source parameter to the External Program. This is used for initialization of External Program objects and should only be called in a constructor or similar setup method
     */
    protected void addSourceParameter(String parameterName, Class type, Object defaultValue, Object[] allowedValues, String description) {
        sourceParameterFormats.add(new Parameter(parameterName,type,defaultValue,allowedValues,description));
    }
    /**
     * Adds a parameter to the External Program. This is used for initialization of External Program objects and should only be called in a constructor or similar setup method
     */
    protected void addResultParameter(String parameterName, Class type, Object defaultValue, Object[] allowedValues, String description) {
        resultsParameterFormats.add(new Parameter(parameterName,type,defaultValue,allowedValues,description));
    }
        
    @Override
    public String toString() {return getName();}

    @Override
    public Object getDefaultValueForParameter(String parameterName) {
        for (Parameter par:regularParameterFormats) {
            if (parameterName.equals(par.getName())) return par.getDefaultValue();
        }
        return null;
    }

    @Override
    public Parameter[] getParameters() {
        Parameter[] list=new Parameter[regularParameterFormats.size()];
        return regularParameterFormats.toArray(list);
    }
    
    @Override
    public Parameter getParameterWithName(String parameterName) {
        for (Parameter parameter:regularParameterFormats) {
            if (parameter.getName().equals(parameterName)) return parameter;
        }
        return null;
    }
    

    private Parameter getParameterWithName(String parameterName, ArrayList<Parameter> list) {
        for (Parameter parameter:list) {
            if (parameter.getName().equals(parameterName)) return parameter;
        }
        return null;
    }
    
    /** Returns the name of the first regular parameter encountered which is of the given type
     *  or null if no such parameter exists
     */
    public String getNameOfFirstRegularParameterForType(Class type) {
        for (Parameter parameter:regularParameterFormats) {
            if (parameter.getType().equals(type)) return parameter.getName();
        }
        return null;        
    }
    
    /**
     * Returns a list of all arguments (names) for this program in the order in which they 
     * shall be listed as arguments for command line execution.
     * This list will typically be superset of the exported parameters, i.e. this list will
     * include the names of all exported parameters and in addition the names of any source
     * and target arguments.
     * @return
     */
    public ArrayList<String> getAllArguments() {
      return allArguments;   
    }
    
    /** Returns the type designation for this argument (e.g. "flag|valued option|implicit|explicit|STDIN|STDOUT")
     */
    public String getArgumentType(String argumentName) {
        return argumentTypes.get(argumentName);
    }
    
    private void setArgumentType(String argumentName, String type) {
        argumentTypes.put(argumentName,type);
    }
    
    /** Returns the (system dependent) switch for the argument with the given name
     * for instance: for a file argument that is specified on the command line as
     * "-f <file>" the method will return "-f"
     */
    public String getArgumentSwitch(String argumentName) {
        return argumentSwitches.get(argumentName);
    }
    
    private void setArgumentSwitch(String argumentName, String switchString) {
        argumentSwitches.put(argumentName,switchString);
    }

    /** Returns the character which separates a switch from the argument value
     *  This is usually the space character (as in "-f filename") but can also
     *  be a different character (e.g. "-f=filename" or "-f:filename").
     *  If no explicit separator has been set for the switch the method will return the space character.
     */
    public String getArgumentSwitchSeparator(String argumentName) {
        if (argumentSwitchesSeparator.containsKey(argumentName)) return argumentSwitchesSeparator.get(argumentName);
        else return " ";
    }

    private void setArgumentSwitchSeparator(String argumentName, String switchString) {
        argumentSwitchesSeparator.put(argumentName,switchString);
    }
    
    /** Returns a value (usually a filename) to use for an implicit argument
     *  Implicit arguments are settings that are usually hardcoded in the external program
     *  and can not (and should not) be specified at the command line.
     *  An example can be a program that always outputs results to a file called "results.txt"
     *  Then the name of the resultsfile should not be specified as a regular argument but as an implicit argument with filename "results.txt"
     */
    public String getImplicitArgument(String argumentName) {
        return implicitArguments.get(argumentName);
    }
    
    private void setImplicitArgument(String argumentName, String filename) {
        implicitArguments.put(argumentName,filename);
    }

    /** Returns TRUE if the given argument is 'implicit'.
     *  Implicit arguments are settings that are usually hardcoded in the external program
     *  and can not (and should not) be specified at the command line.
     *  An example can be a program that always outputs results to a file called "results.txt"
     *  Then the name of the resultsfile should not be specified as a regular argument but as an implicit argument with filename "results.txt"
     */
    public boolean isImplicitArgument(String argumentName) {
        return implicitArguments.containsKey(argumentName);
    }
    
    /** Returns a value (usually a filename) to use for an explicit argument
     */
    public String getExplicitArgument(String argumentName) {
        return explicitArguments.get(argumentName);
    }
    
    private void setExplicitArgument(String argumentName, String filename) {
        explicitArguments.put(argumentName,filename);
    }

    /** Returns TRUE if the given argument is 'explicit'.
      */
    public boolean isExplicitArgument(String argumentName) {
        return explicitArguments.containsKey(argumentName);
    }    
    

    /** Returns either STDIN or STDOUT if the argument represents
     *  data which should be redirected to or from the program
     *  or NULL if the argument does not require redirection
     */
    public String getRedirectionForArgument(String argumentName) {
        return redirectedArguments.get(argumentName);
    }

    /** Sets a redirection destination for the argument
     *  @param argumentName
     *  @param destination (should be either STDIN or STDOUT)
     */
    public void setRedirectionForArgument(String argumentName, String destination) {
        redirectedArguments.put(argumentName,destination);
    }

    /** Returns TRUE if this argument is (directly) associated with 
     *  a redirection to/from STDIN or STDOUT
     */
    public boolean isRedirectionArgument(String argumentName) {
        return redirectedArguments.containsKey(argumentName);
    }

    /** Returns TRUE if this argument is directly or indirectly (linked)
     *  associated with a redirection to/from STDIN or STDOUT
     */
    public boolean isLinkedToRedirectionArgument(String argumentName) {
        if (redirectedArguments.containsKey(argumentName)) return true;
        String linked=getParameterReferenceLink(argumentName, null);
        if (linked!=null) {
             return redirectedArguments.containsKey(linked);
        } else return false;
    }

    /** Returns TRUE if the external program requires any form of redirection
     *  of data from STDIN or STDOUT
     */
    public boolean programRequiresRedirection() {
        return !redirectedArguments.isEmpty();
    }
    
    /** Returns the Data Format required by the argument with the given name
     * If no explicit Data Format is specified for the argument or if the format
     * is unknown, a NULL value is returned
     */
    public DataFormat getArgumentDataFormat(String argumentName) {
        String dataFormatName=dataFormatNames.get(argumentName);
        if (dataFormatName==null) return null;
        return engine.getDataFormat(dataFormatName);
    }

    private void setArgumentDataFormat(String argumentName, String dataformatName) {
        dataFormatNames.put(argumentName, dataformatName);
    }    
    
    /** Returns the Data Format Parameter settings required by the argument with the given name
     * If no explicit Parameter settings is specified for the argument, a NULL value is returned
     */
    public ParameterSettings getArgumentDataFormatSettings(String argumentName) {
        return dataFormatSettings.get(argumentName);
    }

    private void setArgumentDataFormatSettings(String argumentName, ParameterSettings settings) {
        dataFormatSettings.put(argumentName,settings);
    }
    
    /** Returns TRUE if the parameter with the given name represents a "soft link"
     *  Soft links are parameters that refers to other parameters but do not use 
     *  the same temp-files (for complex data)     
     */
    public boolean isSoftLink(String argumentName) {
        return softlinks.contains(argumentName);
    }    
    /** Returns TRUE if the parameter with the given should not be output on
     *  the command line if it has its default value
     */
    public boolean shouldSkipIfDefault(String argumentName) {
        return skipIfDefault.contains(argumentName);
    }
               
    
    public void setEngine(MotifLabEngine engine) {
        this.engine=engine;
    }    
    
    /**
     * Saves the configuration of this external program in XML format to the given file
     */
    public void saveConfigurationToFile(File configurationfile) throws Exception {
        Document document=getXMLrepresentation();
        TransformerFactory factory=TransformerFactory.newInstance();
        factory.setAttribute("indent-number", new Integer(3));
        Transformer transformer=factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source=new DOMSource(document); 
        StreamResult result=new StreamResult(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(configurationfile)),"UTF-8"));
        transformer.transform(source, result);
    }

    /** Returns an XML representation of the configuration of the ExternalProgram 
     *  that could be used when saving the configuration to disc. 
     *  Note that not all properties will necessarily be included in this XML document
     *  such as e.g. "requirements" and "sources" (where the user can obtain the executable)
     *  since these are only required at installation time and not thereafter.
     */
    public Document getXMLrepresentation() throws ParserConfigurationException {
         DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         Document document = builder.newDocument();
         Element program=document.createElement("program");
         program.setAttribute("name", getName());
         program.setAttribute("class", getProgramClass());
         if (isRunInCygwin()) program.setAttribute("cygwin", "yes");
         Element propertiesElement=document.createElement("properties");
         for (String key:properties.keySet()) {
             Object propertyvalue=properties.get(key);
             if (propertyvalue instanceof ArrayList) {
                 for (Object val:(ArrayList)propertyvalue) {
                     Element propertyElement = document.createElement(key);
                     propertyElement.setTextContent(val.toString());
                     propertiesElement.appendChild(propertyElement);  
                 }
             } else if (propertyvalue!=null) {
                 Element propertyElement = document.createElement(key);
                 propertyElement.setTextContent(propertyvalue.toString());
                 propertiesElement.appendChild(propertyElement);                 
             }
         }
         program.appendChild(propertiesElement); 

         Element serviceElement=document.createElement("service");
         serviceElement.setAttribute("type", serviceType);
         serviceElement.setAttribute("location", location);
         program.appendChild(serviceElement);   
         
         String[] commandlines=getCommands();
         if (commandlines!=null && commandlines.length>0) {
             if (commandlines.length==1) {
                   Element commandElement=document.createElement("command");
                   if (commandseparator!=null) commandElement.setAttribute("separator", commandseparator);
                   if (!quotepath.equalsIgnoreCase("auto")) commandElement.setAttribute("quotepath", quotepath);
                   commandElement.setTextContent(commandlines[0]);
                   program.appendChild(commandElement);
             } else {
                Element commandsElement = document.createElement("commands");
                for (String commandline:commandlines) {
                   Element commandElement=document.createElement("command");
                   commandElement.setTextContent(commandline);
                   commandsElement.appendChild(commandElement);
                }
                program.appendChild(commandsElement);
             }
         }
         
         for (String parameterName:allArguments) {
             Element parameterElement=document.createElement("parameter");
             String parameterType=null;
             Parameter parameter;
             if (isSourceParameter(parameterName)) {
                 parameterType="source";
                 parameter=getParameterWithName(parameterName,sourceParameterFormats); 
             } else if (isResultsParameter(parameterName)) {
                 parameterType="result";
                 parameter=getParameterWithName(parameterName,resultsParameterFormats); 
             } else {
                parameterType="regular";
                parameter=getParameterWithName(parameterName,regularParameterFormats);      
             }
             parameterElement.setAttribute("name", parameterName);
             String className=parameter.getType().getSimpleName();
             if (isMapThatAllowsNumericVariableAlso(parameter)) className+="+"; // Add '+' suffix to signal that numeric scalars can be used instead of maps
             parameterElement.setAttribute("class", className);
             parameterElement.setAttribute("type", parameterType);
             parameterElement.setAttribute("required",(parameter.isRequired())?"yes":"no");  
             if (parameter.isHidden()) parameterElement.setAttribute("hidden","yes");
             if (parameter.isAdvanced()) parameterElement.setAttribute("advanced","yes");
             if (shouldSkipIfDefault(parameterName)) parameterElement.setAttribute("skipIfDefault","yes");
             String paramlink=getParameterReferenceLink(parameterName,null);
             if (paramlink!=null) {
                 if (isSoftLink(parameterName)) parameterElement.setAttribute("softlink", paramlink);
                 else parameterElement.setAttribute("link", paramlink);
             } 
             ArrayList<ParameterCondition> conditions=parameter.getConditions();
             if (conditions!=null) {
                 for (ParameterCondition condition:conditions) {
                     Element conditionElement=document.createElement("condition");
                     if (!condition.isMonitoringSelf()) {
                        conditionElement.setAttribute("monitor", condition.getMonitoredParameter()); 
                     }                 
                     boolean negation=condition.isConditionNegated();
                     if (negation) conditionElement.setAttribute("ifNot", condition.getIfClause());
                     else conditionElement.setAttribute("if", condition.getIfClause());
                     conditionElement.setAttribute("then", condition.getThenClause());
                     String elseClause=condition.getElseClause();                     
                     if (elseClause!=null && !elseClause.isEmpty()) conditionElement.setAttribute("else", elseClause);
                     parameterElement.appendChild(conditionElement); 
                 }                
             }

             if (parameter.getType()==Integer.class || parameter.getType()==Double.class) {
                  Object values = parameter.getAllowedValues();
                  if (values instanceof Object[]) {
                         Element minElement=document.createElement("min");
                         Element maxElement=document.createElement("max");
                         minElement.setTextContent(((Object[])values)[0].toString());
                         maxElement.setTextContent(((Object[])values)[1].toString());
                         parameterElement.appendChild(minElement);                       
                         parameterElement.appendChild(maxElement);                       
                  }
             } else if (parameter.getType()==String.class) {
                  Object values = parameter.getAllowedValues();
                  if (values instanceof Object[]) {
                      for (Object value:(Object[])values) {
                         String valueAsString=value.toString();
                         Element optionElement=document.createElement("option");
                         if (optionValueMap!=null && optionValueMap.containsKey(parameterName)) {
                             HashMap<String,String>map=optionValueMap.get(parameterName);                             
                             String usevalue=map.get(valueAsString);
                             if (usevalue!=null) optionElement.setAttribute("value", usevalue);                             
                         }
                         optionElement.setTextContent(valueAsString);
                         parameterElement.appendChild(optionElement);                            
                      }
                  } else if (values!=null) {
                     Element optionElement=document.createElement("option");
                     optionElement.setTextContent(values.toString());
                     parameterElement.appendChild(optionElement);                      
                  }
             }
             Object def = parameter.getDefaultValue();
             if (def!=null) {
                 Element defaultValueElement=document.createElement("default");
                 defaultValueElement.setTextContent(def.toString());
                 parameterElement.appendChild(defaultValueElement);
             }  
             String descriptionString = parameter.getDescription();
             if (descriptionString!=null) {
                 Element descriptionElement=document.createElement("description");
                 descriptionElement.setTextContent(descriptionString);
                 parameterElement.appendChild(descriptionElement);
             }            
             DataFormat dataformat=getArgumentDataFormat(parameterName);
             if (dataformat!=null) {
                 Element dataformatElement=document.createElement("dataformat");
                 dataformatElement.setAttribute("name", dataformat.getName());
                 ParameterSettings dataformatsettings=getArgumentDataFormatSettings(parameterName);
                 if (dataformatsettings!=null) {
                     String[] settings=dataformatsettings.getParameters();
                     for (String setting:settings) {
                         Element settingsElement=document.createElement("setting");
                         settingsElement.setAttribute("name", setting);
                         settingsElement.setAttribute("class", dataformatsettings.getTypeForParameter(setting, dataformat.getParameters()).getSimpleName());
                         String link=getParameterReferenceLink(parameterName,setting);
                         if (link!=null) settingsElement.setAttribute("link", link); 
                         else {
                             Object defaultValue=dataformatsettings.getParameterAsString(setting, dataformat.getParameters());
                             if (defaultValue!=null) settingsElement.setTextContent(defaultValue.toString());
                         }
                         dataformatElement.appendChild(settingsElement);                         
                     }                     
                 }
                 parameterElement.appendChild(dataformatElement);
             }           
             Element argumentElement=document.createElement("argument");
             String argumentType=getArgumentType(parameterName);
             argumentElement.setAttribute("type", argumentType);
             String implicitfilename=getImplicitArgument(parameterName);
             if (implicitfilename!=null && !implicitfilename.isEmpty()) argumentElement.setAttribute("filename", implicitfilename);
             String explicitfilename=getExplicitArgument(parameterName);
             if (explicitfilename!=null && !explicitfilename.isEmpty()) argumentElement.setAttribute("filename", explicitfilename);
             String argumentSwitch=getArgumentSwitch(parameterName);
             if (argumentSwitch!=null) argumentElement.setAttribute("switch", argumentSwitch);
             String argumentSwitchSeparator=getArgumentSwitchSeparator(parameterName);
             if (argumentSwitchSeparator!=null && !argumentSwitchSeparator.equals(" ")) argumentElement.setAttribute("switchseparator", argumentSwitchSeparator);
             parameterElement.appendChild(argumentElement);
             program.appendChild(parameterElement); 
         }        
         for (String temp:temporaryfilenames) {
             Element tempdirElement=document.createElement("temporary");
             tempdirElement.setAttribute("filename", temp);
             program.appendChild(tempdirElement); 
         }
         if (reportExpressions!=null) {
             Element reports=document.createElement("reports");
             for (Report report:reportExpressions) {
                 Element reportElement=document.createElement("report");
                 reportElement.setAttribute("expression", report.getExpressionAsString());
                 reportElement.setAttribute("target", report.getTargetAsString());
                 String output=report.getOutputAsString();
                 if (output!=null && !output.isEmpty()) reportElement.setAttribute("output", output);
                 reports.appendChild(reportElement); 
             }
             program.appendChild(reports); 
         }
         
         document.appendChild(program);
         return document;
    }

    /** this little hack returns true if the given parameter is a NumericMap parameter which
     *  according to its 'allowedValues' also accept the use of NumericVariables 
     *  (i.e. scalars can be used instead of the map)
     */
    private boolean isMapThatAllowsNumericVariableAlso(Parameter parameter) {
        if (!NumericMap.class.isAssignableFrom(parameter.getType())) return false;
        Object allowedValues=parameter.getAllowedValues();
        if (allowedValues instanceof Class[]) {
            for (Class c:(Class[])allowedValues) {
               if (c.equals(NumericVariable.class)) return true;
            }
            return false;
        } else return false;
    }
    
    /** Initializes an ExternalProgram based on an XML-configuration file read from a file
     * @param configurationfile The XML-formatted configuration file of the program
     * @param keepAllProperties If this is TRUE all non-essential properties of the program (such as program description, license text, author contact information and citation information)
     *        is retained in the ExternalProgram object (possibly hogging memory). If the flag is false, only essential info is kept
     */    
    public static ExternalProgram initializeExternalProgramFromFile(File configurationfile, boolean keepAllProperties) throws SystemError {
        try {
            BufferedInputStream inputstream=new BufferedInputStream(MotifLabEngine.getInputStreamForFile(configurationfile));
            return initializeExternalProgramFromStream(inputstream, configurationfile.getName(),keepAllProperties); 
        }
        catch (FileNotFoundException fnf) {throw new SystemError("File not found error: "+configurationfile.getAbsolutePath());}        
        catch (IOException ioe) {throw new SystemError(ioe.getMessage());}        
    }    
    
    /** Initializes an ExternalProgram based on an XML-configuration file read from a stream
     * 
     *  Note: this method is used to initialize (on startup) "installed programs" whose XML-representations 
     *  are stored "internally" in the local MotifLab work-directory. However, it is also used to parse
     *  new config-files that are in process of being installed. These new XML-files are first parsed by
     *  this method, then a local XML-"copy" is stored by generating a new XML-representation with the
     *  getXMLrepresentation() method. The stored copy is subsequently used (and parsed by this method)
     *  whenever MotifLab starts up.
     *  @param inputstream The InputStream from which to read the XML-formatted configuration file of the program
     *  @param sourceName the name (e.g. a filename) that describes the original source for the inputstream. This is used when reporting errors
     *  @param keepAllProperties If this is TRUE all non-essential properties of the program (such as program description, license text, author contact information and citation information)
     *         is retained in the ExternalProgram object (possibly hogging memory). If the flag is false, only essential info is kept
     * 
     */  
    public static ExternalProgram initializeExternalProgramFromStream(InputStream inputstream, String sourcename, boolean keepAllProperties) throws SystemError {
        ExternalProgram externalProgram=null;
        try {
         DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         Document doc = builder.parse(inputstream);
         NodeList programnodes = doc.getElementsByTagName("program");
         if (programnodes.getLength()==0) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nNo 'program' node found");
         Element programNode = (Element) programnodes.item(0);
         String programName=programNode.getAttribute("name");
         if (programName==null || programName.isEmpty()) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nMissing 'name' attribute for program");
         String programCygwin=programNode.getAttribute("cygwin");
         String programclass=programNode.getAttribute("class");                 
         if (programclass==null || programclass.isEmpty()) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nMissing 'class' attribute for program");
              if (programclass.equalsIgnoreCase("MotifDiscovery")) externalProgram=new MotifDiscovery();
         else if (programclass.equalsIgnoreCase("MotifScanning")) externalProgram=new MotifScanning();
         else if (programclass.equalsIgnoreCase("ModuleDiscovery")) externalProgram=new ModuleDiscovery();
         else if (programclass.equalsIgnoreCase("ModuleScanning")) externalProgram=new ModuleScanning();
         else if (programclass.equalsIgnoreCase("EnsemblePrediction")) externalProgram=new EnsemblePredictionMethod();
         else externalProgram=new ExternalProgram();
         externalProgram.name=programName;
         externalProgram.programclass=programclass;
         externalProgram.cygwin=(programCygwin!=null && (programCygwin.equalsIgnoreCase("yes") || programCygwin.equalsIgnoreCase("true"))); // this is a boolean property 
         
         // --- Read properties ---
         NodeList propertiesnodes = programNode.getElementsByTagName("properties");
         if (propertiesnodes.getLength()>0 && keepAllProperties) {
             Element propertiesNode = (Element) propertiesnodes.item(0);
             NodeList propertiesList=propertiesNode.getChildNodes();
             for (int i=0;i<propertiesList.getLength();i++) {
                 Node property = (Node)propertiesList.item(i);
                 String propertyName=property.getNodeName();
                 if (propertyName.equals("#text")) continue; // a hack !
                 String propertyValue=property.getTextContent();
                 propertyValue=propertyValue.replace("&lt;", "<"); // unescape escaped html
                 propertyValue=propertyValue.replace("&gt;", ">"); // unescape escaped html
                 externalProgram.setProperty(propertyName, propertyValue, false);
                 // System.err.println("Adding Property: "+propertyName+" => "+propertyValue);
             }             
         } 
      
         // --- Read service ---
         NodeList servicenodes = programNode.getElementsByTagName("service");
         if (servicenodes.getLength()==0) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nNo 'service' node found");
         Element serviceNode = (Element) servicenodes.item(0);
         externalProgram.serviceType=serviceNode.getAttribute("type");
         if (externalProgram.serviceType==null || externalProgram.serviceType.isEmpty()) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nMissing 'type' attribute for service");
         externalProgram.location=serviceNode.getAttribute("location");                 
         NodeList executableSourcesList = serviceNode.getElementsByTagName("source");
         if (executableSourcesList.getLength()>0) {
             externalProgram.executableSources=new ArrayList<String[]>(executableSourcesList.getLength());
             for (int i=0;i<executableSourcesList.getLength();i++) {
                 Element source = (Element)executableSourcesList.item(i);
                 String os=source.getAttribute("os");
                 String version=source.getAttribute("version");
                 String url=source.getAttribute("url");
                 String compression=source.getAttribute("compression");
                 if (compression!=null && (compression.equalsIgnoreCase("none") || compression.isEmpty())) compression=null;
                 String relativeZIPpath=source.getAttribute("targetInZIP");
                 if (compression!=null && (relativeZIPpath==null || relativeZIPpath.isEmpty())) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nLocation of executable file within the archive must be specified!");
                 externalProgram.executableSources.add(new String[]{os,version,url,compression,relativeZIPpath});
             }
         }
         // --- Check requirements ---
         NodeList requirementsList = serviceNode.getElementsByTagName("require");
         if (requirementsList.getLength()>0) {
             externalProgram.requirements=new ArrayList<String>(requirementsList.getLength());
             for (int i=0;i<requirementsList.getLength();i++) {
                 Element req = (Element)requirementsList.item(i);
                 String reqString=req.getTextContent();
                 externalProgram.requirements.add(reqString);
             }
         }             
         // --- Check if there are different version for different operating systems ---
         NodeList systemsnodes = programNode.getElementsByTagName("system");
         if (systemsnodes.getLength()>0) {
            programNode=getOperatingSystemSpecificNode(systemsnodes); // find the appropriate system node and "enter it" by reassigning the programNode to the system node
            if (programNode==null) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nNo applicable 'system' node found for operating system: "+System.getProperty("os.name"));
         }
         // --- Read command line ---
         NodeList commandsnodes = programNode.getElementsByTagName("commands");
         if (commandsnodes.getLength()>0) { // multiple commands
            Element commandsNode=getOperatingSystemSpecificNode(commandsnodes);
            if (commandsNode==null) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nNo applicable 'commands' node found for operating system: "+System.getProperty("os.name"));
            NodeList commandNodes = commandsNode.getElementsByTagName("command");
            if (commandNodes==null || commandNodes.getLength()==0) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nNo applicable 'command' node found for operating system: "+System.getProperty("os.name"));
            externalProgram.commands=new String[commandNodes.getLength()];
            for (int i=0;i<commandNodes.getLength();i++) {
                Element thisCommandNode = (Element)commandNodes.item(i);
                externalProgram.commands[i]=thisCommandNode.getTextContent();
                String quote=thisCommandNode.getAttribute("quotepath"); // I just use the last seen (perhaps not the best choice :| )
                if (quote!=null && !quote.isEmpty()) externalProgram.quotepath=quote;                   
            }
         } else { // check for single command node(s)
             NodeList commandnodes = programNode.getElementsByTagName("command");
             if (commandnodes.getLength()>0) {
                 Element commandNode=getOperatingSystemSpecificNode(commandnodes);
                 if (commandNode==null) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nNo applicable 'command' node found for operating system: "+System.getProperty("os.name"));
                 externalProgram.commands=new String[]{commandNode.getTextContent()};
                 String separator=commandNode.getAttribute("separator");
                 if (separator!=null && !separator.isEmpty()) externalProgram.commandseparator=separator;
                 String quote=commandNode.getAttribute("quotepath");
                 if (quote!=null && !quote.isEmpty()) externalProgram.quotepath=quote;                                  
             }
         }
         //if (externalProgram.location==null || externalProgram.location.isEmpty()) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]: Missing 'location' attribute for service");          
         // --- Read parameter tags ---
         NodeList parameternodes = programNode.getElementsByTagName("parameter");
         for (int i=0;i<parameternodes.getLength();i++) {
             boolean required=true;
             boolean hidden=false;
             boolean advanced=false;
             Element parameter = (Element)parameternodes.item(i);
             String parameterName=parameter.getAttribute("name");
             if (parameterName==null || parameterName.isEmpty()) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nMissing 'name' attribute for parameter #"+(i+1));
             String parameterType=parameter.getAttribute("type");
             if (parameterType==null || parameterType.isEmpty()) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nMissing 'type' attribute for parameter '"+parameterName+"'");
             String parameterRequired=parameter.getAttribute("required");
             if (parameterRequired!=null && parameterRequired.equalsIgnoreCase("no")) required=false;
             String parameterHidden=parameter.getAttribute("hidden");
             if (parameterHidden!=null && parameterHidden.equalsIgnoreCase("yes")) hidden=true;
             String parameterAdvanced=parameter.getAttribute("advanced");
             if (parameterAdvanced!=null && parameterAdvanced.equalsIgnoreCase("yes")) advanced=true;
             String parameterSkipIfDefault=parameter.getAttribute("skipIfDefault");
             if (parameterSkipIfDefault!=null && parameterSkipIfDefault.equalsIgnoreCase("yes")) externalProgram.skipIfDefault.add(parameterName);
             String parameterClass=parameter.getAttribute("class");
             if (parameterClass==null || parameterClass.isEmpty()) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nMissing 'class' attribute for parameter '"+parameterName+"'");
             boolean plusNumeric=parameterClass.endsWith("+");
             if (plusNumeric) parameterClass=parameterClass.substring(0,parameterClass.length()-1); // trim off "+" suffix
             Class parameterclass=null;
             try {
                 parameterclass=MotifLabEngine.getClassForName(parameterClass);
             } catch (ClassNotFoundException cnf) {throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nUnrecognized class '"+parameterClass+"'");}
             String parameterLink=parameter.getAttribute("link");
             if (parameterLink!=null && !parameterLink.isEmpty()) {
                 externalProgram.referenceLinks.add(new String[]{parameterName,null,parameterClass,parameterLink});
             }
             parameterLink=parameter.getAttribute("softlink");
             if (parameterLink!=null && !parameterLink.isEmpty()) {
                 externalProgram.referenceLinks.add(new String[]{parameterName,null,parameterClass,parameterLink});
                 externalProgram.softlinks.add(parameterName);
             }          
             // parse conditions
             ArrayList<ParameterCondition> conditions=null;
             NodeList parameterconditions = parameter.getElementsByTagName("condition");
             for (int j=0;j<parameterconditions.getLength();j++) {
                 Element conditionElement = (Element)parameterconditions.item(j);
                 String ifClause=null;
                 boolean negation=false;
                 String monitoredParameter=conditionElement.getAttribute("monitor");
                 ifClause=conditionElement.getAttribute("if");
                 if (ifClause==null || ifClause.isEmpty()) {
                     ifClause=conditionElement.getAttribute("ifNot");
                     if (ifClause!=null && !ifClause.isEmpty()) negation=false;
                     else throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nMissing 'if' or 'ifNot' attribute for condition for parameter '"+parameterName+"'");
                 }
                 String thenClause=conditionElement.getAttribute("then");
                 if (thenClause==null || thenClause.isEmpty()) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nMissing 'then' attribute for condition for parameter '"+parameterName+"'");
                 String elseClause=conditionElement.getAttribute("else");
                 if (elseClause!=null && elseClause.isEmpty()) elseClause=null;                
                 ParameterCondition newCondition=new ParameterCondition(parameterName, monitoredParameter, ifClause, negation, thenClause, elseClause);
                 if (conditions==null) conditions=new ArrayList<ParameterCondition>();
                 conditions.add(newCondition);
             }                 
                    
             //System.err.println("{"+programName+"} parameter: name="+parameterName+" type="+parameterType+" class="+parameterClass+" link="+parameterLink);
             String parameterDescription=null;
             NodeList parameterDescriptionList = parameter.getElementsByTagName("description");
             if (parameterDescriptionList.getLength()>0) {
                parameterDescription = ((Element)parameterDescriptionList.item(0)).getTextContent();
                if (parameterDescription.isEmpty()) parameterDescription=null;
             }
             String defaultValueString=null;
             NodeList parameterDefaultValueList = parameter.getElementsByTagName("default");
             if (parameterDefaultValueList.getLength()>0) {
                 defaultValueString = ((Element)parameterDefaultValueList.item(0)).getTextContent();
             }                          
             Object parameterDefaultValue=null;
             Object[] allowedValues=null;
             if (parameterClass.equals("Integer")) {
                 if (defaultValueString==null || defaultValueString.isEmpty()) parameterDefaultValue=null;
                 else try {parameterDefaultValue=new Integer(Integer.parseInt(defaultValueString));} catch (NumberFormatException nf) {throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nUnable to parse expected numeric default value for parameter '"+parameterName+"' => "+defaultValueString);}
                 String minValue=getTextContentFromChildElement(parameter,"min");
                 String maxValue=getTextContentFromChildElement(parameter,"max");
                 int min=0;
                 int max=1;
                 if (minValue!=null && !minValue.isEmpty()) try {min=Integer.parseInt(minValue);} catch (NumberFormatException nf) {throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nUnable to parse expected numeric minimum value for parameter '"+parameterName+"' => "+minValue);}
                 if (maxValue!=null && !maxValue.isEmpty()) try {max=Integer.parseInt(maxValue);} catch (NumberFormatException nf) {throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nUnable to parse expected numeric maximum value for parameter '"+parameterName+"' => "+maxValue);}
                 allowedValues=new Integer[]{min,max};
             } else if (parameterClass.equals("Double") || parameterClass.equals("Float")) {
                 if (defaultValueString==null || defaultValueString.isEmpty()) parameterDefaultValue=null;
                 else try {parameterDefaultValue=new Double(Double.parseDouble(defaultValueString));} catch (NumberFormatException nf) {throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nUnable to parse expected numeric default value for parameter '"+parameterName+"' => "+defaultValueString);}
                 String minValue=getTextContentFromChildElement(parameter,"min");
                 String maxValue=getTextContentFromChildElement(parameter,"max");
                 double min=0;
                 double max=1;
                 if (minValue!=null && !minValue.isEmpty()) try {min=Double.parseDouble(minValue);} catch (NumberFormatException nf) {throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nUnable to parse expected numeric minimum value for parameter '"+parameterName+"' => "+minValue);}
                 if (maxValue!=null && !maxValue.isEmpty()) try {max=Double.parseDouble(maxValue);} catch (NumberFormatException nf) {throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nUnable to parse expected numeric maximum value for parameter '"+parameterName+"' => "+maxValue);}
                 allowedValues=new Double[]{min,max};
             } else if (parameterClass.endsWith("NumericMap")) {
                 if (defaultValueString==null || defaultValueString.isEmpty()) parameterDefaultValue=null;
                 else try {parameterDefaultValue=new Double(Double.parseDouble(defaultValueString));} catch (NumberFormatException nf) {throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nUnable to parse expected numeric default value for parameter '"+parameterName+"' => "+defaultValueString);}
//                 String minValue=getTextContentFromChildElement(parameter,"min");
//                 String maxValue=getTextContentFromChildElement(parameter,"max");
//                 double min=0;
//                 double max=1;
//                 if (minValue!=null && !minValue.isEmpty()) try {min=Double.parseDouble(minValue);} catch (NumberFormatException nf) {throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nUnable to parse expected numeric minimum value for parameter '"+parameterName+"' => "+minValue);}
//                 if (maxValue!=null && !maxValue.isEmpty()) try {max=Double.parseDouble(maxValue);} catch (NumberFormatException nf) {throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nUnable to parse expected numeric maximum value for parameter '"+parameterName+"' => "+maxValue);}
                 allowedValues=(plusNumeric)?new Class[]{parameterclass,NumericVariable.class}:new Class[]{parameterclass};
             } else if (parameterClass.endsWith("Map")) {
                 if (defaultValueString==null || defaultValueString.isEmpty()) parameterDefaultValue=null;
                 else parameterDefaultValue=defaultValueString;
                 allowedValues=new Class[]{parameterclass};
             } else if (parameterClass.equals("String")) {
                 if (defaultValueString==null || defaultValueString.isEmpty()) parameterDefaultValue=null;
                 else parameterDefaultValue=defaultValueString;
                 allowedValues=getStringOptions(parameter, parameterName, externalProgram);
             } else if (parameterClass.equals("Boolean")) {
                 if (defaultValueString==null || defaultValueString.isEmpty()) parameterDefaultValue=null;
                 else try {parameterDefaultValue=Boolean.parseBoolean(defaultValueString);} catch (NumberFormatException nf) {throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nUnable to parse expected boolean default value for parameter '"+parameterName+"' => "+defaultValueString);}
                 allowedValues=new Boolean[]{Boolean.TRUE,Boolean.FALSE};
             } else if (Data.class.isAssignableFrom(parameterclass)) {
                  allowedValues=new Class[]{parameterclass};
                  parameterDefaultValue=null;
             } else {throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nParameter-class '"+parameterClass+"' for parameter '"+parameterName+"' is neither Data nor allowed basic type (Integer|Float|Boolean|String)");}
             // Read argument switch
             NodeList argumentlist = parameter.getElementsByTagName("argument");
             if (argumentlist.getLength()>0) { // Note that only one argument element is allowed per parameter
                String type=((Element)argumentlist.item(0)).getAttribute("type");
                externalProgram.setArgumentType(parameterName, type);
                if (type.equalsIgnoreCase("STDIN") || type.equalsIgnoreCase("STDOUT")) externalProgram.setRedirectionForArgument(parameterName, type.toUpperCase());
                if (type.equalsIgnoreCase("explicit")) {
                    String filename=((Element)argumentlist.item(0)).getAttribute("filename");
                    if (filename!=null && !filename.isEmpty()) {
                        externalProgram.setExplicitArgument(parameterName,filename);
                    } else throw new SystemError("Missing 'filename' attribute for 'explicit' parameter '"+parameterName+"'");                       
                }
                if (type.equalsIgnoreCase("implicit")) {
                    String filename=((Element)argumentlist.item(0)).getAttribute("filename");
                    if (filename!=null && !filename.isEmpty()) {
                        externalProgram.setImplicitArgument(parameterName,filename);
                    } else { // Implicit parameter is missing a filname. Check if parameter links to another. If not, throw an exception
                        if (externalProgram.getParameterReferenceLink(parameterName, null)!=null) externalProgram.setImplicitArgument(parameterName,null); // the null is OK for now
                        else throw new SystemError("Implicit parameter '"+parameterName+"' is missing filename or link");
                    }                      
                }                
                String switchString=((Element)argumentlist.item(0)).getAttribute("switch");
                if (switchString!=null && !switchString.isEmpty()) externalProgram.setArgumentSwitch(parameterName,switchString);
                String switchSeparatorString=(((Element)argumentlist.item(0)).hasAttribute("switchseparator"))?((Element)argumentlist.item(0)).getAttribute("switchseparator"):null;
                if (switchSeparatorString!=null) externalProgram.setArgumentSwitchSeparator(parameterName,switchSeparatorString);
             }   
             // read dataformat
             NodeList dataformatlist = parameter.getElementsByTagName("dataformat");
             if (dataformatlist.getLength()>0) {
                Element dataformatElement=((Element)dataformatlist.item(0));
                setDataFormatAndSettings(parameterName, dataformatElement, externalProgram);
             }                        
                  if (parameterType.equals("regular")) externalProgram.addParameter(parameterName,parameterclass,parameterDefaultValue,allowedValues,parameterDescription,required,hidden,advanced,conditions);
             else if (parameterType.equals("source")) externalProgram.addSourceParameter(parameterName,parameterclass,parameterDefaultValue,allowedValues,parameterDescription);
             else if (parameterType.equals("result")) externalProgram.addResultParameter(parameterName,parameterclass,parameterDefaultValue,allowedValues,parameterDescription);
             externalProgram.allArguments.add(parameterName);
         } // END: for each parameter element
         
         // --- Read tempdir declarations ---
         NodeList tempdirnodes = programNode.getElementsByTagName("temporary");
         for (int i=0;i<tempdirnodes.getLength();i++) {
             Element tempdir = (Element)tempdirnodes.item(i);
             String tempdirName=tempdir.getAttribute("filename");
             if (tempdirName==null || tempdirName.isEmpty()) throw new SystemError("An error occurred while loading External Program ["+sourcename+"]:\n\nMissing 'filename' attribute for temporary-element #"+(i+1));
             externalProgram.temporaryfilenames.add(tempdirName);
         }    
         
         // --- Read reports ---
         NodeList reportssnodes = programNode.getElementsByTagName("reports");
         if (reportssnodes.getLength()>0) {
             Element reportNode = (Element) reportssnodes.item(0);
             NodeList reportList=reportNode.getChildNodes();
             if (reportList.getLength()>0) {
                 externalProgram.reportExpressions=new ArrayList<Report>();            
                 for (int i=0;i<reportList.getLength();i++) {
                     if (!(reportList.item(i) instanceof Element)) continue;
                     Element report = (Element)reportList.item(i);
                     String expressionString=report.getAttribute("expression");
                     String targetString=report.getAttribute("target");
                     String outputString=report.getAttribute("output");
                     Report reportObject=new Report(expressionString,targetString,outputString);
                     externalProgram.reportExpressions.add(reportObject);
                 }
             }             
         }          
       } // -- end try
       catch (SystemError se) {
           throw se;
       } catch (Exception e) {
           //e.printStackTrace(System.err);
           throw new SystemError("An error occurred while loading External Program:\n\n["+e.getClass().getSimpleName()+"]: "+e.getMessage());
       }      
       return externalProgram;
}  

/** Returns Eode in the NodeList which has its "OS" attribute corresponding to
 *  the current operating system or a default node (with no OS argument) 
 *  if no matching OS-node can be found
 */
private static Element getOperatingSystemSpecificNode(NodeList nodelist) {
     if (nodelist==null || nodelist.getLength()==0) return null;
     String operatingsystem=System.getProperty("os.name").toLowerCase();
     Element commandNode=null;
     for (int i=0;i<nodelist.getLength();i++) {
       Element thisCommandNode = (Element)nodelist.item(i);
       String osString=thisCommandNode.getAttribute("os").toLowerCase();
       if (osString.isEmpty() && commandNode==null) commandNode=thisCommandNode; // use "no OS" as default if no more specific node can be found
            if (operatingsystem.contains("windows") && osString.startsWith("windows")) return thisCommandNode;
//       else if (operatingsystem.contains("mac") && osString.startsWith("mac")) return thisCommandNode;
//       else if (operatingsystem.contains("linux") && osString.startsWith("linux")) return thisCommandNode;
       else if (operatingsystem.equals(osString)) return thisCommandNode;
     }
     return commandNode;
}


/** Given a reference to a parent node in an XML-document, and the name of a child-element
 * (assuming only one child element with the given tag name exists) this method will return 
 * the text content of the child (or NULL if no such child exists).
 */    
private static String getTextContentFromChildElement(Element parent, String childName) {
     NodeList list = parent.getElementsByTagName(childName);
     if (list.getLength()>0) {
        return ((Element)list.item(0)).getTextContent();
     } else return null;   
}


/** Given a reference to a parent node in an XML-document, and the name of child-elements
 * (assuming multiple children with the given tag name can exist) this method will return 
 * a String array containing the text content of all such child tags 
 * (or NULL if no children with the given tag name exists)
 */    
private static String[] getTextContentFromChildElements(Element parent, String childName) {
     NodeList list = parent.getElementsByTagName(childName);
     if (list.getLength()==0) return null;
     String[] result=new String[list.getLength()];
     for (int i=0;i<list.getLength();i++) {         
        result[i]=((Element)list.item(i)).getTextContent();
     }
     return result;
}    

/** 
 * Returns a list of 'option' fields for parameters of the String type
 * Also populates the optionValueMap lookup-table if the options have alternative values
 */    
private static String[] getStringOptions(Element parent, String parameterName, ExternalProgram program) {
     NodeList list = parent.getElementsByTagName("option");
     if (list.getLength()==0) return null;
     String[] result=new String[list.getLength()];
     for (int i=0;i<list.getLength();i++) {         
        result[i]=((Element)list.item(i)).getTextContent();
        org.w3c.dom.Attr valueattr=((Element)list.item(i)).getAttributeNode("value");
        if (valueattr!=null) {
            if (program.optionValueMap==null) program.optionValueMap=new HashMap<String,HashMap<String,String>>();
            HashMap<String,String>map=program.optionValueMap.get(parameterName);
            if (map==null) {map=new HashMap<String,String>();program.optionValueMap.put(parameterName, map);}
            map.put(result[i], valueattr.getValue());            
        }
     }
     return result;
}    


/** 
 * Reads a part of a tree that corresponds to a DataFormat element and sets 
 * the relevant properties in the external program configuration 
 */
private static void setDataFormatAndSettings(String parameterName, Element dataformatElement, ExternalProgram externalProgram) throws SystemError {
     //System.err.println("Reading dataformat for "+parameterName+"  "+dataformatElement.getTagName()+"  "+dataformatElement.getAttribute("name"));
     String formatName=dataformatElement.getAttribute("name");
     if (formatName==null || formatName.isEmpty()) throw new SystemError("An error occurred while loading External Program ["+externalProgram.getName()+"]:\n\nMissing 'name' attribute for dataformat");    
     // if (engine.getDataFormat(formatName)==null) throw new SystemError("An error occurred while loading External Program ["+externalProgram.getName()+"]:\n\nUnknown dataformat '"+formatName+"'");    
     externalProgram.setArgumentDataFormat(parameterName, formatName);
     NodeList parameterlist = dataformatElement.getElementsByTagName("setting");
     if (parameterlist.getLength()==0) return; // no more to do here
     ParameterSettings settings=new ParameterSettings();
     for (int i=0;i<parameterlist.getLength();i++) {         
         Element dataformatParameter=(Element)parameterlist.item(i);
         String dataformatParameterName=dataformatParameter.getAttribute("name");
         if (dataformatParameterName==null || dataformatParameterName.isEmpty()) throw new SystemError("An error occurred while loading External Program ["+externalProgram.getName()+"]:\n\n Missing 'name' attribute for parameter in dataformat["+formatName+"] for program-parameter '"+parameterName+"'");
         String dataformatParameterClass=dataformatParameter.getAttribute("class");
         if (dataformatParameterClass==null || dataformatParameterClass.isEmpty()) throw new SystemError("An error occurred while loading External Program ["+externalProgram.getName()+"]:\n\n Missing 'type' attribute for parameter in dataformat["+formatName+"] for program-parameter '"+parameterName+"'");
         //System.err.println("setDataFormatAndSettings["+parameterName+"]["+formatName+"]: name="+dataformatParameterName+"  class="+dataformatParameterClass);
         String dataformatParameterLink=dataformatParameter.getAttribute("link");
         if (dataformatParameterLink!=null && !dataformatParameterLink.isEmpty()) { // the value of this setting links a different parameter
             externalProgram.referenceLinks.add(new String[]{parameterName,dataformatParameterName,dataformatParameterClass,dataformatParameterLink});
             settings.setParameter(dataformatParameterName, null);
             continue; // the reference links will be resolved later!
         }
         String dataformatParameterValueString=dataformatParameter.getTextContent();
         if (dataformatParameterValueString==null || dataformatParameterValueString.isEmpty()) throw new SystemError("An error occurred while loading External Program ["+externalProgram.getName()+"]:\n\n Missing 'value' attribute for parameter in dataformat["+formatName+"] for program-parameter '"+parameterName+"'");
         Object dataFormatParameterValue=null;
         if (dataformatParameterClass.equals("Integer")) {
             try {dataFormatParameterValue=new Integer(Integer.parseInt(dataformatParameterValueString));} catch (NumberFormatException nf) {throw new SystemError("An error occurred while loading External Program ["+externalProgram.getName()+"]:\n\n Unable to parse expected numeric value for parameter in dataformat["+formatName+"] for program-parameter '"+parameterName+"'");}
         } else if (dataformatParameterClass.equals("Double")) {
             try {dataFormatParameterValue=new Double(Double.parseDouble(dataformatParameterValueString));} catch (NumberFormatException nf) {throw new SystemError("An error occurred while loading External Program ["+externalProgram.getName()+"]:\n\n Unable to parse expected numeric value for parameter in dataformat["+formatName+"] for program-parameter '"+parameterName+"'");}
         } else if (dataformatParameterClass.equals("String")) {
             dataFormatParameterValue=dataformatParameterValueString;
         } else if (dataformatParameterClass.equals("Boolean")) {
             try {dataFormatParameterValue=Boolean.parseBoolean(dataformatParameterValueString);} catch (NumberFormatException nf) {throw new SystemError("An error occurred while loading External Program ["+externalProgram.getName()+"]:\n\n Unable to parse expected boolean value for parameter in dataformat["+formatName+"] for program-parameter '"+parameterName+"'");}
         } else {throw new SystemError("An error occurred while loading External Program ["+externalProgram.getName()+"]:\n\n Illegal type for parameter in dataformat["+formatName+"] for program-parameter '"+parameterName+"'");}
         // note that dataformatParameterClass can be more complex than these elementary constants 
         // but only if the setting links to another parameter (which is checked above)
         settings.setParameter(dataformatParameterName, dataFormatParameterValue);
     } 
     externalProgram.setArgumentDataFormatSettings(parameterName, settings);
     //System.err.println("Settings for "+parameterName+" => ");
}

/**
 * This method resolves any bindings between DataFormat parameter settings and
 * values of other parameters. (It is possible to link or bind the value of 
 * a DataFormat parameter to values of normal ExternalProgram parameters, so that
 * the first will use the value of the latter). 
 * 
 * @param program A link to the external program
 * @param argument The name of the parameter whose DataFormat settings might need to be resolved
 * @param settings The settings for the DataFormat used by the given parameter
 * @throws java.lang.Exception
 */
public void resolveDataFormatLinks(ExternalProgram program, String argument, ParameterSettings settings, OperationTask task) throws Exception {    
    for (String[] vector:referenceLinks) {
        if (!vector[0].equals(argument) || vector[1]==null) continue; //
        String dataformatParameterName=vector[1];
        String link=vector[3];            
        Parameter externalProgramParameter=program.getParameterWithName(link);
        if (externalProgramParameter==null) throw new ExecutionError("DataFormat setting '"+dataformatParameterName+"' used by parameter '"+argument+"' links to unknown parameter '"+link+"'");
        Object value=task.getParameter(link);
        settings.setParameter(dataformatParameterName, value);
    }
}

/** Returns the name of a parameter that is referenced by the argument parameter  (or null if argument parameter has no references)
 *  This method is used to resolve parameter links in both normal parameters and dataformat settings.
 *  When used for dataformat settings the name of 'parent' parameter (normal parameter) is the first argument
 *  and the name of the dataformat-setting parameter is the second argument.
 *  When used to retrieve links for normal parameters, the second argument should be null  
 */
public String getParameterReferenceLink(String parameterName, String dataformatParameter) {  
    for (String[] vector:referenceLinks) {
        if (!vector[0].equals(parameterName)) continue; //
        if (dataformatParameter==null && vector[1]==null) return vector[3]; // vector[3] holds the link
        if (dataformatParameter!=null && vector[1]!=null && vector[1].equals(dataformatParameter)) return vector[3]; //
    }
    return null;
}



/** 
 * This method executes the given program external program 
 * (the superclass executes locally installed programs only!)
 * All needed resources (like parameter settings) should be available in the 
 * OperationTask object before execution and on successful completion the results 
 * will be made available in the OperationTask object 
 */
public void execute(OperationTask task) throws Exception {
        int sourcecount=-1;
        int resultscount=-1;
        int expectedSourceCount=sourceParameterFormats.size();
        int expectedResultsCount=resultsParameterFormats.size();
        Data[] datasources=(Data[])task.getParameter(SOURCES);
        if ((datasources!=null && datasources.length<expectedSourceCount) || (expectedSourceCount>0 && datasources==null)) throw new ExecutionError("SYSTEM ERROR: Not enough sources added to task for execution of external program "+getName()+" (Expected "+expectedSourceCount+" got "+((datasources==null)?"null":datasources.length)+")");
        File[] resultFiles=new File[expectedResultsCount];        
        task.setStatus(ExecutableTask.WAITING); // indeterminate progress
        String[] commandLines=getCommands(); // these contain templates that will be substituted with actual parameter values for the argument field codes in braces
        String[] unresolvedRediretionsCommandLines=getCommands(); // this array will be almost the same as the (resolved) 'commandLines' except that redirected arguments are not resolved
        boolean hasCommandLines=false; // is the command line explicitly stated in the XML config-file?
        if (commandLines==null || commandLines.length==0) {
            commandLines=new String[]{"%PROGRAM"};
            unresolvedRediretionsCommandLines=new String[]{"%PROGRAM"};
        }
        else hasCommandLines=true;
        String workdir=engine.getNewWorkDirectory(); // this will create a new workdir to be used only by this particular execution (to avoid name clash with implicit/explicit files)!
        // build a command line with applicable arguments and their values (as specified in the OperationTask)
        HashMap<String,Object> resolvedArguments=new HashMap<String,Object>(allArguments.size()); // keeps the values for arguments that have been resolved so far
        for (String argument:allArguments) {
            String currentargumentCommandLineString=""; //this is output on the command line (either appended or replaced for references)
            Parameter parameter=getParameterWithName(argument);
            //System.err.println("Argument["+argument+"] => "+parameter);            
            if (parameter!=null) { // the argument is a regular parameter!              
                Object dataobject=task.getParameter(argument);
                //System.err.println("Argument["+argument+"] data => "+((dataobject!=null)?(dataobject.getClass().toString()):"null"));      
                if (getParameterReferenceLink(argument, null)!=null) { // the parameter references another parameter (either hard link or soft link)
                   if (isSoftLink(argument)) {
                       String linkRefersTo=getParameterReferenceLink(argument, null);
                       dataobject=task.getParameter(linkRefersTo);
                       //System.err.println("   softlinked to '"+getParameterReferenceLink(argument, null)+"', value="+dataobject);
                   } else { // "hard link"
                       Object linkedValue=resolvedArguments.get(getParameterReferenceLink(argument, null));
                       //System.err.println("   linked to '"+getParameterReferenceLink(argument, null)+"', value="+linkedValue);
                       resolvedArguments.put(argument, linkedValue);
                       currentargumentCommandLineString=getCommandStringForParameter(argument,linkedValue);
                   }
                } // end "parameter is a link"
                // argument is not a link (or maybe it is a 'soft link' which must be output anew
                if (dataobject==null || dataobject.toString().isEmpty() || !currentargumentCommandLineString.isEmpty()) {}  // this probably means that an optional parameter has been omitted. Do not mention it on the command line.  (or it can be a link).                 
                else if (isComplexType(dataobject)) {
                   //System.err.println("  complex");
                   File tempFile;
                   if (dataobject instanceof FeatureDataset) {
                       DataFormat dataobjectFormat=getArgumentDataFormat(argument);
                       if (dataobjectFormat==null) throw new ExecutionError("SYSTEM ERROR: Unspecified (or unrecognized) data format for "+getName()+" argument "+argument);
                       ParameterSettings dataobjectFormatSettings=getArgumentDataFormatSettings(argument);
                       resolveDataFormatLinks(this,argument,dataobjectFormatSettings,task);
                       if (isImplicitArgument(argument)) tempFile=new File(resolveGlobalDirectoryReferences(getImplicitArgument(argument),workdir));
                       else if (isExplicitArgument(argument)) tempFile=new File(resolveGlobalDirectoryReferences(getExplicitArgument(argument),workdir));
                       else tempFile=engine.createTempFile(workdir);
                       outputSourceSequences((FeatureDataset)dataobject,dataobjectFormat,dataobjectFormatSettings,task,tempFile);
                   }
                   else tempFile=createTempFile(this, argument, workdir, task);
                   resolvedArguments.put(argument, tempFile);
                   currentargumentCommandLineString=getCommandStringForParameter(argument,tempFile);
                } else { // argument is non-complex type: String, Number or Boolean
                   Object value=dataobject;
                   if (value instanceof String && optionValueMap!=null && optionValueMap.containsKey(argument)) { // this is a String option which could have specific key->value pairing that must be resolved
                       HashMap<String,String> map=optionValueMap.get(argument);
                       if (map.containsKey((String)value)) {
                           String valuestring=map.get((String)value); // lookup
                           if (valuestring==null || valuestring.isEmpty()) value=null;
                           else value=valuestring;
                           //change the value for the String parameter in the task object in case other data format settings links to it later
                           task.setParameter(argument, value); // I sincerely hope this is OK and does not interfere with something else :-| 
                       }                       
                   }
                   resolvedArguments.put(argument, value);
                   currentargumentCommandLineString=getCommandStringForParameter(argument,value);                
                }                              
            } else if (isSourceParameter(argument)) {                
                DataFormat sourceFormat=getArgumentDataFormat(argument);
                if (sourceFormat==null) throw new ExecutionError("SYSTEM ERROR: Unspecified (or unrecognized) data format for "+getName()+" source file");
                ParameterSettings sourceFormatSettings=getArgumentDataFormatSettings(argument);
                try {
                    sourcecount++;
                    Data datasource=datasources[sourcecount];
                    File sourceFile;     
                    if (getParameterReferenceLink(argument, null)!=null) { // the parameter references another parameter
                         Object linkedValue=resolvedArguments.get(getParameterReferenceLink(argument, null));
                         if (linkedValue==null) throw new ExecutionError("Source parameter links to unresolved parameter (or target parameter has value NULL)");
                         if (!(linkedValue instanceof File)) throw new ExecutionError("Source parameters can only reference 'complex' data types");
                         sourceFile=(File)linkedValue;
                    }                     
                    else if (datasource instanceof FeatureDataset) {              
                        if (isImplicitArgument(argument)) sourceFile=resolveImplicitFileForArgument(argument,workdir);
                        else if (isExplicitArgument(argument)) sourceFile=resolveExplicitFileForArgument(argument,workdir);
                        else sourceFile=engine.createTempFile(workdir);                          
                        resolveDataFormatLinks(this,argument,sourceFormatSettings,task);
                        outputSourceSequences((FeatureDataset)datasource,sourceFormat,sourceFormatSettings,task,sourceFile);
                    }
                    else sourceFile=createTempFile(this, argument, workdir, task); // this outputs the data to a temp-file (dataformat and settings are found in the task object)                    
                    resolvedArguments.put(argument, sourceFile);
                    currentargumentCommandLineString=getCommandStringForParameter(argument,sourceFile);  
                } catch (IOException io) {
                    throw new ExecutionError("Unable to create needed temporary file for source ("+io.getClass().getSimpleName()+")\n\n"+io.getMessage(),io);
                }                
            } else if (isResultsParameter(argument)) {
                resultscount++;
                if (getParameterReferenceLink(argument, null)!=null) { // the parameter references another parameter
                     Object linkedValue=resolvedArguments.get(getParameterReferenceLink(argument, null));
                     if (linkedValue==null) throw new ExecutionError("Results parameter links to unresolved parameter (or target parameter has value NULL)");
                     if (!(linkedValue instanceof File)) throw new ExecutionError("Results parameters can only reference 'complex' data types");
                     resultFiles[resultscount]=(File)linkedValue;
                } 
                else if (isImplicitArgument(argument)) {
                    resultFiles[resultscount]=resolveImplicitFileForArgument(argument,workdir);
                    resultFiles[resultscount].deleteOnExit();
                }
                else if (isExplicitArgument(argument)) {
                    resultFiles[resultscount]=resolveExplicitFileForArgument(argument,workdir);
                    resultFiles[resultscount].deleteOnExit();
                }
                else {                     
                    resultFiles[resultscount]=engine.createTempFile(workdir); // these temp-files are deleted on exit
                } 
                if (resultFiles[resultscount]==null) throw new ExecutionError("Unable to create needed temporary file for results");  
                resolvedArguments.put(argument, resultFiles[resultscount]);
                currentargumentCommandLineString=getCommandStringForParameter(argument,resultFiles[resultscount]);  
            } else throw new ExecutionError("SYSTEM ERROR: Unknown parameter: "+argument);
            String argumentReferenceName="{"+argument+"}"; // use the name in braces to refer to it on the command line

            if (hasCommandLines) {
                //System.err.println("["+argument+"]  refName='"+argumentReferenceName+"'  value='"+currentargumentCommandLineString+"'");
                if (argumentReferenceName!=null && !argumentReferenceName.isEmpty()) {
                    for (int i=0;i<commandLines.length;i++) commandLines[i]=commandLines[i].replace(argumentReferenceName, currentargumentCommandLineString);
                } // replace references to the argument in the command line with the arguments resolved value
                if (!isRedirectionArgument(argument)) { //
                  if (argumentReferenceName!=null && !argumentReferenceName.isEmpty()) {
                     for (int i=0;i<unresolvedRediretionsCommandLines.length;i++) unresolvedRediretionsCommandLines[i]=unresolvedRediretionsCommandLines[i].replace(argumentReferenceName, currentargumentCommandLineString);
                  }
                }
            } else { // no explicit command line(s) are given. Just add arguments to the end of the line
                if (!currentargumentCommandLineString.isEmpty() && !isImplicitArgument(argument)) {
                    // System.err.println("Adding to command line["+argument+"]:"+currentargumentCommandLineString);
                    commandLines[0]+=(" "+currentargumentCommandLineString);
                } 
                if (isRedirectionArgument(argument)) unresolvedRediretionsCommandLines[0] += (" " + argumentReferenceName);
                else unresolvedRediretionsCommandLines[0] += (" "+currentargumentCommandLineString);
            }
        } // end of foreach argument

        // resolve additional references in the command line(s)
        String programLocation=getLocation();   
        programLocation="\""+programLocation+"\""; // quote the location in case the path contains spaces. These quotes are stripped later anyway
        // replace path-references: ${path}$  and $'{path}'$
        HashMap<String,String> tobereplaced=new HashMap<String,String>();
        Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}\\$");  // look for arguments enclosed in ${...}$
        Pattern pattern2 = Pattern.compile("\\$'\\{(.+?)\\}'\\$"); // look for arguments enclosed in $'{...}'$
        
        for (int i=0;i<commandLines.length;i++) {
            commandLines[i]=resolveGlobalDirectoryReferences(commandLines[i],workdir);
            commandLines[i]=commandLines[i].replace("%PROGRAM", programLocation); //
            Matcher matcher=pattern.matcher(commandLines[i]);
            while (matcher.find()) { // look for arguments enclosed in ${...}$
                String path=convertFilePath(matcher.group(1));
                tobereplaced.put(matcher.group(0),path);
            }
            for (String key:tobereplaced.keySet()) {
                String replacement=tobereplaced.get(key);
                commandLines[i]=commandLines[i].replace(key, replacement);
            }
            tobereplaced.clear();

            matcher=pattern2.matcher(commandLines[i]);
            while (matcher.find()) { // look for arguments enclosed in $'{...}'$
                String path=quoteFilePathIfNecessary(matcher.group(1));
                tobereplaced.put(matcher.group(0),path);
            }
            for (String key:tobereplaced.keySet()) {
                String replacement=tobereplaced.get(key);
                commandLines[i]=commandLines[i].replace(key, replacement);
            }
        }
        // System.err.println(commandLine);
        // There might be several commands in one commandLine (separated by semicolons or other strings).
        // Execute each command in turn
        String splitstring=(commandseparator!=null)?commandseparator:";";
        if (commandLines.length==1) { // if there is just a single command line, try to split it on the separator to see if there are actually multiple commands on one single line        
            commandLines=commandLines[0].split(splitstring);
            unresolvedRediretionsCommandLines=unresolvedRediretionsCommandLines[0].split(splitstring);
        } 
        
        Object[] redirarg=getRedirectedArgumentNames(unresolvedRediretionsCommandLines); // this method needs the original argument field codes in braces
        String[] STDIN_arguments=(String[])redirarg[0]; // the lengths of these arrays equal the number of commands to be executed in succession
        String[] STDOUT_arguments=(String[])redirarg[1]; // and if the String[x]!=null it means that the given argument should be used as STDIN/OUT
        //for (int i=0;i<STDIN_arguments.length;i++) {System.err.println("Command["+i+"]  STDIN="+STDIN_arguments[i]+", STDOUT="+STDOUT_arguments[i]);}
        
        for (int commandNumber=0;commandNumber<commandLines.length;commandNumber++) { // for each command
           String currentCommandline=commandLines[commandNumber].trim();
           if (currentCommandline.isEmpty()) continue; // maybe it disappeared because it was conditional?
           ArrayList<String> argumentslist=MotifLabEngine.splitOnSpace(currentCommandline,true,false);
           //for (int i=0;i<argumentslist.size();i++) {System.err.println("["+commandNumber+"/"+commandLines.length+"]["+i+"]=>["+argumentslist.get(i)+"]");}
           engine.logMessage(currentCommandline);
           String STDIN_argument=STDIN_arguments[commandNumber];
           String STDOUT_argument=STDOUT_arguments[commandNumber];
           
           // -- Create a process and run it. Also create an OutputMonitor to catch STDOUT and STDERR activity
           ProcessBuilder pb = new ProcessBuilder(argumentslist);
           //for (int x=0;x<argumentslist.size();x++) System.err.println("ARG["+x+"] "+argumentslist.get(x));
           pb.redirectErrorStream(STDOUT_argument==null);
           File appdir=null;
           if (getApplicationDirectory()!=null) appdir=new File(getApplicationDirectory());
           pb.directory(appdir);         
           //pb.directory(new File(workdir)); // use the directory of this MotifLab session as work-dir for the process
           Process process = pb.start();

           OutputMonitor monitor=null;

           InputFeeder STDINfeeder=null;
           if (STDIN_argument!=null) {
               Object file=resolvedArguments.get(STDIN_argument);
               if (file==null || !(file instanceof File)) throw new ExecutionError("SYSTEM ERROR: Argument '"+STDIN_argument+"' redirected to STDIN has not been assigned a file");
               else STDINfeeder=new InputFeeder(task, process, Thread.currentThread(), (File)file);
               //System.err.println("STDIN ["+STDIN_argument+"] => "+((File)file).getAbsolutePath());
           }
           OutputMonitor STDOUTreceiver=null;
           if (STDOUT_argument!=null) {
               Object file=resolvedArguments.get(STDOUT_argument);
               if (file==null || !(file instanceof File)) throw new ExecutionError("SYSTEM ERROR: Argument '"+STDOUT_argument+"' redirected to STDOUT has not been assigned a file");
               else STDOUTreceiver=new OutputMonitor(task, process, Thread.currentThread(), (File)file, "STDOUT");
               //System.err.println("STDOUT ["+STDOUT_argument+"] => "+((File)file).getAbsolutePath());
               monitor=new OutputMonitor(task, process, Thread.currentThread(), null,"STDERR");
           } else monitor=new OutputMonitor(task, process, Thread.currentThread(),null, "MERGED");

           int flag=0;
           try {
               if (STDOUTreceiver!=null) STDOUTreceiver.start(); // this thread stores the STDOUT output in a file
               monitor.start();
               if (STDINfeeder!=null) STDINfeeder.start(); // start feeding data to the process' STDIN
               flag=process.waitFor();
               if (flag!=0) engine.logMessage(getName()+" finished with error code: "+flag);
               if (STDINfeeder!=null) try {STDINfeeder.join();} catch (InterruptedException ie) {}
               if (STDOUTreceiver!=null) try {STDOUTreceiver.join();} catch (InterruptedException ie) {}
               try {monitor.join();} catch (InterruptedException ie) {}
           } catch (Exception e) { // this could be an Interrupted exception or else
               // destroying the process causes closing of STDIN/STDOUT/STDERR streams
               // which will cause the auxilary threads to finish
               process.destroy();
               // Wait for the auxilary threads to finish. I am not sure if this is necessary, but just in case
               if (STDINfeeder!=null) try {STDINfeeder.join();} catch (InterruptedException ie) {}
               if (STDOUTreceiver!=null) try {STDOUTreceiver.join();} catch (InterruptedException ie) {}
               if (monitor!=null) try {monitor.join();} catch (InterruptedException ie) {}

               deleteFiles(temporaryfilenames,workdir);
               if (task.getStatus().equals(ExecutableTask.ERROR)) { // this will happen if a "report" expression has been recognized
                 throw new ExecutionError(task.getStatusMessage());  
               } else throw e;
           }
           Exception err=monitor.getError(); // Check if the monitor reported any errors that were not propagated in time?
           if (err!=null) { 
               process.destroy();
               // Wait for the auxilary threads to finish. I am not sure if this is necessary, but just in case
               if (STDINfeeder!=null) try {STDINfeeder.join();} catch (InterruptedException ie) {}
               if (STDOUTreceiver!=null) try {STDOUTreceiver.join();} catch (InterruptedException ie) {}
               if (monitor!=null) try {monitor.join();} catch (InterruptedException ie) {}
               deleteFiles(temporaryfilenames,workdir);
               throw err;              
           }
       } // end for each command 
      
       // -- parse result files and set results parameters in the task. Also delete temporary files
       for (int i=0;i<expectedResultsCount;i++) {
           Parameter resultsParameter=resultsParameterFormats.get(i);
           String resultParameterName=resultsParameter.getName();
           File resultFile=resultFiles[i];
           if (!resultFile.exists()) throw new ExecutionError("Results file is missing for '"+resultParameterName+"'");
           if (!resultFile.canRead()) throw new ExecutionError("Unable to read contents of results file: "+resultFile.getAbsolutePath());
           ArrayList<String> result=readFile(resultFile);
           DataFormat resultsFormat=getArgumentDataFormat(resultParameterName);
           if (resultsFormat==null) throw new ExecutionError("SYSTEM ERROR: Unspecified (or unrecognized) data format for "+getName()+" results ("+resultParameterName+")");
           ParameterSettings resultsFormatSettings=getArgumentDataFormatSettings(resultParameterName);
           resolveDataFormatLinks(this,resultParameterName,resultsFormatSettings,task);
           Class resultsDataType=resultsParameter.getType();
           Data resultsData=engine.createDataObject(resultsDataType,resultParameterName);
           resultsFormat.parseInput(result, resultsData, resultsFormatSettings, task);
           task.setParameter(resultParameterName, resultsData);
       }
       task.setStatus(ExecutableTask.RUNNING);   
       deleteFiles(temporaryfilenames,workdir);
}


/**
 * This method returns an array with 2 elements both of which are String arrays (String[])
 * The two String[] arrays contains names of arguments that are linked to redirection wrt
 * STDIN or STOUD (the first array is for STDIN the second for STDOUT). The length of the
 * String[] arrays is the same as the number of commands that should be executed in succession
 * when this external program is run. E.g. lets say two commands should be run and the values
 * of the arrays are STDIN[0]==arg1, STDIN[1]==null, STDOUT[0]==null, STDOUT[1]==arg2.
 * This means that for the first command there is an argument (arg1) which should be feed to the
 * process through STDIN but no data needs to be read back from STDOUT. For the second command
 * no data needs to be feed to STDIN but this command produces output to STDOUT which must be
 * read and associated with the argument 'arg2'.
 *
 * @param commandLines
 * @return
 */
private Object[] getRedirectedArgumentNames(String[] commandLines) throws ExecutionError {    
    String[] STDIN = new String[commandLines.length];
    String[] STDOUT = new String[commandLines.length];
    Pattern pattern = Pattern.compile("\\{(.+?)\\}"); 
    for (int i=0;i<commandLines.length;i++) { // look for parameter names in the command line
        String subcommand=commandLines[i];
        Matcher matcher = pattern.matcher(subcommand);
        while (matcher.find()) {
            String argumentname=matcher.group(1);
            if (!redirectedArguments.containsKey(argumentname)) continue; // check if this parameter should be redirected
            String destination=redirectedArguments.get(argumentname);
            //System.err.println("Redirecting '"+argumentname+"' to "+destination+" for command="+i);
            if (destination.equalsIgnoreCase("STDIN")) {
               if (STDIN[i]!=null) throw new ExecutionError("Only one argument can be redirected to STDIN for each command");
               else STDIN[i]=argumentname;
            } else if (destination.equalsIgnoreCase("STDOUT")) {
                if (STDOUT[i]!=null) throw new ExecutionError("Only one argument can be redirected to STDOUT for each command");
                else STDOUT[i]=argumentname;
            } else throw new ExecutionError("Unknown redirection destination '"+destination+"' (Allowed values are STDIN and STDOUT)");
        }
    }
    return new Object[]{STDIN,STDOUT};
}


/** Given an argument name and the resolved value of this argument (which could be null),
 *  this method returns a string that could be inserted in a command line to represent this argument.
 *  Depending on the argument type, the returned string could be a switch followed by a value,
 *  just the switch (for Boolean flags), just the value or simply and empty string.
 */
private String getCommandStringForParameter(String argumentname, Object value) {
   if (value==null || value.toString().isEmpty()) return "";
   Parameter thisParameter=getParameterWithName(argumentname);
   if (thisParameter!=null && shouldSkipIfDefault(argumentname)) { // check if this parameter has default value
       Object defaultValue=thisParameter.getDefaultValue();
       if ((value==null && defaultValue==null) || (value!=null && defaultValue!=null && value.equals(defaultValue))) return ""; // do not output this default value
   }
   if (isLinkedToRedirectionArgument(argumentname)) return ""; // redirection argument should not be written to command line. They are implicit!
   String argumentswitch=getArgumentSwitch(argumentname);   
   String cmdString=(argumentswitch!=null)?argumentswitch+getArgumentSwitchSeparator(argumentname):"";
   if (value instanceof File) cmdString+=convertFilePath(((File)value).getAbsolutePath());   
   else if (!(value instanceof Boolean)) cmdString+=(value.toString()); // probably String or Number
   else if (value instanceof Boolean && ((Boolean)value).booleanValue()==false) cmdString=""; // do not output anything for boolean flags that are false
   return cmdString;
}


private void deleteFiles(HashSet<String> files, String workdir) {
   for (String filename:files) {
       filename=resolveGlobalDirectoryReferences(filename, workdir); 
       File tempfile=new File(filename);          
       if (!engine.deleteTempFile(tempfile)) engine.logMessage("WARNING: Unable to delete temporary file (or directory): "+tempfile.getAbsolutePath());
   }    
} 

private File resolveImplicitFileForArgument(String argument, String workdir) {
    String impArg=getImplicitArgument(argument);
    String resolvedArg=resolveGlobalDirectoryReferences(impArg,workdir);
    if (impArg.equals(resolvedArg)) return new File(workdir,impArg); // the reference is local, so put it under temp-dir
    else return new File(resolvedArg);
}

private File resolveExplicitFileForArgument(String argument, String workdir) {
    String expArg=getExplicitArgument(argument);
    String resolvedArg=resolveGlobalDirectoryReferences(expArg,workdir);
    if (expArg.equals(resolvedArg)) return new File(workdir,expArg); // the reference is local, so put it under temp-dir
    else return new File(resolvedArg);
}

private String resolveGlobalDirectoryReferences(String command, String workdir) {
    command=command.replace("%WORKDIR/", workdir+File.separator); // 
    command=command.replace("%WORKDIR\\", workdir+File.separator); // 
    command=command.replace("%WORKDIR", workdir); // 
    command=command.replace("%APPDIR/", getApplicationDirectory()+File.separator); // 
    command=command.replace("%APPDIR\\", getApplicationDirectory()+File.separator); //    
    command=command.replace("%APPDIR", getApplicationDirectory()); //    
    return command;
}

/** 
 * Does extra processing of a filename path 
 * Converts pathnames to 'Cygwin format' if the program is run under cygwin 
 * and adds quotes around the filename if necessary
 */
private String convertFilePath(String pathname) {
   if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
       if (isRunInCygwin()) {
           if (pathname.length()>=3 && pathname.charAt(1)==':' && pathname.charAt(2)=='\\') {
               char driveLetter=Character.toLowerCase(pathname.charAt(0));
               pathname="/cygdrive/"+driveLetter+"/"+pathname.substring(3);
           }   
           pathname=pathname.replace("\\", "/");
       } 
   } 
   //if (System.getProperty("os.name").startsWith("Windows") && pathname.contains(" ")) pathname="\""+pathname+"\"";
   if (!(quotepath.equalsIgnoreCase("no") || quotepath.equalsIgnoreCase("off"))) {
      if (pathname.contains(" ")) pathname="\""+pathname+"\"";
   }
   return pathname;
}
/**
 * Adds quotes around a filename (path) if the OS is Windows and the pathname contains spaces
 */
private String quoteFilePathIfNecessary(String pathname) {
   if (quotepath.equalsIgnoreCase("no") || quotepath.equalsIgnoreCase("off")) return pathname; // config has overruled quoting
   if (pathname.contains(" ")) pathname="\""+pathname+"\"";
   return pathname;
}




private void outputSourceSequences(FeatureDataset sourceData, DataFormat outputFormat, ParameterSettings outputSettings, OperationTask task, File sequencesFile) throws Exception {
    OutputData sequenceOutput=new OutputData(sourceData+"_sourceTemp");
    task.checkExecutionLock(); // checks to see if this task should suspend execution
    if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
    try {
         outputFormat.format(sourceData, sequenceOutput, outputSettings, task);
   } catch (Exception e) {throw e;}
    sequenceOutput.saveToFile(sequencesFile.getAbsolutePath(),engine);  
}
    


/** Creates a temporary file holding the data for the given argument 
 *  The argument data is output in a format (with settings) as specified in the OperationTask object
 */        
private File createTempFile(ExternalProgram program, String argument, String workdir, OperationTask task) throws Exception {
    if (!dataFormatNames.containsKey(argument)) return null; // this parameter is apparently not used directly, so we need not write it to file
    DataFormat dataformat=program.getArgumentDataFormat(argument);
    if (dataformat==null) throw new ExecutionError("SYSTEM ERROR: Unspecified (or unrecognized) data format for "+program.getName()+" argument '"+argument+"' => "+dataFormatNames.get(argument));  
    File outputfile=engine.createTempFile(workdir);
    if (outputfile==null) throw new ExecutionError("Unable to create necessary temporary file");
    ParameterSettings formatsettings=program.getArgumentDataFormatSettings(argument);
    resolveDataFormatLinks(program,argument,formatsettings,task);
    OutputData output=new OutputData(argument+"Temp");
    Data dataobject=null;
    if (isSoftLink(argument)) {
       String linkRefersTo=getParameterReferenceLink(argument, null);
       dataobject=(Data)task.getParameter(linkRefersTo);
    } else dataobject=(Data)task.getParameter(argument); // the parameter must be a Data object since it is "complex" (otherwise createTempFile would not have been called)
    dataformat.format(dataobject, output, formatsettings, task);
    output.saveToFile(outputfile.getAbsolutePath(),engine);
    return outputfile;
}
    
/**
 * Reads the specified file and returns its contents as an ArrayList
 * with one entry for each line of text
 */
private ArrayList<String> readFile(File file) throws Exception {
    if (file==null) return new ArrayList<String>(); // empty file
    BufferedReader inputStream = null;
    ArrayList<String> result=new ArrayList<String>();
    try {
        inputStream =  new BufferedReader(new FileReader(file));
        String l;
        while ((l = inputStream.readLine()) != null) {
            result.add(l);
        }
    } catch (Exception e) {
        throw e;
    } finally {
        if (inputStream != null) {try {inputStream.close();} catch (Exception c) {}}
    }
    return result;
}    
    
private boolean isComplexType(Object object) {
    if (object instanceof FeatureDataset) return true;
    if (object instanceof FeatureSequenceData) return true;
    if (object instanceof DataCollection) return true;
    if (object instanceof DataPartition) return true;    
    if (object instanceof Motif) return true;
    if (object instanceof Module) return true;
    if (object instanceof TextVariable) return true;
    if (object instanceof Sequence) return true;
    if (object instanceof BackgroundModel) return true;
    if (object instanceof NumericMap) return true;
    if (object instanceof TextMap) return true;    
    if (object instanceof ExpressionProfile) return true;   
    return false;
}
    
  
/**
 * This class monitors activity on STDOUT and/or STDERR made by the external process
 * and conveys this information to the task as a status message
 * @param outputfile If this is not NULL the data read will be written to this file 
 * @param type should be STDOUT, STDERR or MERGED
 *                   
*/
private class OutputMonitor extends Thread {
        ExecutableTask task;
        Process process;
        Thread taskThread;
        File outputfile=null;
        String type;
        BufferedReader input=null;
        BufferedWriter output=null;
        Exception fatalError=null;

        public OutputMonitor(ExecutableTask task, Process process, Thread taskThread, File outputfile, String type) {
            this.task=task;
            this.process=process;
            this.taskThread=taskThread;
            this.outputfile=outputfile;
            this.type=type;
        }
   
        @Override
        public void run() {
            String line;
            try {
               if (type.equalsIgnoreCase("STDERR")) input=new BufferedReader(new InputStreamReader(process.getErrorStream())); //
               else input=new BufferedReader(new InputStreamReader(process.getInputStream())); // STDERR and STDOUT may have been combined into one stream (getInputStream)

               if (outputfile!=null) output = new BufferedWriter( new OutputStreamWriter(new BufferedOutputStream( new FileOutputStream(outputfile))));
               //System.err.println("Monitor running");
               while ((line = input.readLine()) != null) {
                   // engine.logMessage("[Monitor] "+line); // for debugging only!!
                   if (outputfile!=null) {
                       output.write(line);
                       output.newLine();
                   } else {
                       if (reportExpressions!=null) {
                          checkAndReport(line);
                       }
                       else task.setStatusMessage(line);
                   }
                   // System.err.println(line);
                   task.checkExecutionLock(); // checks to see if this task should suspend execution
                   if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) {taskThread.interrupt();break;}
               }
               input.close();
               if (output!=null) {
                   output.flush();
                   output.close();
               }
               //System.err.println("Monitor finished");
            }
            catch (IOException ioe) {engine.errorMessage("FILE ERROR: "+ioe.getMessage(), 0);}
            catch (ExecutionError e) {
               task.setStatus(ExecutableTask.ERROR);
               task.setStatusMessage(e.getMessage());
               taskThread.interrupt();
            }
            catch (Exception e) {}
            finally {
                try {
                    if (input!=null) input.close();
                    if (output!=null) {output.flush();output.close();}
                } catch (Exception ie) {}
            }
        }

        /**
         * Goes through all registered "report" rules and compares them to the current line. If the line matches a report rule it will trigger a report action
         * @param line
         * @throws ExecutionError 
         */
        private void checkAndReport(String line) throws ExecutionError {
            for (Report report:reportExpressions) {
                String reportString=report.getReport(line); // getReport(line) will compare the report's expression against the line and return a report string if matched or NULL if the expression does not match the line
                if (reportString!=null) {
                    short target=report.getTarget();
                    if (target==Report.DESTINATION_STATUS) task.setStatusMessage(reportString);
                    else if (target==Report.DESTINATION_LOG) engine.logMessage(reportString);
                    else if (target==Report.DESTINATION_ERROR) {
                        ExecutionError err=new ExecutionError(reportString);
                        fatalError=err;
                        throw err; // this might not be caught in time, but the main process will check if fatalError is set!
                    }
                    else if (target==Report.DESTINATION_PROGRESS) {
                        try {
                           int progress=Integer.parseInt(reportString);
                           if (progress>=0 && progress<=100) task.setProgress(progress);
                        } catch (NumberFormatException e) {}                       
                    } else if (target==Report.DESTINATION_IGNORE) {
                        return; // don't check any more rules for this line
                    }                    
                }
            }
        }
                   
        public Exception getError() {
            return fatalError;
        }
        
} // end OutputMonitor

/**
 * This class can be used to feed the contents of a file to the STDIN of the given process
 */
private class InputFeeder extends Thread {
        ExecutableTask task;
        Process process;
        Thread taskThread;
        File file;
        BufferedWriter output=null;
        BufferedReader input = null;

        public InputFeeder(ExecutableTask task, Process process, Thread taskThread, File file) {
            this.task=task;
            this.process=process;
            this.taskThread=taskThread;
            this.file=file;
        }


        @Override
        public void run() {
            String line;
            try {
              output = new BufferedWriter( new OutputStreamWriter( process.getOutputStream()));
              input=new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(file))));
              while ((line = input.readLine()) != null) {
                    output.write(line);
                    output.newLine();
                    if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) {taskThread.interrupt();break;}
              }
              input.close();
              output.close();
            }
            catch (IOException ioe) {engine.errorMessage("ERROR while feeding data to process STDIN: "+ioe.getMessage(),0);}
            catch (Exception e) {}
            finally {
                try {
                   if (input!=null) input.close();
                   if (output!=null) output.close();
                } catch (Exception ie) {}
            }
        }

} // end InputFeeder


    public String getProgramDocumentation() {
        return getProgramDocumentation(true);
    }
    
    /**
     * Returns a HTML document describing this program
     * @return
     */
    public String getProgramDocumentation(boolean readdocumentationFromFile) {
        // Since the documentation of the program can be large (and not always needed) the documentation
        // is usually not included in the ExternalProgram object on initialization.
        // Hence the need to initialize a new copy of the program where the documentation has been included
        ExternalProgram programcopy=this;
        if (readdocumentationFromFile) {
            String externaldir = engine.getMotifLabDirectory()+java.io.File.separator+"external"+java.io.File.separator;
            
            File externalprogramfile=new File(externaldir+programcopy.getName()+".xml"); // try lowercase suffix first
            if (!externalprogramfile.exists()) externalprogramfile=new File(externaldir+programcopy.getName()+".XML"); // then try uppercase suffix
            if (externalprogramfile.exists()) {
                try {
                   programcopy=ExternalProgram.initializeExternalProgramFromFile(externalprogramfile,true);
                } catch (SystemError e) {}
            }
        }
        StringBuilder document=new StringBuilder();
        StringBuilder authors=new StringBuilder();
        Object authorsObject=programcopy.getProperty("author");
        if (authorsObject instanceof ArrayList) {
            int i=((ArrayList)authorsObject).size();
            for (Object obj:(ArrayList)authorsObject) {
                authors.append(obj.toString());
                i--;
                if (i>0) authors.append(", ");
            }
        } else if (authorsObject!=null)  authors.append(authorsObject.toString());
        
        StringBuilder contact=new StringBuilder();
        Object contactObject=programcopy.getProperty("contact");
        if (contactObject instanceof ArrayList) {
            int i=((ArrayList)contactObject).size();
            for (Object obj:(ArrayList)contactObject) {
                contact.append(obj.toString());
                i--;
                if (i>0) contact.append(", ");
            }
        } else if (contactObject!=null) contact.append(contactObject.toString());
        
        StringBuilder citations=new StringBuilder();
        Object citationsObject=programcopy.getProperty("citation");
        if (citationsObject instanceof ArrayList) {
            int i=((ArrayList)citationsObject).size();
            for (Object obj:(ArrayList)citationsObject) {
                citations.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                citations.append(obj.toString());
                i--;
                if (i>0) citations.append("<br><br>");
            }
        } else if (citationsObject!=null) citations.append(citationsObject.toString());
        String programClass=programcopy.getProgramClass();
             if (programClass.equals("MotifDiscovery")) programClass="Motif discovery (<i>de novo</i>)";
        else if (programClass.equals("MotifScanning")) programClass="Motif scanning";
        else if (programClass.equals("ModuleDiscovery")) programClass="Module discovery";
        else if (programClass.equals("ModuleScanning")) programClass="Module scanning";
        else if (programClass.equals("EnsemblePrediction")) programClass="Ensemble prediction method";
        String service=programcopy.getServiceType();
        if (service.equals("local")) service="Locally installed program";
        else if (service.equals("soap")) service="Web Service";     
               
        Object homepage=programcopy.getProperty("homepage");
        if (programcopy.getLocation()!=null && !(service.equals("bundled") || service.equals("plugin"))) {
            String locationlink=programcopy.getLocation();
            if (service.equals("Locally installed program")) locationlink="<a href=\"file:///"+locationlink+"\">"+locationlink+"</a>";
            service=service+"<br><nobr>"+locationlink+"</nobr>";
        }
        document.append("<html><body><h1>");
        document.append(programcopy.getName());
        document.append("</h1><br>");
        document.append("<table border=0><tr><td><b>Class: </b></td><td>");
        document.append(programClass);
        document.append("</td></tr>");
        document.append("<tr><td valign=top><b>Service: </b></td><td valign=top>");
        document.append(service);
        document.append("</td></tr>");
        if (authors.length()>0) {
            document.append("<tr><td valign=top><b>Authors: </b></td><td valign=top>");
            document.append(authors);
            document.append("</td></tr>");
        }
        if (contact!=null && contact.length()>0) {
            document.append("<tr><td valign=top><b>Contact: </b></td><td valign=top>");
            document.append(contact);
            document.append("</td></tr>");
        }
        if (homepage!=null) {
            document.append("<tr><td valign=top><b>Homepage: </b></td><td valign=top><nobr><a href=\"");
            document.append(homepage);
            document.append("\">");
            document.append(homepage);
            document.append("</a></nobr></td></tr>");
        }
        document.append("</table>");
        String description=programcopy.getDescription();
        if (description!=null && !description.isEmpty()) {
            document.append("<br><hr><br>");
            document.append(description);
            document.append("<br><br>");
        }
        if (citations.length()>0) {
           document.append("<hr><br><b>Citations: </b><br><br>If you use ");
           document.append(programcopy.getName());
           document.append(" in your own research, please cite one of the following publications:<br><br>");
           document.append(citations);
           document.append("<br>");
        }
        String license=(String)programcopy.getProperty("license");
        if (license!=null && !license.trim().isEmpty()) {
            document.append("<br><hr><br>");
            document.append(license);
        }
        document.append("<br><br></body></html>");
        return document.toString();
    }
    

    /** This little class is a small struct to bundle "report" declarations for the program.
        These are expressions that should be monitored from the programs STDOUT and STDERR and
        reported to the user either as status messages, log messages or fatal errors.
     */   
    private static class Report {          
        public static short DESTINATION_STATUS=0;
        public static short DESTINATION_LOG=1;
        public static short DESTINATION_ERROR=2;
        public static short DESTINATION_PROGRESS=3;   
        public static short DESTINATION_IGNORE=4;          
        private static String[] destinationStrings=new String[]{"status","log","error","progress","ignore"}; // corresponding to the destination values above
        
        String pattern=null;
        short target=DESTINATION_STATUS;
        String output=null;
//        boolean hasbackRef=false;
//        Pattern expPattern=null;
        
        public Report(String expression, short target, String output) {           
           if (expression!=null && !expression.isEmpty()) this.pattern=expression;
           if (target>=0 && target<destinationStrings.length) this.target=target;
           if (output!=null && !output.isEmpty()) this.output=output;
           //checkBackReferences();
        }
        
        public Report(String expression, String targetexpression, String output) {           
           if (targetexpression!=null) {
               for (short i=0;i<destinationStrings.length;i++) {
                   if (destinationStrings[i].equalsIgnoreCase(targetexpression)) {target=i;break;}
               }
           }
           if (expression!=null && !expression.isEmpty()) this.pattern=expression;
           if (output!=null && !output.isEmpty()) this.output=output;
           //checkBackReferences();
        }        
        
        public short getTarget() {           
            return target;
        }          
        
        public String getExpressionAsString() {
            return (pattern==null)?"":pattern;
        }
        
        public String getTargetAsString() {           
            return destinationStrings[target];
        }  
        
        public String getOutputAsString() {           
            return (output==null)?"":output;
        }        
        
        public boolean matches(String string) {
            if (pattern==null) return true; // empty expression is code for "wildcard"
            else return string.matches(pattern);
        }
        
        /** Returns the string that should be reported when the given input 
         *  is encountered. If the input does not trigger a report, NULL will be returned
         */
        public String getReport(String inputstring) {
            if (target==DESTINATION_PROGRESS && pattern!=null) {
                Pattern expr=Pattern.compile(pattern);
                Matcher matcher=expr.matcher(inputstring);
                boolean isMatch=matcher.matches();
                if (isMatch && output!=null) return output; // the progress is set explicitly
                else if (isMatch && matcher.groupCount()==1) { // progress is a single number which should be in the range [0,100]
                    return matcher.group(1);
                }
                else if (isMatch && matcher.groupCount()==2) { // progress is given as a ratio between two integer number (e.g. " processing sequence 3 of 28"). 
                    try {
                       double first=Integer.parseInt(matcher.group(1));
                       double second=Integer.parseInt(matcher.group(2));
                       int percentage=(int)((first/second)*100.0); // convert ratio to single percentage in the range [0,100] (hopefully)
                       if (percentage<0) percentage=0;
                       if (percentage>100) percentage=100;
                       return ""+percentage; // return value as string (it will be reparsed later)
                    } catch (NumberFormatException e) {
                        return "error"; // this will just be ignored later when progress is set
                    }                    
                }                
            }
            if (pattern==null || inputstring.matches(pattern)) {             
                if (output!=null) return output;
                else return inputstring;            
            } else return null;
        }
        
        @Override
        public String toString() {
            return "REPORT["+getTargetAsString()+"] expression='"+getExpressionAsString()+"', output='"+getOutputAsString()+"'";
        }
        
        // checks if the output string refers to capturing groups in the expression using $n syntax
//        private void checkBackReferences() {
//            hasbackRef=(output!=null && output.matches("\\$(\\d+)"));
//            if (hasbackRef && pattern!=null) expPattern=Pattern.compile(pattern);
//        }
    }
}    

