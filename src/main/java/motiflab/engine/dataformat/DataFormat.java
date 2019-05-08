package motiflab.engine.dataformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterExporter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.DataSegment;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.ParameterCondition;
import motiflab.engine.ProgressListener;
import motiflab.engine.SystemError;

/**
 *
 * @author kjetikl
 */
public abstract class DataFormat implements ParameterExporter, Comparable {
    protected ArrayList<Parameter> parameterFormats=new ArrayList<Parameter>(); 
    protected MotifLabEngine engine=null;
    protected ArrayList<ProgressListener> progressListeners=new ArrayList<ProgressListener>();
    private int progress=0;
    private static HashMap<Integer,DecimalFormat> decimalformatters=new HashMap<Integer,DecimalFormat>();
    
    /**
     * Returns a name for this output format
     */
    public abstract String getName(); 
    
    /**
     * Returns an URL referring to a 'help page' document for this data format
     * or a String containing the 'help page' document itself.
     * The help-page describes the purpose of the data format and its various parameters
     * @return
     */

    public Object getHelp(MotifLabEngine engine) {
        if (engine!=null) {
            try {
               String safename=getName().replace(" ", "_");
               return new java.net.URL(engine.getWebSiteURL()+"getHelp.php?type=DataFormat&topic="+safename);
            } catch (Exception e) {}            
        }
        return null;
    }     
    
    /**
     * Sets a reference to the engine for this DataFormat
     */
    public void setEngine(MotifLabEngine engine) {this.engine=engine;} 
        
    /**
     * This method returns true if the DataFormat object can format output for Data objects
     * of the given type in the objects format. 
     */
    public abstract boolean canFormatOutput(Data data);

    /**
     * This method returns true if the DataFormat object can format output Data objects
     * of the given class in the objects format. 
     */
    public abstract boolean canFormatOutput(Class dataclass);
    
    
    /**
     * This method returns true if the DataFormat object can parse input for Data objects
     * of the given type in the objects format. 
     */
    public abstract boolean canParseInput(Data data);

    /**
     * This method returns true if the DataFormat object can parse input for Data objects
     * of the given class in the objects format. 
     */
    public abstract boolean canParseInput(Class dataclass);
    
    
    
    
    /**
     * This method returns a list containing the Data classes that this output
     * format supports  
     */
    public abstract Class[] getSupportedDataTypes();
    
    /**
     * This method returns the default file-suffix to use for the DataFormat
     */
    public abstract String getSuffix();

    /**
     * This method returns a list of alternative suffixes that are common for this data format.
     * The default implementation just returns a list containing the default suffix, but this
     * can be overridden by subclasses that want to return more suffixes. 
     * Note that the default suffix should also be included in the returned list.
     */
    public String[] getSuffixAlternatives() {return new String[]{getSuffix()};}


    /** This method should return TRUE (default) if output in this format can be appended
     *  to an already existing non-empty output object and FALSE if a new object must be
     *  used as target
     */
    public boolean isAppendable() {
        return true;
    }


    /**
     * This method takes a dataobject as input and outputs its content according to the output format 
     * @param dataobject The input data object which is to be formatted for output
     * @param outputobject An OutputData object to use as destination for the output
     * @param settings An object specifying additional output format parameters
     * @param task An ExecutableTask which can be prompted for interruption or status. Note that this can be NULL
     * @return An OutputData object containing the formatted output of the input Data
     * @throws ExecutionError
     * @throws InterruptedException
     */
    public abstract OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException;
   
               
    /**
     * Adds a parameter to the DataFormat. This is used for initialization of new DataFormats and should only be called in a constructor or similar setup method
     */
    protected void addParameter(String parameterName, Object defaultValue, Object[] allowedValues, String description) {
        Class typeclass=null;
        if (defaultValue==null) {
           if (allowedValues==null || allowedValues.length==0) throw new NullPointerException("allowedValues[] must not be empty if default parameter is null");
           Object first=allowedValues[0];
           if (first instanceof Class) typeclass=(Class)first;
           else typeclass=first.getClass();
        } else typeclass=defaultValue.getClass();
        parameterFormats.add(new Parameter(parameterName,typeclass,defaultValue,allowedValues,description));
    }
    
