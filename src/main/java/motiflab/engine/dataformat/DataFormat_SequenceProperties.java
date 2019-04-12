/*


 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.DataMap;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.data.SequenceTextMap;

/**
 *
 * @author kjetikl
 */
public class DataFormat_SequenceProperties extends DataFormat {
    private String name="Sequence_Properties";
    public static final String TAB="TAB";
    public static final String SEMICOLON="Semicolon";
    public static final String COMMA="Comma";
    public static final String COLON="Colon";    
    public static final String PIPE="Vertical bar";    
    private Class[] supportedTypes=new Class[]{SequenceCollection.class, Sequence.class};


    public DataFormat_SequenceProperties() {
        String[] standard=Sequence.getAllStandardProperties(engine);
        StringBuilder builder=new StringBuilder();
        builder.append("<html>An ordered, comma-separated list of properties to output for each sequence.<br>Note that standard properties are case-insensitive,<br>but user-defined properties are case-sensitive!<br>");
        builder.append("<br>Standard sequence properties include:<br><br>");
        for (String prop:standard) {builder.append(prop);builder.append("<br>");}
        builder.append("</html>");
        addParameter("Format", "name, gene name", null,builder.toString(),true,false);
        addOptionalParameter("Separator",TAB, new String[]{TAB,SEMICOLON,COMMA,COLON,PIPE},"The character used to separate properties in the output (replaces commas in the format-string)");
        addOptionalParameter("List-separator",COMMA, new String[]{TAB,SEMICOLON,COMMA,COLON,PIPE},"The character used to separate elements within properties");
        addOptionalParameter("Header", Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Add a header (starting with #) in the first line which tells which fields are included");
        addOptionalParameter("Sort by", null, new Class[]{SequenceNumericMap.class},"If specified, the sequences will be sorted according to ascending value in this map");
        setParameterFilter("Header","output"); 
        setParameterFilter("Sort by","output"); 
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof SequenceCollection || data instanceof Sequence);
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(SequenceCollection.class) || dataclass.equals(Sequence.class));
    }

    @Override
    public boolean canParseInput(Data data) {
        return false;
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return false;
    }


    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "tsv";
    }


    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(5);
        String separator=TAB;
        String listseparator=COMMA;
        boolean header=false;
        String format="";
        SequenceNumericMap sortOrder=null;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             format=(String)settings.getResolvedParameter("Format",defaults,engine);
             separator=(String)settings.getResolvedParameter("Separator",defaults,engine);
             listseparator=(String)settings.getResolvedParameter("List-separator",defaults,engine);
             header=(Boolean)settings.getResolvedParameter("Header",defaults,engine);
             sortOrder=(SequenceNumericMap)settings.getResolvedParameter("Sort by",defaults,engine);               
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
             format=(String)getDefaultValueForParameter("Format");
             separator=(String)getDefaultValueForParameter("Separator");
             listseparator=(String)getDefaultValueForParameter("List-separator");             
             header=(Boolean)getDefaultValueForParameter("Header");
             sortOrder=(SequenceNumericMap)getDefaultValueForParameter("Sort by");             
        }
        String[] properties=format.trim().split("\\s*,\\s*");       
             if (separator.equalsIgnoreCase(TAB)) separator="\t";
        else if (separator.equalsIgnoreCase(SEMICOLON)) separator=";";
        else if (separator.equalsIgnoreCase(COMMA)) separator=",";
        else if (separator.equalsIgnoreCase(COLON)) separator=":";
        else if (separator.equalsIgnoreCase(PIPE)) separator="|";
             if (listseparator.equalsIgnoreCase(TAB)) listseparator="\t";
        else if (listseparator.equalsIgnoreCase(SEMICOLON)) listseparator=";";
        else if (listseparator.equalsIgnoreCase(COMMA)) listseparator=",";
        else if (listseparator.equalsIgnoreCase(COLON)) listseparator=":";
        else if (listseparator.equalsIgnoreCase(PIPE)) listseparator="|";             
        StringBuilder outputString=new StringBuilder();
        if (header) {
            outputString.append("#");
            String headerString=format.replaceAll("\\s*,\\s*", separator);
            outputString.append(headerString);
            outputString.append("\n");
        }
        if (dataobject instanceof SequenceCollection) {
            ArrayList<Sequence> sequencelist=((SequenceCollection)dataobject).getAllSequencesInDefaultOrder(engine);
            if (sortOrder!=null) sortOrder.sortDataAccordingToMap(sequencelist);            
            int size=sequencelist.size();
            int i=0;
            for (Sequence sequence:sequencelist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                outputSequence(sequence, outputString, properties, separator, listseparator);
                // task.setStatusMessage("Sequence "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%100==0) Thread.yield();
            }
        } else if (dataobject instanceof Sequence){
            outputSequence((Sequence)dataobject, outputString, properties, separator, listseparator);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }


    /** output formats a single sequence */
    protected void outputSequence(Sequence sequence, StringBuilder outputString, String[] properties, String separator, String listseparator) throws ExecutionError {
        boolean first=true;
        for (String property:properties) {
            if (first) first=false; else outputString.append(separator);          
            Object value=null;
            try {
                value=sequence.getPropertyValue(property,engine); // this throws ExecutionError if property is not recognized
            } catch (Exception e) { // check if the property could be a map instead
               Data item=engine.getDataItem(property);
               if (item instanceof SequenceTextMap || item instanceof SequenceNumericMap) value=((DataMap)item).getValue(sequence.getName());
               else throw new ExecutionError("'"+property+"' is not a recognized sequence property or applicable Map");
            }                
            if (value instanceof ArrayList) outputArrayList((ArrayList)value,outputString,listseparator);
            else if (value!=null) outputString.append(value.toString());
        }
        outputString.append("\n");
    }

    /** Outputs an ArrayList to a StringBuilder buffer as a list of comma-separated values*/
    private void outputArrayList(ArrayList list, StringBuilder outputString, String separator) {
        Iterator i = list.iterator();
	boolean first=true;
	while (i.hasNext()) {
	    if (first) first=false; else outputString.append(separator);
            Object e = i.next();
	    outputString.append(e.toString());	    
	}
    }

    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        throw new ParseError("DATAFORMAT ERROR: Unable to input data in Sequence_Properties format (functionality not implemented)");
    }


                       
}
