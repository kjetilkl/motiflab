/*


 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.DataMap;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.MotifNumericMap;
import motiflab.engine.data.MotifTextMap;

/**
 *
 * @author kjetikl
 */
public class DataFormat_MotifProperties extends DataFormat {
    private String name="Motif_Properties";
    public static final String TAB="TAB";
    public static final String SEMICOLON="Semicolon";
    public static final String COMMA="Comma";
    public static final String COLON="Colon";    
    public static final String PIPE="Vertical bar";    
    private Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class};


    public DataFormat_MotifProperties() {
        String[] standard=Motif.getAllStandardProperties(true);
        String[] standardExt=new String[standard.length+2];
        System.arraycopy(standard, 0, standardExt, 0, standard.length);
        standardExt[standardExt.length-1]="ID";
        standardExt[standardExt.length-2]="Matrix";
        Arrays.sort(standardExt);
        StringBuilder builder=new StringBuilder();
        builder.append("<html>An ordered, comma-separated list of properties to output for each motif.<br>Note that standard properties are case-insensitive,<br>but user-defined properties are case-sensitive!<br>");
        builder.append("<br>Standard motif properties include:<br><br>");
        for (String prop:standardExt) {builder.append(prop);builder.append("<br>");}
        builder.append("</html>");
        addParameter("Format", "ID, Short name, Classification, Consensus", null,builder.toString(),true,false);
        addOptionalParameter("Separator",TAB, new String[]{TAB,SEMICOLON,COMMA,COLON,PIPE},"The character used to separate properties in the output (replaces commas in the format-string)");
        addOptionalParameter("List-separator",COMMA, new String[]{TAB,SEMICOLON,COMMA,COLON,PIPE},"The character used to separate elements within properties");
        addOptionalParameter("Header", Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Add a header (starting with #) in the first line which tells which fields are included");
        addOptionalParameter("Sort by", null, new Class[]{MotifNumericMap.class},"If specified, the motifs will be sorted according to ascending value in this map");
        setParameterFilter("Header","output"); 
        setParameterFilter("Sort by","output");         
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof MotifCollection || data instanceof Motif);
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(MotifCollection.class) || dataclass.equals(Motif.class));
    }

    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof MotifCollection || data instanceof Motif);
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(MotifCollection.class) || dataclass.equals(Motif.class));
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
        MotifNumericMap sortOrder=null;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             format=(String)settings.getResolvedParameter("Format",defaults,engine);
             separator=(String)settings.getResolvedParameter("Separator",defaults,engine);
             listseparator=(String)settings.getResolvedParameter("List-separator",defaults,engine);
             header=(Boolean)settings.getResolvedParameter("Header",defaults,engine);
             sortOrder=(MotifNumericMap)settings.getResolvedParameter("Sort by",defaults,engine);             
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
             sortOrder=(MotifNumericMap)getDefaultValueForParameter("Sort by");             
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
        if (dataobject instanceof MotifCollection) {
            ArrayList<Motif> motiflist=((MotifCollection)dataobject).getAllMotifs(engine);
            if (sortOrder!=null) sortOrder.sortDataAccordingToMap(motiflist);
            int size=motiflist.size();
            int i=0;
            for (Motif motif:motiflist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                outputMotif(motif, outputString, properties, separator, listseparator);
                // task.setStatusMessage("Motif "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%100==0) Thread.yield();
            }
        } else if (dataobject instanceof Motif){
            outputMotif((Motif)dataobject, outputString, properties, separator, listseparator);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }


    /** output formats a single motif */
    protected void outputMotif(Motif motif, StringBuilder outputString, String[] properties, String separator, String listseparator) throws ExecutionError {
        boolean first=true;
        for (String property:properties) {
            if (first) first=false; else outputString.append(separator);        
            Object value=null;
            try {
               value=motif.getPropertyValue(property,engine); // this throws ExecutionError if property is not recognized                    
            } catch (Exception e) { // check if the property could be a map instead
               Data item=engine.getDataItem(property);
               if (item instanceof MotifTextMap || item instanceof MotifNumericMap) value=((DataMap)item).getValue(motif.getName());
               else throw new ExecutionError("'"+property+"' is not a recognized motif property or applicable Map");
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
       if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_MotifProperties.parseInput(ArrayList<String> input, Data target)");
 
        String separator=TAB;
        String listseparator=COMMA;
        String format="";
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             format=(String)settings.getResolvedParameter("Format",defaults,engine);
             separator=(String)settings.getResolvedParameter("Separator",defaults,engine);
             listseparator=(String)settings.getResolvedParameter("List-separator",defaults,engine);        
          } catch (ExecutionError e) {
             throw new ParseError(e.getMessage());
          } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.toString());
          }
        } else {
             format=(String)getDefaultValueForParameter("Format");
             separator=(String)getDefaultValueForParameter("Separator");
             listseparator=(String)getDefaultValueForParameter("List-separator");                     
        }               
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
        if (input==null || input.isEmpty()) throw new ParseError("No data to parse");
        int firstLineIndex=0;
        String[] properties=null;
        if (format.equals("*")) { // try do determine format from header              
            String firstLine=input.get(0);
            firstLineIndex=1;
            if (!firstLine.startsWith("#")) throw new ParseError("Either the 'format' parameter must be specified or the file must contain a header defining the format on the first line");
            format=firstLine.substring(1).trim(); 
            properties=format.trim().split(separator); // use same separator as in header (according to specification)
        } else {
           properties=format.trim().split("\\s*,\\s*"); 
        }
        if (format.isEmpty()) throw new ParseError("Missing format specification");        
        
        
        boolean IDpresent=false;
        for (String prop:properties) {
            if (prop.equalsIgnoreCase("ID")) {IDpresent=true;break;}
        }
        if (!IDpresent) throw new ParseError("Required property 'ID' is missing from the format specification");
        if (target instanceof MotifCollection) return parseMotifCollection(input,(MotifCollection)target,properties,separator,listseparator, task);
        else if (target instanceof Motif) {
            if (input.size()<=firstLineIndex) throw new ParseError("No data to parse");
            return parseMotif(input.get(firstLineIndex),(Motif)target,properties,separator,listseparator);  
        }
        else throw new ParseError("Unable to parse Motif input to target data of type "+target.getTypeDescription());
    }
    

    protected Motif parseMotif(String line, Motif target, String[] properties, String separator, String listseparator) throws ParseError {
       String motifID=null;
       String[] values=line.split(separator);
       if (values.length<properties.length) throw new ParseError("Expected "+properties.length+" properties but found only "+values.length+": "+line);      
       // determine motifID first
       for (int i=0;i<properties.length;i++) {
           if (properties[i].equalsIgnoreCase("ID")) {
               motifID=values[i];
               break;
           }
       }       
       if (motifID==null) throw new ParseError("Missing required property: ID");
       if (target==null) target=new Motif(motifID);
       else target.rename(motifID);
       
       for (int i=0;i<properties.length;i++) {
           String property=properties[i];
           String valueAsString=values[i];   
           Object value=null;
           if (property.equals("*")) continue; // property defined as wildcard. Skip this column!
           if (property.equalsIgnoreCase("ID")) {
               continue; // this has been processed already
           }    
           if (valueAsString.isEmpty()) continue; // don't bother if the value is empty
           Object basicValue=MotifLabEngine.getBasicValueForStringAsObject(valueAsString);
           
           Class type=Motif.getPropertyClass(property, engine);
                if (type==String.class) {value=valueAsString;}
           else if (type==Boolean.class) {
               if (!(basicValue instanceof Boolean)) throw new ParseError("Expected boolean value for property '"+property+"'. Got: "+valueAsString);    
               value=basicValue;
           }
           else if (type==Integer.class) {
                if (!(basicValue instanceof Integer || basicValue instanceof Double)) throw new ParseError("Expected numeric value for property '"+property+"'. Got: "+valueAsString);           
                value=basicValue;
           }
           else if (type==Double.class) {
                if (basicValue instanceof Integer) basicValue=new Double((Integer)basicValue);
                if (!(basicValue instanceof Double)) throw new ParseError("Expected numeric value for property '"+property+"'. Got: "+valueAsString);    
                value=basicValue;
           }
           else if (type==List.class || type==ArrayList.class) {
                String[] list=valueAsString.split(listseparator);
                ArrayList<String> newlist=new ArrayList<String>(list.length);
                newlist.addAll(Arrays.asList(list));
                value=newlist;                 
           }
           else if (type==null) { // this would be completely new user-defined property
               if (basicValue instanceof String) { // still a string, check if it could be a list
                   String[] list=valueAsString.split(listseparator);
                   if (list.length>1) { // it is a list
                        ArrayList<String> newlist=new ArrayList<String>(list.length);
                        newlist.addAll(Arrays.asList(list));
                        value=newlist;
                   } else value=valueAsString; // not a list
               } else value=basicValue;              
           }  
           try {
               target.setPropertyValue(property, value);
           } catch (ExecutionError e) {
               throw new ParseError(e.getMessage());
           }
       }
       return target;
    } 
                        
    
    protected MotifCollection parseMotifCollection(List<String> input, MotifCollection target, String[] properties, String separator, String listseparator, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) target=new MotifCollection("MotifCollection");
        int count=0;  
        int size=input.size();
        for (String line:input) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            Motif motif=parseMotif(line, null, properties, separator, listseparator);
            target.addMotifToPayload(motif);            
            if (task!=null) task.setProgress(count, size);
            count++;
            if (count%100==0) {
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              if (count%200==0) Thread.yield();
              setProgress(count, size);
            }                            
        }
        return target;
    }
                          
                       
}