    /**
     * Adds a parameter to the DataFormat. This is used for initialization of new DataFormats and should only be called in a constructor or similar setup method
     */
    protected void addParameter(String parameterName, Object defaultValue, Object[] allowedValues, String description, boolean required, boolean hidden) {
        Class typeclass=null;
        if (defaultValue==null) {
           if (allowedValues==null || allowedValues.length==0) throw new NullPointerException("allowedValues[] must not be empty if default parameter is null");
           Object first=allowedValues[0];
           if (first instanceof Class) typeclass=(Class)first;
           else typeclass=first.getClass();
        } else typeclass=defaultValue.getClass();
        parameterFormats.add(new Parameter(parameterName,typeclass,defaultValue,allowedValues,description,required,hidden));
    }
    
    /**
     * Adds a parameter to the DataFormat. This is used for initialization of new DataFormats and should only be called in a constructor or similar setup method
     */
    protected void addAdvancedParameter(String parameterName, Object defaultValue, Object[] allowedValues, String description) {
        Class typeclass=null;
        if (defaultValue==null) {
           if (allowedValues==null || allowedValues.length==0) throw new NullPointerException("allowedValues[] must not be empty if default parameter is null");
           Object first=allowedValues[0];
           if (first instanceof Class) typeclass=(Class)first;
           else typeclass=first.getClass();
        } else typeclass=defaultValue.getClass();
        parameterFormats.add(new Parameter(parameterName,typeclass,defaultValue,allowedValues,description,false,false,true));
    }    
        
    /**
     * Adds a parameter to the DataFormat. This is used for initialization of new DataFormats and should only be called in a constructor or similar setup method
     */
    protected void addOptionalParameter(String parameterName, Object defaultValue, Object[] allowedValues, String description) {
        Class typeclass=null;
        if (defaultValue==null) {
           if (allowedValues==null || allowedValues.length==0) throw new NullPointerException("allowedValues[] must not be empty if default parameter is null");
           Object first=allowedValues[0];
           if (first instanceof Class) typeclass=(Class)first; // class is decided by first class in list
           else typeclass=first.getClass();
        } else typeclass=defaultValue.getClass();
        parameterFormats.add(new Parameter(parameterName,typeclass,defaultValue,allowedValues,description,false,false));
    }
    
    /**
     * Adds the given condition to the corresponding parameter (the name of the parameter in the condition should correspond to a registered parameter) 
     * NOTE: If conditions are used that change some parameter values based on the values of others, the method "applyConditions()" in the ParameterSettings object 
     *       should be called in parse and format methods before the settings are used further in order to properly initialize all the settings.
     * @return TRUE if the condition was added to the parameter or FALSE if the parameter was not found
     */
    protected void addParameterCondition(ParameterCondition condition) throws SystemError {
        String parameterName=condition.getParameter();
        for (Parameter par:parameterFormats) {
            if (par.getName().equals(parameterName)) {
                par.addCondition(condition);
                return;
            }
        }
        throw new SystemError("No such parameter: "+parameterName);
    }
    
    @Override
    public String toString() {return getName();}
    
    /** 
     * Can be used to specify a filter-type for a parameter
     * @param parameterName
     * @param filterType This should be either "output" or "input" (or null)
     *                   to specify that the parameter is only applicable for 
     *                   output or input respectively (the default 'null' value
     *                   means the parameter is applicable to both output and input)
     */
    public void setParameterFilter(String parameterName, String filterType) {
        for (Parameter par:parameterFormats) {
            if (parameterName.equals(par.getName())) {par.setFilterType(filterType);break;}
        } 
    }    
    
    @Override
    public Object getDefaultValueForParameter(String parameterName) {
        for (Parameter par:parameterFormats) {
            if (parameterName.equals(par.getName())) return par.getDefaultValue();
        }
        return null; 
    }

    @Override
    public Parameter[] getParameters() {
        Parameter[] list=new Parameter[parameterFormats.size()];
        return parameterFormats.toArray(list);
    }
    
    @Override
    public Parameter getParameterWithName(String parameterName) {
        for (Parameter parameter:parameterFormats) {
            if (parameter.getName().equals(parameterName)) return parameter;
        }
        return null;
    }    
    
