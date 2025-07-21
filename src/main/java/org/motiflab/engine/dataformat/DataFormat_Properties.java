/*


 */

package org.motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.BasicDataType;
import org.motiflab.engine.data.DataCollection;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.TextVariable;
import org.motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class DataFormat_Properties extends DataFormat {
    private static final int WHEN_MISSING_DO_NOTHING=0;
    private static final int WHEN_MISSING_LEAVE_BLANK=1;
    private static final int WHEN_MISSING_SKIP_LINE=2;
    
    private static final String MISSING_DO_NOTHING="Leave as is";
    private static final String MISSING_LEAVE_BLANK="Leave blank";
    private static final String MISSING_SKIP_LINE="Skip lines";    

    private final String name="Properties";  
    private final Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class, ModuleCollection.class, ModuleCRM.class, SequenceCollection.class, Sequence.class};
    private static final Pattern fieldcode=Pattern.compile("(?<!\\\\)\\{(.+?)\\}"); // matches {XXX} not preceeded by the escape character \ 

    public DataFormat_Properties() {
        addParameter("Template", null, new Class[]{TextVariable.class},"A Text Variable containing a template for the output that should be generated for each data item in the collection. This template can contain property references on the form: {propertyname} that will be replaced with the actual values of the specified properties.",true,false);
        addOptionalParameter("If missing", MISSING_LEAVE_BLANK, new String[]{MISSING_LEAVE_BLANK,MISSING_SKIP_LINE},"<html>What to do if a data object has no value for a specific property:<br><ul><li><b>"+MISSING_LEAVE_BLANK+"</b>: Insert an empty string for the property value</li><li><b>"+MISSING_SKIP_LINE+"</b>: Skip the whole line (or block) that includes the property</li></ul></html>");
        addOptionalParameter("Sort by", null, new Class[]{NumericMap.class},"If specified, the collection will be sorted according to ascending values in this map");   
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof DataCollection || data instanceof Motif || data instanceof ModuleCRM || data instanceof Sequence);
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (DataCollection.class.isAssignableFrom(dataclass) || dataclass.equals(Motif.class) || dataclass.equals(ModuleCRM.class) || dataclass.equals(Sequence.class));
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
        return "txt";
    }


    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(5);
        TextVariable template=null;
        NumericMap sortOrder=null;
        int ifMissing=WHEN_MISSING_LEAVE_BLANK;
        String missingString=MISSING_LEAVE_BLANK;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             template=(TextVariable)settings.getResolvedParameter("Template",defaults,engine);     
             missingString=(String)settings.getResolvedParameter("If missing",defaults,engine);
             sortOrder=(NumericMap)settings.getResolvedParameter("Sort by",defaults,engine);               
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
             template=(TextVariable)getDefaultValueForParameter("Template");
             missingString=(String)getDefaultValueForParameter("If missing");             
             sortOrder=(NumericMap)getDefaultValueForParameter("Sort by");             
        }
        if (template==null) throw new ExecutionError("Missing template for Properties format");
        if (missingString.equalsIgnoreCase(MISSING_DO_NOTHING)) ifMissing=WHEN_MISSING_DO_NOTHING;
        else if (missingString.equalsIgnoreCase(MISSING_SKIP_LINE)) ifMissing=WHEN_MISSING_SKIP_LINE;
        if (!(dataobject instanceof DataCollection)) { // convert single item to collection instead
            Data item=dataobject;
            if (item instanceof Motif) {dataobject=new MotifCollection("tmp");((MotifCollection)dataobject).addMotif((Motif)item);}
            else if (item instanceof ModuleCRM) {dataobject=new ModuleCollection("tmp");((ModuleCollection)dataobject).addModule((ModuleCRM)item);}
            else if (item instanceof Sequence) {dataobject=new SequenceCollection("tmp");((SequenceCollection)dataobject).addSequence((Sequence)item);}           
        } 

        Object[] result=parseTemplate(template, ((DataCollection)dataobject).getMembersClass());
        String header=(result[0]==null)?null:resolveEscapeCharacters((String)result[0]);
        String separator=(result[1]==null)?null:resolveEscapeCharacters((String)result[1]);
        String footer=(result[2]==null)?null:resolveEscapeCharacters((String)result[2]);
        ArrayList<Object> lines=(ArrayList<Object>)result[3];
        
        ArrayList<String> list=((DataCollection)dataobject).getValues();
        if (sortOrder!=null) sortOrder.sortAccordingToMap(list);
        else Collections.sort(list); 
        
        int size=list.size();
        int i=1;        
        StringBuilder outputString=new StringBuilder();
        if (header!=null) {
            outputString.append(processHSF(header,i,size));
            outputString.append("\n");            
        }
        for (String dataname:list) {          
            if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
            Data data=engine.getDataItem(dataname);
            if (!(data instanceof BasicDataType)) continue;
            outputData((BasicDataType)data, lines, ifMissing, outputString);          
            // task.setStatusMessage(""+(i+1)+" of "+size);
            setProgress(i, size);
            if (i%100==0) Thread.yield();
            i++;              
            if (i<=size && separator!=null) {
                outputString.append(processHSF(separator,i,size));
                outputString.append("\n");                
            }                           
        }
        if (footer!=null) {
            outputString.append(processHSF(footer,i,size));
            outputString.append("\n");
        }        
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }
    
    private String processHSF(String line, int i, int size) {
        line=line.replace("{counter}", ""+(i));
        line=line.replace("{counter-1}", ""+(i-1));        
        line=line.replace("{size}", ""+(size)); 
        return line;
    }


    /** output formats a single data item 
     * @param dataitem
     * @param template
     * @param ifMissing
     * @param outputString
     * @throws ExecutionError 
     */
    protected void outputData(BasicDataType dataitem, ArrayList<Object> template, int ifMissing, StringBuilder outputString) throws ExecutionError {
        for (Object object:template) {
            if (object instanceof String) {
                String line=(replaceRegularReferences((String)object, dataitem, ifMissing));
                if (line!=null) {outputString.append(line);outputString.append("\n");}
            } else if (object instanceof Object[]) { // repeat block
                String[] properties=(String[])((Object[])object)[0];
                String settings=(String)((Object[])object)[1];
                String listSeparator=getValueWithin(settings, "\"","\"",",");
                String inlineSeparator=getValueWithin(settings, "'","'",null); if (inlineSeparator!=null && inlineSeparator.isEmpty()) inlineSeparator=null;
                String defaultValue=null;                
                boolean inline=(settings.toLowerCase().contains("inline") || settings.toLowerCase().contains("single line"));
                boolean breakBefore=(settings.toLowerCase().contains("break before"));
                boolean breakAfter=(settings.toLowerCase().contains("break after"));
                boolean expandLists=(settings.toLowerCase().contains("expand list"));
                if (expandLists) inline=false; // these two are mutually exclusive
                if (inline && outputString.length()>0 && outputString.charAt(outputString.length()-1)=='\n' && !breakBefore) {outputString=outputString.deleteCharAt(outputString.length()-1);} // remove last inserted newline
                int whenMissing=ifMissing;
                if (settings.contains("[")) {defaultValue=getValueWithin(settings, "[","]",null);}
                else if (settings.toLowerCase().contains("skip")) whenMissing=WHEN_MISSING_SKIP_LINE;
                else if (settings.toLowerCase().contains("blank")) whenMissing=WHEN_MISSING_LEAVE_BLANK;
                ArrayList<String> lines=(ArrayList<String>)((Object[])object)[2];
                int propCount=0; // counts the number of properties in the repeat block that have actually been output
                for (String propertyName:properties) {
                    if (propertyName.equalsIgnoreCase("matrix") && dataitem instanceof Motif) { // handle this as a special case
                        outputMatrix((Motif)dataitem, lines, settings, whenMissing, outputString);
                    } else {
                        Object value=dataitem.getPropertyValue(propertyName, engine);
                        if ((value instanceof String && ((String)value).isEmpty()) || (value instanceof List && ((List)value).isEmpty())) value=null; // value is missing or empty
                        if (value==null && defaultValue!=null) value=defaultValue;
                        String valueAsString=null;
                        List valueAsList=null;
                        if (expandLists && value instanceof List) {valueAsList=(List)value;}
                        else if (expandLists && value instanceof String && ((String)value).contains(",")) {String[] tmp=((String)value).split("\\s*,\\s*");valueAsList=Arrays.asList(tmp);}
                        StringBuilder text=new StringBuilder();
                        if (value!=null) {
                           valueAsString=(value instanceof List)?MotifLabEngine.splice((List)value, listSeparator):value.toString(); 
                        } else { // property value is missing
                           if (whenMissing==WHEN_MISSING_SKIP_LINE) continue; // skip whole block and proceed to next property in the list
                           else if (whenMissing==WHEN_MISSING_LEAVE_BLANK) valueAsString="";
                        }          
                        boolean more=true; int listCounter=0;
                        if (expandLists && valueAsList!=null) {
                            if (valueAsList.isEmpty()) more=false; else valueAsString=valueAsList.get(0).toString();
                        }
                        outer:
                        while (more) { // used to repeat expanded lists into multiple property values
                            for (int j=0;j<lines.size();j++) { // output repeat block for a single property/value pair
                                String line=lines.get(j);
                                if (valueAsString!=null) {
                                    line=line.replace("{key}", propertyName);   line=line.replace("{KEY}", propertyName); // allow both lower and uppercase
                                    line=line.replace("{value}", valueAsString);line=line.replace("{VALUE}", valueAsString);                                
                                }
                                line=replaceRegularReferences(line, dataitem, whenMissing);
                                if (line==null) {text=null;break outer;} // lines contain unknown properties. Skip the whole block for this property
                                text.append(line);
                                if (inline) {
                                    if (inlineSeparator!=null) text.append(inlineSeparator);
                                } else text.append("\n");
                            } 
                            if (expandLists && valueAsList!=null) {
                                listCounter++;
                                if (listCounter<valueAsList.size()) valueAsString=valueAsList.get(listCounter).toString();
                                else more=false;
                            }
                            else more=false;
                        }
                        // the property has indeed generated output, so append it to the buffer
                        if (text!=null) {
                            outputString.append(text.toString());
                            propCount++; 
                        }
                    }
                } // end: for each property in repeat block
                if (inline) {
                   if (inlineSeparator!=null && propCount>0) { // remove the last inserted inlineSeparator
                       int length=outputString.length();
                       int start=length-inlineSeparator.length();
                       outputString.delete(start, length);
                   }
                   if (breakAfter) outputString.append("\n");
                }                 
            } // end: repeat block in template           
        }
    }
    
    private String getValueWithin(String string, String opening, String closing, String defaultValue) {
        int p1=string.indexOf(opening)+1;
        int p2=string.lastIndexOf(closing);
        if (p2>p1) return (resolveEscapeCharacters(string.substring(p1,p2)));
        else return defaultValue;
    }
    
    private String replaceRegularReferences(String line, BasicDataType data, int ifMissing) { // replace references on the form {propertyName} with their actual values
       HashMap<String,String> fields=new HashMap<>();
       Matcher matcher=fieldcode.matcher(line);  
       while (matcher.find()) {
           String propertyName=matcher.group(1);
           Object value=null;
           try {value=data.getPropertyValue(propertyName, engine);} catch (ExecutionError e) {}
           if ((value instanceof String && ((String)value).isEmpty()) || (value instanceof List && ((List)value).isEmpty())) value=null; // sort of missing
           if (value!=null) {
               String valueAsString=(value instanceof List)?MotifLabEngine.splice((List)value, ","):value.toString(); 
               fields.put(propertyName,valueAsString);
           } else {
               if (ifMissing==WHEN_MISSING_SKIP_LINE) return null;
               else if (ifMissing==WHEN_MISSING_LEAVE_BLANK) fields.put(propertyName,"");
           } 
       }
       for (String key:fields.keySet()) {
           line=line.replace("{"+key+"}", fields.get(key));
       }
       return line;
    }

    private void outputMatrix(Motif motif, ArrayList<String> template, String settings, int ifMissing, StringBuilder outputString) {
        double[][] matrix=null;
        boolean vertical=true;
        String colSeparator=",";
        boolean integer=false;        
        if (settings.toLowerCase().contains("horisontal") || settings.toLowerCase().contains("horizontal")) vertical=false;
        if (settings.toLowerCase().contains("frequency")) {matrix=Motif.normalizeMatrix(motif.getMatrixAsFrequencyMatrix());}
        else if (settings.toLowerCase().contains("count")) {
            integer=true;
            int[][] intmatrix=motif.getMatrixAsCountMatrix(3);
            if (intmatrix==null) return;
            matrix=new double[intmatrix.length][4];
            for (int i=0;i<intmatrix.length;i++) {
                for (int j=0;j<4;j++) {
                    matrix[i][j]=intmatrix[i][j];
                }
            }
        } else {
            integer=(motif.getMatrixType()==Motif.COUNT_MATRIX);
            matrix=motif.getMatrix();
        }
        if (settings.contains("\"")) {
            colSeparator=getValueWithin(settings, "\"", "\"",",");
        }
        if (matrix==null) return;
        if (vertical) {
            String text="";
            for (int i=0;i<matrix.length;i++) {
                double[] row=matrix[i];
                for (String line:template) {  
                    line=line.replace("{A}", ""+formatNumber(row[0],integer));
                    line=line.replace("{C}", ""+formatNumber(row[1],integer));
                    line=line.replace("{G}", ""+formatNumber(row[2],integer));
                    line=line.replace("{T}", ""+formatNumber(row[3],integer));
                    line=line.replace("{x}", ""+Motif.getConsensusBase(row[0], row[1], row[2], row[3]));
                    line=line.replace("{X}", ""+Motif.getConsensusBase(row[0], row[1], row[2], row[3]).toUpperCase());
                    line=line.replace("{row}", ""+(i+1));                             
                    line=line.replace("{row-1}", ""+i);                                               
                    line=line.replace("{rowXX}", ""+pad(i+1));                             
                    line=line.replace("{rowXX-1}", ""+pad(i));                                               
                    line=replaceRegularReferences(line, motif, ifMissing);
                    if (line!=null) text+=(line+"\n"); // lines contain unknown properties. Skip the whole block;
                } 
            }
            outputString.append(text);
        } else {
            String text="";
            for (String line:template) {                    
                line=line.replace("{A}", ""+splice(matrix,0,colSeparator,integer));
                line=line.replace("{C}", ""+splice(matrix,1,colSeparator,integer));
                line=line.replace("{G}", ""+splice(matrix,2,colSeparator,integer));
                line=line.replace("{T}", ""+splice(matrix,3,colSeparator,integer));
                line=line.replace("{x}", ""+spliceConsensus(matrix,colSeparator,false));
                line=line.replace("{X}", ""+spliceConsensus(matrix,colSeparator,true));            
                line=line.replace("{columns}", ""+spliceAndPad(matrix.length,1,colSeparator,false));                          
                line=line.replace("{columns-1}", ""+spliceAndPad(matrix.length,0,colSeparator,false));  
                line=line.replace("{columnsXX}", ""+spliceAndPad(matrix.length,1,colSeparator,true));                          
                line=line.replace("{columnsXX-1}", ""+spliceAndPad(matrix.length,0,colSeparator,true));                
                line=replaceRegularReferences(line, motif, ifMissing);
                if (line!=null) text+=(line+"\n"); // lines contain unknown properties. Skip the whole block;
            }            
            outputString.append(text);            
        }
    }
    private String formatNumber(double input, boolean integer) {
        if (integer) return ""+((int)input);
        else return ""+input;
    }  
    private String pad(int input) {
        return (input<10)?("0"+input):(""+input);
    }
    private String spliceAndPad(int length, int offset, String separator, boolean pad) {   
        StringBuilder builder=new StringBuilder();
        for (int i=0;i<length;i++) {
            if (i>0) builder.append(separator);
            int value=i+offset;
            builder.append((pad && value<10)?("0"+value):(""+value));
        }
        return builder.toString();        
    }
    
    private String splice(double[][] matrix, int base, String separator, boolean integer) {
        StringBuilder builder=new StringBuilder();
        for (int i=0;i<matrix.length;i++) {
            if (i>0) builder.append(separator);
            double value=0;
            if (base>=0) value=matrix[i][base];
            else value=i+base+2;
            if (integer) builder.append((int)value);
            else builder.append(value);
        }
        return builder.toString();
    }
    
    private String spliceConsensus(double[][] matrix, String separator, boolean uppercase) {
        StringBuilder builder=new StringBuilder();
        for (int i=0;i<matrix.length;i++) {
            if (i>0) builder.append(separator);
            String letter=Motif.getConsensusBase(matrix[i][0], matrix[i][1], matrix[i][2], matrix[i][3]);
            builder.append(letter);
        }
        String string=builder.toString();
        if (uppercase) return string.toUpperCase();
        else return string;
    }
    
    
    private Object[] parseTemplate(TextVariable template, Class type) {
        String header=null;
        String separator=null;
        String footer=null;
        ArrayList<Object> output=new ArrayList<>();
        ArrayList<String> lines=template.getAllStrings();
        // parse "header directives" first and remove them
        Iterator<String> iter=lines.iterator();
        int start=0;
        while (iter.hasNext()) {
            String line=iter.next();
            if (line.startsWith("HEADER:")) {
                header=line.substring("HEADER:".length());
                start++;
            } else if (line.startsWith("FOOTER:")) {
                footer=line.substring("FOOTER:".length());
                start++;
            } else if (line.startsWith("SEPARATOR:")) {
                separator=line.substring("SEPARATOR:".length());
                start++;
            } else break;
        }
        // now parse the actual template
        for (int i=start;i<lines.size();i++) {
            String line=lines.get(i);
            if (line.startsWith("%")) {
               ArrayList<String> replines=new ArrayList<>();
               String[] parts=line.substring(1).trim().split("\\s*:\\s*");
               String[] properties=processProperties(parts[0].trim().split("\\s*,\\s*"), type);
               String settings=(parts.length<=1)?"":parts[1]; // (parts[1].trim().split("\\s*,\\s*"));
               while (i<lines.size()-1) {
                   i++;                   
                   line=lines.get(i);
                   if (line.startsWith("%%")) break; else replines.add(line);
               }
               output.add(new Object[]{properties,settings,replines});
            } else {
               output.add(line);
            }
        }
        return new Object[]{header,separator,footer,output};        
    }
    
    private String[] processProperties(String[] properties, Class type) {
        ArrayList<String> result=new ArrayList<>();
        for (String property:properties) {
           if (property.equals("*")) {
               String[] props=null;
               if (type==Motif.class) props=Motif.getAllProperties(true, engine);
               else if (type==ModuleCRM.class) props=ModuleCRM.getAllProperties(true, engine);
               else if (type==Sequence.class) props=Sequence.getAllProperties(engine);
               if (props!=null) {
                   List list=Arrays.asList(props);
                   Collections.sort(list);
                   result.addAll(list);
               }  
           } else if (property.equalsIgnoreCase("*user") || property.equalsIgnoreCase("*U")) {
               String[] props=null;
               if (type==Motif.class) props=Motif.getAllUserDefinedProperties(engine);
               else if (type==ModuleCRM.class) props=ModuleCRM.getAllUserDefinedProperties(engine);
               else if (type==Sequence.class) props=Sequence.getAllUserDefinedProperties(engine);
               if (props!=null) {
                   List list=Arrays.asList(props);
                   Collections.sort(list);
                   result.addAll(list);
               }              
           } else if (property.equalsIgnoreCase("*standard") || property.equalsIgnoreCase("*S")) {
               String[] props=null;
               if (type==Motif.class) props=Motif.getAllStandardProperties(true);
               else if (type==ModuleCRM.class) props=ModuleCRM.getAllStandardProperties(true);
               else if (type==Sequence.class) props=Sequence.getAllStandardProperties(engine);
               if (props!=null) {
                   List list=Arrays.asList(props);
                   Collections.sort(list);
                   result.addAll(list);
               }                
           } else if (property.startsWith("-")) {
               result.remove(property.substring(1));
           } else result.add(property);
        }
        String[] list=new String[result.size()];
        return result.toArray(list);
    }

    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("Data format '"+getName()+"' can not be used to parse data");
    }
                      
    
    private String resolveEscapeCharacters(String input) {
        input=input.replace("\\n","\n");
        input=input.replace("\\t","\t");
        return input;
    }
}