    /** Replaces the current output parameters with the new set (use this method with caution) */
    public void replaceParameters(Parameter[] parameters) {
        parameterFormats.clear();
        parameterFormats.addAll(Arrays.asList(parameters));
    }
    
    
     /** Returns an XML element for this DataFormat that can be included in XML-based configuration files */
    public org.w3c.dom.Element getXMLrepresentation(org.w3c.dom.Document document, ParameterSettings settings) {
        org.w3c.dom.Element element = document.createElement("dataformat");
        element.setAttribute("name", getName());
        if (settings!=null) {
            Parameter[] parameters=getParameters();
            for (Parameter parameter:parameterFormats) {
                if (settings.usesDefaultValue(parameter.getName(),parameters)) continue; // The default value is used for this setting so it is not necessary to include it in the output
                String value=settings.getParameterAsString(parameter.getName(), parameters);
                org.w3c.dom.Element setting = document.createElement("setting");
                setting.setAttribute("name", parameter.getName());
                setting.setAttribute("class", parameter.getType().getSimpleName());
                setting.setTextContent(value);
                element.appendChild(setting);
            }  
        }
        return element;
    }     
    /**
     * Parses input from a stream and returns a Data object of the appropriate type
     * @param input The source inputstream
     * @param target the Data object where the results should be stored
     * @param settings optional settings for the DataFormat
     * @param task Optional task object that can be used to send abort signals
     * @return
     */
    public abstract Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException;


    /**
     * Parses input from an InputStream. The default implementation in the superclass 
     * will simply read contents of the whole stream line by line and add each line
     * to an ArrayList before finally calling parseInput(ArrayList... ) instead.
     * can override this behavior.
     * 
     * @param input
     * @param target
     * @param settings
     * @param task
     * @return
     * @throws ParseError
     * @throws InterruptedException 
     */
    public Data parseInput(InputStream input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        BufferedReader inputStream=null;
        ArrayList<String> inputaslist=new ArrayList<String>();
        long lines=0;
        try {
            inputStream=new BufferedReader(new InputStreamReader(input));
            String line;
            while((line=inputStream.readLine())!=null) {
                lines++;
                if (lines%100==0 && (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED)))) throw new InterruptedException();
                inputaslist.add(line);
            }
        } catch (IOException e) { 
            throw new ParseError(e.getClass().getSimpleName()+":"+e.getMessage(), ((Long)lines).intValue());
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {}
        }
        return parseInput(inputaslist, target, settings, task);
    }

    /**
     * Parses textual input represented as a list of Strings and returns a DataSegment containing the relevant data
     * This method should only be used to retrieve partial segments of Feature data
     * @param input The text to be parsed
     * @param target the DataSegment object where the results should be stored
     * @param settings optional settings for the DataFormat
     * @param task Optional task object that can be used to send abort signals     * 
     * @return
     */
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("System Error: Inappropriate use of data format '"+getName()+"' to parse feature data");
    }

     /**
     * Parses input from a stream and returns a DataSegment containing the relevant data
     * This method should only be used to retrieve partial segments of Feature data
     * @param input The source inputstream
     * @param target the DataSegment object where the results should be stored
     * @param settings optional settings for the DataFormat
     * @param task Optional task object that can be used to send abort signals     *
     * @return
     */
    public DataSegment parseInput(InputStream input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        BufferedReader inputStream=null;
        ArrayList<String> inputaslist=new ArrayList<String>();
        long lines=0;
        try {
            inputStream=new BufferedReader(new InputStreamReader(input));
            String line;
            while((line=inputStream.readLine())!=null) {
                lines++;
                if (lines%100==0 && (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                inputaslist.add(line);
            }
        } catch (IOException e) {
            throw new ParseError(e.getClass().getSimpleName()+":"+e.getMessage(), ((Long)lines).intValue());
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {}
        }
        return parseInput(inputaslist, target, settings, task);
    }

    /**
     * This method should be overriden from applicable data formats and return TRUE
     * if the data format can only be used to parse data directly from local files
     * (not remove files on the web or even InputStreams). The default implementation
     * in the super class returns FALSE. A data format that returns TRUE would normally
     * throw exceptions if the methods parseInput(InputStream...) or parseInput(ArrayList<String>...)
     * are used. Instead, the methods parseInput(String filename,...) should be used instead.
     * @return 
     */
    public boolean canOnlyParseDirectlyFromLocalFile() {
        return false;
    }
    
    public DataSegment parseInput(String filename, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("System Error: Inappropriate use of data format '"+getName()+"' to parse feature data from file");
    }  
    
    public Data parseInput(String filename, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("System Error: Inappropriate use of data format '"+getName()+"' to parse feature data from file");
    }      
    
    /** Adds a progressListener to this DataFormat object */
    public void addProgressListener(ProgressListener listener) {
        progressListeners.add(listener);
    }
    
    /** Remove a progressListener from this DataFormat object */
    public void removeProgressListener(ProgressListener listener) {
        progressListeners.remove(listener);
    }
    
    /** 
     * Sets the progress of this DataFormat 
     * Be careful to synchronize properly if using DataFormats as singletons
     */
    public void setProgress(int i) {
        if (i>100) progress=100;
        else if (i<0) progress=0;
        else progress=i;
        for (ProgressListener listener:progressListeners) listener.processProgressEvent(this, progress);
    }   
    
    /** 
     * Sets the progress value for this operation according to the relative number
     * of "subtasks" that have been completed so far
     * Be careful to synchronize properly if using DataFormats as singletons
     * 
     * @param i The number of subtasks currently completed
     * @param n The total number of subtasks that must be completed
     */
    public void setProgress(int i, int n) {
        int newprogress;
        if (i>n) newprogress=100;
        else if (i<0) newprogress=0;
        else newprogress=(int)((i*1.0/n)*100);
        setProgress(newprogress);
    }
    
    /** Returns a progress value from this DataFormat */
    public int getProgress() {
        return progress;
    }


    /** Returns a string with a fixed size of 'length' where the number has been formatted
     *  right-aligned in the string (by padding with spaces). 
     *  Note, however, that the returned string can be longer than the specified 
     *  length if the number does not fit within the given length
     */
    public String formatFixedWidthDouble(double value, int length, int decimaldigits) {
        String numberString=getDecimalFormatter(decimaldigits).format(value);
        if (numberString.length()>=length) return numberString;
        StringBuilder builder=new StringBuilder(length);
        for (int i=0;i<length-numberString.length();i++) builder.append(' ');
        builder.append(numberString);
        return builder.toString();
    }

    /** Returns a string with a fixed size of 'length' where the number has been formatted
     *  right-aligned in the string (by padding with spaced). 
     *  Note, however, that the returned string can be longer than the specified 
     *  length if the number does not fit within the given length
     */
    public String formatFixedWidthInteger(int value, int length) {
        String numberString=""+value;
        if (numberString.length()>=length) return numberString;
        StringBuilder builder=new StringBuilder(length);
        for (int i=0;i<length-numberString.length();i++) builder.append(' ');
        builder.append(numberString);
        return builder.toString();
    }

    /** Returns a DecimalFormat that always uses the specified number of decimaldigits */
    public static DecimalFormat getDecimalFormatter(int decimaldigits) {
        DecimalFormat formatter=decimalformatters.get(decimaldigits);
        if (formatter!=null) return formatter;
        String dec=(decimaldigits>0)?".":"";
        for (int i=0;i<decimaldigits;i++) dec+="0";
        formatter=new DecimalFormat("0"+dec);
        decimalformatters.put(decimaldigits,formatter);
        return formatter;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof DataFormat) {
            String name=getName();
            String other=((DataFormat)o).getName();
            return name.compareTo(other);
        } else return -1;
    }
    
    /**
     * This method will check if the given sequence name contains illegal characters
     * and return a corrected name if the "autoCorrectSequenceNames" setting is turned ON
     * in the engine. If a sequence is renamed, a log-message will be output stating this fact
     * unless the 'silent' mode flag is set.
     * @param sequencename
     * @return 
     */
    public String convertIllegalSequenceNamesIfNecessary(String sequencename, boolean silent) {
        String fail=engine.checkSequenceNameValidity(sequencename, false);
        if (fail!=null && engine.autoCorrectSequenceNames()) { // sequencename contains illegal characters that should be corrected  
            String newsequencename=MotifLabEngine.convertToLegalSequenceName(sequencename);
            if (!silent) engine.logMessage("NOTE: sequence '"+sequencename+"' was renamed to '"+newsequencename+"'");
            sequencename=newsequencename;                                 
        }   
        return sequencename;
    }
    
}
