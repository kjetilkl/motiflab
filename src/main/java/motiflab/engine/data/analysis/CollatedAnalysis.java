/*
 
 
 */

package motiflab.engine.data.analysis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JTable;
import motiflab.engine.data.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import static motiflab.engine.data.analysis.Analysis.EXCEL;
import motiflab.engine.dataformat.DataFormat;
import motiflab.gui.GenericBrowserPanel;
import motiflab.gui.GenericModuleBrowserPanel;
import motiflab.gui.GenericMotifBrowserPanel;
import motiflab.gui.GenericSequenceBrowserPanel;
import motiflab.gui.ModuleLogo;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.MotifLogo;
import motiflab.gui.ToolTipHeader;
import motiflab.gui.VisualizationSettings;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * This class represents a diverse set of analyses that are created by collating 
 * together data from several other analyses. It is not an analysis that can be
 * performed with the 'analyze' operation, but rather the result of a 'collate' operation.
 * @author kjetikl
 */
public class CollatedAnalysis extends Analysis {
    private final static String typedescription="Collated Analysis";
    private final static String analysisName="Collated analysis";
    private final static String description="Counts the number of occurrences of each motif in each sequence and the total number";

    private Class collateType=null;
    private ArrayList<String> columns; // Names of the columns included in this Analysis (in order)
    private ArrayList<Class> columnTypes; // The types of the columns included in this Analysis (in order)
    private ArrayList<HashMap<String,Object>> tabledata; // A HashMap for each column. Note that the 'Object' can be an array if additional info is needed 
    private HashSet<String> entryNames; // this set contains the names of all entries (Motifs, sequences etc.) that have been included in this analysis 
    private ArrayList<String[]> originalColumns; // information about which columns the ones here were based on. The value is a pair with the names of [0] analysis of origin [1] column name in that analysis
    private String headline=null; // an optional headline to show for this analysis (defaults to "Collated Analysis")
    
    private static final int PREFIX_COLUMNS_FOR_MOTIF=4; // Color, ID, Name, Class
    private static final int PREFIX_COLUMNS_FOR_MODULE=2; // Color, ID
    private static final int PREFIX_COLUMNS_FOR_SEQUENCE=1; // ID
    private static final int POSTFIX_COLUMNS_FOR_MOTIF=1; // Logo
    private static final int POSTFIX_COLUMNS_FOR_MODULE=1; // Logo
    private static final int POSTFIX_COLUMNS_FOR_SEQUENCE=0; // 0

    public CollatedAnalysis(String name) {
        this.name=name;
        columns=new ArrayList<String>();
        columnTypes=new ArrayList<Class>();
        tabledata=new ArrayList<HashMap<String, Object>>();
        entryNames=new HashSet<String>();
        originalColumns=new ArrayList<String[]>();
    }

    /** Adds a new (empty) column to this collated analysis
     *  @param columnname the name to use for the new column in this analysis
     *  @param type The class of the Data object that this collated analysis contains (e.g. Motif.class, Module.class or Sequence.class)
     *  @param columnData
     *  @param origin A String pair defining the [0] Analysis of origin (or DataMap) for the column [1] name of the column in that analysis (or the keyword 'values' for DataMaps)
     */
    public void addColumn(String columnname, Class type, HashMap<String,Object> columnData, String[] origin) throws ExecutionError {
        if (columns!=null) {
            for (int i=0;i<columns.size();i++) if (columnname.equalsIgnoreCase(columns.get(i))) throw new ExecutionError("Duplicate column name '"+columnname+"' in analysis '"+name+"'");
        }
        if ((collateType==Motif.class || collateType==Module.class) && columnname.equalsIgnoreCase("ID")) throw new ExecutionError("The column name 'ID' is reserved in analysis '"+name+"'");
        if ((collateType==Motif.class || collateType==Module.class) && columnname.equalsIgnoreCase("Logo")) throw new ExecutionError("The column name 'Logo' is reserved in analysis '"+name+"'");
        if (columnname.equalsIgnoreCase("Name")) throw new ExecutionError("The column name 'Name' is reserved in analysis '"+name+"'");
        if (collateType==Motif.class && columnname.equalsIgnoreCase("Class")) throw new ExecutionError("The column name 'Class' is reserved in analysis '"+name+"'");
        columns.add(columnname);
        columnTypes.add(type);
        tabledata.add(cloneColumn(columnData));
        entryNames.addAll(columnData.keySet());
        originalColumns.add(origin);
    }
    
    public void setColumnData(String column, String dataID, Object value) throws ExecutionError {
        if (column==null || column.isEmpty() || columns==null || columns.isEmpty()) throw new ExecutionError("Analysis '"+name+"' does not a column named '"+column+"'");
        for (int i=0;i<columns.size();i++) {
           if (column.equalsIgnoreCase(columns.get(i))) {
               HashMap<String,Object> col=tabledata.get(i);
               Object useValue=(value instanceof AnnotatedValue)?((AnnotatedValue)value).getValue():value;
               if (useValue!=null) { // check that the type is correct
                   Class expected=columnTypes.get(i);
                   if (useValue.getClass()!=expected) throw new ExecutionError("The value for '"+dataID+"' in column '"+column+"' is not of the expected type. Expected "+expected.getSimpleName()+" but got "+value.getClass().getSimpleName()+".");
               } 
               col.put(dataID,value);
               entryNames.add(dataID);
               return;
           }
        }       
        throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
    }      
    
    /** 
     * Returns the (original) list of entries (rows) included in this collated analysis
     */
    public HashSet<String> getEntries() {
       return entryNames; 
    }
    
    @Override
    public String[] getSourceProxyParameters() {return new String[]{};}
    
    @Override
    public Class getCollateType() {
        return collateType;
    }    
    
    public void setCollateType(Class type) {
        collateType=type;
    }        
    
    public void setOptionalTitle(String title) {
        this.headline=title;
    }
    
    public String getOptionalTitle() {
        return headline;
    }    
    
    @Override
    public String[] getColumnsExportedForCollation() {
        return getResultVariables();
    }

    @Override
    public Class getColumnType(String column) {
        if (columns==null || columns.isEmpty()) return null;
         for (int i=0;i<columns.size();i++) {
           if (column.equalsIgnoreCase(columns.get(i))) {
               return columnTypes.get(i);
           }
        }
        return null;
    }

    @Override
    public Parameter[] getOutputParameters(String dataformat) {
        // Unfortunately, it is not possible to filter the "Include" parameter below based on collateType, 
        // because the operation dialog bases the parameters on a template-analysis (where the collateType is not set) rather than the actual analysis object             
        Parameter incPar  = new Parameter("Include",DataCollection.class,null,new Class[]{DataCollection.class},"Only include data from this collection",false,false);
        Parameter logos   = new Parameter("Logos",String.class,getMotifLogoDefaultOption(dataformat), getMotifLogoOptions(dataformat),"Include graphical logos in the table",false,false);
        Parameter markPar = new Parameter("Only markup",Boolean.class,Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, columns will not show regular values, only markup classes",false,false);
        Parameter colPar  = new Parameter("Color boxes",Boolean.class,Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a box with the assigned color for the motif, module or sequence will be output as the first column",false,false);
        Parameter legend  = new Parameter("Legend",Boolean.class,Boolean.TRUE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a header with a title and analysis details will be included at the top of the Excel sheet.",false,false);          
        if (dataformat.equals(HTML)) return new Parameter[]{incPar,logos,markPar,colPar};
        else if (dataformat.equals(EXCEL)) return new Parameter[]{incPar,logos,markPar,legend};
        else if (dataformat.equals(RAWDATA)) return new Parameter[]{incPar,logos,markPar};
        else return new Parameter[0];
    }
    
//    @Override
//    public String[] getOutputParameterFilter(String parameter) {
//        if (parameter.equals("Only markup") || parameter.equals("Color boxes")) return new String[]{"HTML"};
//        if (parameter.equals("Include") || parameter.equals("Logos")) return new String[]{"HTML","RawData"};        
//        return null;
//    }    

    @Override
    public String[] getResultVariables() {
        if (columns==null || columns.isEmpty()) return new String[0];
        else {
          String[] results=new String[columns.size()];
          return columns.toArray(results);
        }
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename==null || variablename.isEmpty() || columns==null || columns.isEmpty()) throw new ExecutionError("'"+getName()+"' does not have a result for ''");
        for (int i=0;i<columns.size();i++) {
           if (variablename.equalsIgnoreCase(columns.get(i))) {
               Class type=columnTypes.get(i);
               if (Number.class.isAssignableFrom(type)) {
                   if (collateType==Motif.class || collateType==Module.class || collateType==Sequence.class) return getColumnAsNumericMap(tabledata.get(i));
                   else return getColumnAsTextVariable(tabledata.get(i)); // Numeric data for non-standard type
               }
               else {
                   if (collateType==Motif.class || collateType==Module.class || collateType==Sequence.class) return getColumnAsTextMap(tabledata.get(i));
                   else return getColumnAsTextVariable(tabledata.get(i));
               }
           }
        }
        throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (columns==null || columns.isEmpty() || variablename==null) return null;
       for (int i=0;i<columns.size();i++) {
           if (variablename.equalsIgnoreCase(columns.get(i))) {
               Class type=columnTypes.get(i);               
               if (Number.class.isAssignableFrom(type)) {
                        if (collateType==Motif.class) return MotifNumericMap.class;
                   else if (collateType==Module.class) return ModuleNumericMap.class;
                   else if (collateType==Sequence.class) return SequenceNumericMap.class;
                   else return TextVariable.class; // this has not been accounted for, so I just return a text variable :|
               }
               else {
                        if (collateType==Motif.class) return MotifTextMap.class;
                   else if (collateType==Module.class) return ModuleTextMap.class;
                   else if (collateType==Sequence.class) return SequenceTextMap.class;
                   else return TextVariable.class; // this has not been accounted for, so I just return a text variable :|
               }              
           }
       }
       return null; // defaults
    }

    private NumericMap getColumnAsNumericMap(HashMap<String, Object> column) {
        NumericMap result=null;
             if (collateType==Motif.class) result=new MotifNumericMap("temp",0);
        else if (collateType==Module.class) result=new ModuleNumericMap("temp",0);
        else if (collateType==Sequence.class) result=new SequenceNumericMap("temp",0);
        ArrayList<String> rows=new ArrayList<String>(entryNames.size());
        rows.addAll(entryNames);
        Collections.sort(rows);
        for (String key:rows) {
            Object value=column.get(key);
            if (value instanceof AnnotatedValue) value=((AnnotatedValue)value).getValue();
            if (value!=null) {
                result.setValue(key, ((Number)value).doubleValue());
            }
        }
        return result;
    }
    
    private TextVariable getColumnAsTextVariable(HashMap<String, Object> column) {
        TextVariable result=new TextVariable("temp");
        ArrayList<String> rows=new ArrayList<String>(entryNames.size());
        rows.addAll(entryNames);
        Collections.sort(rows);
        for (String key:rows) {
            Object value=column.get(key);
            if (value instanceof AnnotatedValue) value=((AnnotatedValue)value).getValue();
            if (value==null) value="";
            result.append(key+"\t"+value.toString()); // use TAB to separate key from value
        }
        return result;
    }    
    
    private TextMap getColumnAsTextMap(HashMap<String, Object> column) {
        TextMap result=null;
             if (collateType==Motif.class) result=new MotifTextMap("temp","");
        else if (collateType==Module.class) result=new ModuleTextMap("temp","");
        else if (collateType==Sequence.class) result=new SequenceTextMap("temp","");
        for (String key:column.keySet()) {
            Object value=column.get(key);
            if (value instanceof AnnotatedValue) value=((AnnotatedValue)value).getValue();
            if (value==null) value="";
            result.setValue(key,value.toString()); // use TAB to separate key from value
        }
        return result;
    }        
    

    private Object getCellData(int column, String dataID)  {
       HashMap<String,Object> col=tabledata.get(column);
       return col.get(dataID);
    }


    private Object getCellValue(int column, String dataID)  {
       HashMap<String,Object> col=tabledata.get(column);
       Object cell=col.get(dataID);
       return (cell instanceof AnnotatedValue)?((AnnotatedValue)cell).getValue():cell;
    }
    /** Returns the class name associated with the given cell, or NULL if the cell has no associated class */
    private String getCellMarkup(int column, String dataID)  {
       HashMap<String,Object> col=tabledata.get(column);
       Object cell=col.get(dataID);
       return (cell instanceof AnnotatedValue)?((AnnotatedValue)cell).getMarkup():null;
    }
    
    @Override
    public HashMap<String,Object> getColumnData(String column) throws ExecutionError {
        if (column==null || column.isEmpty() || columns==null || columns.isEmpty()) throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
        for (int i=0;i<columns.size();i++) {
           if (column.equalsIgnoreCase(columns.get(i))) {
               return tabledata.get(i);
           }
        }       
        throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
    }  
    
    /** Sets the data for a column directly. Use with caution!*/
    @Deprecated    
    public void setColumnData(String column, HashMap<String,Object> data, Class type) throws ExecutionError {
        if (column==null || column.isEmpty() || columns==null || columns.isEmpty()) throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
        for (int i=0;i<columns.size();i++) {
           if (column.equalsIgnoreCase(columns.get(i))) {
               tabledata.set(i, data);
               columnTypes.set(i, type);
               return;
           }
        }       
        throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
    }   
    /** Sets the data type a column directly. Use with caution! */
    @Deprecated
    public void setColumnType(String column, Class type) throws ExecutionError {
        if (column==null || column.isEmpty() || columns==null || columns.isEmpty()) throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
        for (int i=0;i<columns.size();i++) {
           if (column.equalsIgnoreCase(columns.get(i))) {
               columnTypes.set(i, type);
               return;
           }
        }       
        throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
    }      
    
    
    
    @Override
    public String getAnalysisName() {
        return analysisName;
    }

    @Override
    public String getDescription() {return description;}


    @Override
    @SuppressWarnings("unchecked")
    public CollatedAnalysis clone() { 
        CollatedAnalysis newanalysis=new CollatedAnalysis(name);
        super.cloneCommonSettings(newanalysis);
        newanalysis.headline=this.headline;
        newanalysis.collateType=this.collateType;
        newanalysis.columns=(this.columns!=null)?(ArrayList<String>)columns.clone():null;
        newanalysis.columnTypes=(this.columnTypes!=null)?(ArrayList<Class>)columnTypes.clone():null;
        newanalysis.entryNames=(this.entryNames!=null)?(HashSet<String>)entryNames.clone():null;
        newanalysis.originalColumns=(this.originalColumns!=null)?(ArrayList<String[]>)originalColumns.clone():null;
        if (this.tabledata==null) newanalysis.tabledata=null;
        else { // clone the data
            ArrayList<HashMap<String,Object>> datacopy=new  ArrayList<HashMap<String,Object>>(tabledata.size());
            for (int i=0;i<tabledata.size();i++) {
                HashMap<String,Object> datacolumn=tabledata.get(i);
                HashMap<String,Object> datacolumncopy=new HashMap<String, Object>(datacolumn.size());
                for (String key:datacolumn.keySet()) {
                    Object dataObject=datacolumn.get(key);
                    Object dataObjectCopy=(dataObject!=null)?cloneObject(dataObject):null;
                    datacolumncopy.put(key, dataObjectCopy);
                }
                datacopy.add(datacolumncopy);
            }
        }     
        return newanalysis;
    }

    private HashMap<String,Object> cloneColumn(HashMap<String,Object> datacolumn) {
        HashMap<String,Object> datacolumncopy=new HashMap<String, Object>(datacolumn.size());
        for (String key:datacolumn.keySet()) {
            Object dataObject=datacolumn.get(key);
            Object dataObjectCopy=(dataObject!=null)?cloneObject(dataObject):null;
            datacolumncopy.put(key, dataObjectCopy);
        }
        return datacolumncopy;
    }

    private Object cloneObject(Object object) {
        if (object instanceof Object[]) {
            Object[] newarray=new Object[((Object[])object).length];
            for (int i=0;i<newarray.length;i++) {
                newarray[i]=cloneObject(((Object[])object)[i]);
            }
            return newarray;
        }
        else if (object instanceof Color) return new Color(((Color)object).getRGB());
        else return object; // assuming that object is String or Number which is a constant anyway   
    }
    
    
    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        this.headline=((CollatedAnalysis)source).headline;
        this.collateType=((CollatedAnalysis)source).collateType;
        this.columns=((CollatedAnalysis)source).columns;
        this.columnTypes=((CollatedAnalysis)source).columnTypes;
        this.tabledata=((CollatedAnalysis)source).tabledata;
        this.entryNames=((CollatedAnalysis)source).entryNames; 
        this.originalColumns=((CollatedAnalysis)source).originalColumns;        
    }

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

    
    @Override
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        AnnotatedValueRenderer histogramrenderer=new AnnotatedValueRenderer(engine.getClient().getVisualizationSettings());
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        Color [] basecolors=vizSettings.getBaseColors();
        MotifLogo sequencelogo=new MotifLogo(basecolors,sequencelogoSize);
        String showSequenceLogosString="";
        boolean showOnlyMarkup=false;
        boolean showColorBoxes=false;
        DataCollection include=null;
        if (settings!=null) {
            try {
                Parameter[] defaults=getOutputParameters(format);
                showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
                showOnlyMarkup=(Boolean)settings.getResolvedParameter("Only markup",defaults,engine);
                showColorBoxes=(Boolean)settings.getResolvedParameter("Color boxes",defaults,engine);
                include=(DataCollection)settings.getResolvedParameter("Include",defaults,engine);             
            }
            catch (ExecutionError e) {throw e;}
            catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        if (include!=null && include.getMembersClass()!=collateType) include=null; // ignore non-compatible collection
        boolean showSequenceLogos = includeLogosInOutput(showSequenceLogosString);
        showSequenceLogos = (showSequenceLogos && (collateType==Motif.class || collateType==Module.class));
        
        engine.createHTMLheader((headline!=null)?headline:"Collated Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<h1 class=\"headline\">",HTML);
        outputobject.append((headline!=null)?headline:"Collated Analysis",HTML);
        outputobject.append("</h1>\n",HTML);
        outputobject.append("<table class=\"sortable\">\n",HTML);
        outputobject.append("<tr>",HTML);
        if (showColorBoxes) outputobject.append("<th>&nbsp;</th>",HTML);
        if (collateType==Motif.class) {
            outputobject.append("<th>ID</th>",HTML);
            outputobject.append("<th>Name</th>",HTML);
            outputobject.append("<th class=\"sorttable_ip\">Class</th>",HTML);
        } else {
            outputobject.append("<th>ID</th>",HTML);
        }
        for (int j=0;j<columns.size();j++) { // collated columns here
            String[] originalColumn=originalColumns.get(j);
            outputobject.append("<th",HTML);
            if (originalColumn!=null) {
                outputobject.append(" title=\"",HTML);
                outputobject.append(escapeHTML(originalColumn[0]),HTML);
                outputobject.append(" &rarr; ",HTML);
                outputobject.append(escapeHTML(originalColumn[1]),HTML);
                outputobject.append("\"",HTML);
            }
            outputobject.append(">",HTML);
            outputobject.append(escapeHTML(columns.get(j)),HTML);
            outputobject.append("</th>",HTML);
        }
        if (showSequenceLogos) outputobject.append("<th class=\"sorttable_nosort\">Logo</th>",HTML);
        outputobject.append("</tr>\n",HTML);
        ArrayList<String> rows=new ArrayList<String>(entryNames.size());
        if (include!=null) {
           for (String entry:include.getValues()) {
               if (entryNames.contains(entry)) rows.add(entry);
           }
        } else rows.addAll(entryNames);
        Collections.sort(rows);
        for (int i=0;i<rows.size();i++) {
            String dataname=rows.get(i);
            // if (include!=null && !include.contains(dataname)) continue; // this should be redundant now
            outputobject.append("<tr>",HTML);
            if (showColorBoxes) {
                Color color=Color.WHITE;
                Data data=engine.getDataItem(dataname);                
                if (data instanceof Motif || data instanceof Module) color=vizSettings.getFeatureColor(dataname);
                else if (data instanceof Sequence) color=vizSettings.getSequenceLabelColor(dataname);               
                String colorString=VisualizationSettings.convertColorToHTMLrepresentation(color);
                outputobject.append("<td><div style=\"width:10px;height:10px;border:1px solid #000;background-color:"+colorString+";\"></div></td>",HTML);
            }
            outputPrefixColumnsHTML(outputobject, dataname, engine);
            for (int j=0;j<columns.size();j++) {              
                Object cellData=getCellData(j, dataname);
                Object value=(cellData instanceof AnnotatedValue)?((AnnotatedValue)cellData).getValue():cellData;
                String markup=(cellData instanceof AnnotatedValue)?((AnnotatedValue)cellData).getMarkup():null;
                if (markup!=null) outputobject.append("<td class=\""+markup+"\"",HTML);
                else if (value instanceof Number) outputobject.append("<td class=\"num\"",HTML);
                else outputobject.append("<td",HTML);
                if (value==null) {
                    outputobject.append("></td>",HTML);
                } else if (value instanceof double[]) { // render as a histogram
                    outputobject.append(">",HTML);
                    if (!showOnlyMarkup) histogramrenderer.outputHistogramToHTML(outputobject, (double[])value, engine);
                    outputobject.append("</td>",HTML);
                } else {
                    String valueAsString=null;
                    if (value instanceof Double) valueAsString=histogramrenderer.formatNumber((Double)value);
                    else valueAsString=value.toString();
                    valueAsString=escapeHTML(valueAsString);
                    valueAsString=valueAsString.replace("\"", "&#34;");// escape quotes
                    if (showOnlyMarkup) {
                        outputobject.append(" key=\""+valueAsString+"\"",HTML);  // include value as 'hidden' custom sort key
                        outputobject.append(" title=\""+valueAsString+"\"",HTML);  // include value as tooltip also!
                    }
                    outputobject.append(">",HTML);
                    if (!showOnlyMarkup) outputobject.append(valueAsString,HTML);
                    outputobject.append("</td>",HTML);
                }
            }
            if (showSequenceLogos) {
                Data data=engine.getDataItem(dataname);                
                if (data instanceof Motif) {
                   sequencelogo.setMotif((Motif)data);
                   outputobject.append("<td title=\"",HTML);
                   outputobject.append(sequencelogo.getMotifInfoTooltip(),HTML);
                   outputobject.append("\">",HTML);
                }
                else outputobject.append("<td>",HTML);
                if (data instanceof Motif) outputobject.append(getMotifLogoTag((Motif)data, outputobject, sequencelogo, showSequenceLogosString, engine),HTML);
                else if (data instanceof Module) outputobject.append(getModuleLogoTag((Module)data, outputobject, showSequenceLogosString, engine),HTML);               
                outputobject.append("</td>",HTML);
            }
            outputobject.append("</tr>\n",HTML);
            if (i%100==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                Thread.yield();
            }
            task.setStatusMessage("Executing operation: output ("+(i+1)+"/"+rows.size()+")");
            format.setProgress(i, rows.size()); //
        }

        outputobject.append("</table>\n</body>\n</html>\n",HTML);
        format.setProgress(100);
        return outputobject;
    }
       

    @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        AnnotatedValueRenderer histogramrenderer=new AnnotatedValueRenderer(engine.getClient().getVisualizationSettings());
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        Color [] basecolors=vizSettings.getBaseColors();
        boolean border=(Boolean)vizSettings.getSettingAsType("motif.border", Boolean.TRUE);        
        MotifLogo sequencelogo = (collateType==Motif.class)?new MotifLogo(basecolors,sequencelogoSize):null;
        ModuleLogo modulelogo = (collateType==Module.class)?new ModuleLogo(vizSettings):null;
        int logoheight = (collateType==Module.class)?28:19;        
        String showSequenceLogosString="";
        boolean showOnlyMarkup=false;
        boolean includeLegend=false;
        DataCollection include=null;
        if (settings!=null) {
            try {
                Parameter[] defaults=getOutputParameters(format);
                showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
                showOnlyMarkup=(Boolean)settings.getResolvedParameter("Only markup",defaults,engine);
                includeLegend=(Boolean)settings.getResolvedParameter("Legend",defaults,engine); 
                include=(DataCollection)settings.getResolvedParameter("Include",defaults,engine);             
            }
            catch (ExecutionError e) {throw e;}
            catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        if (include!=null && include.getMembersClass()!=collateType) include=null; // ignore non-compatible collection
        boolean showLogos = includeLogosInOutput(showSequenceLogosString);
        showLogos = (showLogos && (collateType==Motif.class || collateType==Module.class));
        boolean showLogosAsText = (showLogos && includeLogosInOutputAsText(showSequenceLogosString));
        
        int rownum=0;
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Collated Analysis");
        CreationHelper helper = workbook.getCreationHelper();
        Drawing drawing = sheet.createDrawingPatriarch();         
                   
        CellStyle title = getExcelTitleStyle(workbook);
        CellStyle tableheader = getExcelTableHeaderStyle(workbook);
        CellStyle normalStyle=createExcelStyle(workbook, BorderStyle.NONE, null, HorizontalAlignment.GENERAL, false);      
        normalStyle.setVerticalAlignment(VerticalAlignment.CENTER);        
        
        // Make room for the header which will be added later
        Row row = null;
        int headerrows=3;
        if (includeLegend) {
            for (int j=0;j<headerrows;j++) {
               row = sheet.createRow(j); 
            }
            rownum=headerrows-1; // -1 because it will be incremented later on...
        }     
        Cell cell;
        int column=0;  
        int columnCount=0; // number of columns not including logos
        int maxlogowidth=0;
        row = sheet.createRow(rownum);
        ArrayList<Integer> dontResizeColumns = new ArrayList<>(2);
        
        // header row
        if (collateType==Motif.class) {
            cell = row.createCell(column++); cell.setCellValue("ID"); cell.setCellStyle(tableheader);
            cell = row.createCell(column++); cell.setCellValue("Name"); cell.setCellStyle(tableheader);
            cell = row.createCell(column++); cell.setCellValue("Class"); cell.setCellStyle(tableheader);           
        } else {
            cell = row.createCell(column++); cell.setCellValue("ID"); cell.setCellStyle(tableheader);
        }
        for (int j=0;j<columns.size();j++) { // collated columns here                        
            cell = row.createCell(column); cell.setCellValue(columns.get(j)); cell.setCellStyle(tableheader);
            Class type = columnTypes.get(j);
            if (type == double[].class) { // this column contains a histogram
                sheet.setColumnWidth(column, 3800);
                dontResizeColumns.add(column);
            } 
            column++;
        }      
        if (showLogos) {
            cell = row.createCell(column); cell.setCellValue("Logo"); cell.setCellStyle(tableheader);
            sheet.setColumnWidth(column, 10000);
            dontResizeColumns.add(column);
            column++;            
        }
        columnCount=column;         
               
        ArrayList<String> rows=new ArrayList<String>(entryNames.size());
        if (include!=null) {
           for (String entry:include.getValues()) {
               if (entryNames.contains(entry)) rows.add(entry);
           }
        } else rows.addAll(entryNames);
        Collections.sort(rows);
        HashMap<String,CellStyle> markupStyles = new HashMap<>();

        for (int i=0;i<rows.size();i++) {
            rownum++;            
            column = 0;
            String dataname=rows.get(i);
            // if (include!=null && !include.contains(dataname)) continue; // this should be redundant now
            row = sheet.createRow(rownum);
            column = outputPrefixColumnsExcel(row, column, dataname, normalStyle, engine); // either 3 or 1 column depending on the collation type
            for (int j=0;j<columns.size();j++) {
                cell = row.createCell(column);
                cell.setCellStyle(normalStyle);
                Object cellData=getCellData(j, dataname);
                Object value=(cellData instanceof AnnotatedValue)?((AnnotatedValue)cellData).getValue():cellData;
                String markup=(cellData instanceof AnnotatedValue)?((AnnotatedValue)cellData).getMarkup():null;
                
                // set background color based on markup     
                if (markup!=null) {
                    if (!markupStyles.containsKey(markup)) {
                        Color color = vizSettings.getSystemColor(markup);
                        CellStyle style = createExcelStyle(workbook, null, color, null, false);
                        style.setVerticalAlignment(VerticalAlignment.CENTER);
                        markupStyles.put(markup, style);
                    }
                    cell.setCellStyle(markupStyles.get(markup));
                }                
                if (!showOnlyMarkup) {
                    if (value instanceof Number) {
                        double number = ((Number)value).doubleValue();
                        if (!Double.isNaN(number)) cell.setCellValue( number );
                    } else if (value instanceof String) {
                        cell.setCellValue((String)value);
                    } else if (value instanceof double[]) { // render as a histogram
                        try {
                            byte[] image=histogramrenderer.outputHistogramToByteArray( (double[])value );
                            int imageIndex=workbook.addPicture(image, XSSFWorkbook.PICTURE_TYPE_PNG);
                            ClientAnchor picanchor = helper.createClientAnchor();
                            picanchor.setCol1(column);
                            picanchor.setRow1(rownum);
                            picanchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
                            Picture pict=drawing.createPicture(picanchor, imageIndex);	
                            pict.resize();
                            int offsetX=25000;
                            int offsetY=25000;                    
                            picanchor.setDx1(offsetX);
                            picanchor.setDy1(offsetY);    
                            picanchor.setDx2(picanchor.getDx2()+offsetX);
                            picanchor.setDy2(picanchor.getDy2()+offsetY);                
                        } catch (IOException e) {throw new ExecutionError(e.getMessage());}                        
                    } else if (value!=null) {
                        cell.setCellValue(""+value);
                    }
                }
                column++;
            }
            if (showLogos) {
                Data data=engine.getDataItem(dataname);                
                cell = row.createCell(column);
                if (data instanceof Motif && showLogosAsText) {
                    cell.setCellValue( ((Motif)data).getConsensusMotif() );
                } else if (data instanceof Motif) {
                     Motif motif = (Motif)data;                    
                     try {          
                        sequencelogo.setMotif(motif);
                        int width=motif.getLength();
                        if (width>maxlogowidth) maxlogowidth=width;
                        byte[] image=getMotifLogoImageAsByteArray(sequencelogo, logoheight, border, "png");
                        int imageIndex=workbook.addPicture(image, XSSFWorkbook.PICTURE_TYPE_PNG);
                        ClientAnchor picanchor = helper.createClientAnchor();
                        picanchor.setCol1(column);
                        picanchor.setRow1(rownum);
                        picanchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
                        Picture pict=drawing.createPicture(picanchor, imageIndex);	
                        pict.resize();
                        int offsetX=25000;
                        int offsetY=25000;
                        picanchor.setDx1(offsetX);
                        picanchor.setDy1(offsetY);    
                        picanchor.setDx2(picanchor.getDx2()+offsetX);
                        picanchor.setDy2(picanchor.getDy2()+offsetY);                         
                    } catch (Exception e) {throw new ExecutionError(e.getMessage());}                   
                } else if (data instanceof Module && showLogosAsText) {                   
                    cell.setCellValue( ((Module)data).getModuleLogo() );
                } else if (data instanceof Module) {
                    Module module = (Module)data;
                    try {
                        row.setHeightInPoints((short)(logoheight));                        
                        modulelogo.setModule(module);                        
                        byte[] image=getModuleLogoImageAsByteArray(modulelogo, logoheight, false, "png");
                        int imageIndex=workbook.addPicture(image, XSSFWorkbook.PICTURE_TYPE_PNG);
                        ClientAnchor anchor = helper.createClientAnchor();
                        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
                        anchor.setCol1(column);
                        anchor.setRow1(rownum);                                                
                        Picture pict=drawing.createPicture(anchor, imageIndex);	
                        pict.resize();   
                        // now offset the image a little bit down and to the left to avoid overlap with the Excel cell borders
                        int offsetX=25000;
                        int offSetY=25000;
                        anchor.setDx1(anchor.getDx1()+offsetX); // top-left
                        anchor.setDy1(anchor.getDy1()+offSetY); // top-left  
                        anchor.setDx2(anchor.getDx2()+offsetX); // bottom-right
                        anchor.setDy2(anchor.getDy2()+offSetY); // bottom-right
                    } catch (Exception e) {throw new ExecutionError(e.getMessage(),e);}                    
                }                                     
            }

            if (i%100==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                Thread.yield();
            }
            task.setStatusMessage("Executing operation: output ("+(i+1)+"/"+rows.size()+")");
            format.setProgress(i, rows.size()); //
        }
        // resize columns to fit (but not the ones containing images)
        for (int i=0; i<columnCount; i++) {
            if (dontResizeColumns.contains(i)) continue;
            sheet.autoSizeColumn(i);
            try {sheet.setColumnWidth(i, sheet.getColumnWidth(i)+800); } // add some padding
            catch (java.lang.IllegalArgumentException argX) {}
        }        
       
        if (includeLegend) {        
            sheet.createFreezePane(0,headerrows,0,headerrows);
            row=sheet.getRow(0);
            outputStringValueInCell(row, 0, headline, title);
            // StringBuilder firstLine=new StringBuilder();
            // firstLine.append("Add more information here?");
            // row=sheet.getRow(2);
            // outputStringValueInCell(row, 0, firstLine.toString(), null); 
        }
        
        // now write to the outputobject. The binary Excel file is included as a dependency in the otherwise empty OutputData object.
        File excelFile=outputobject.createDependentBinaryFile(engine,"xlsx");        
        try {
            BufferedOutputStream stream=new BufferedOutputStream(new FileOutputStream(excelFile));
            workbook.write(stream);
            stream.close();
        } catch (Exception e) {
            throw new ExecutionError("An error occurred when creating the Excel file: "+e.toString(),0);
        }
        outputobject.setBinary(true);        
        outputobject.setDirty(true); // this is not set automatically since I don't append to the document
        outputobject.setDataFormat(EXCEL); // this is not set automatically since I don't append to the document
        format.setProgress(95);
        return outputobject;
    }
    
    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {      
        AnnotatedValueRenderer histogramrenderer=new AnnotatedValueRenderer(engine.getClient().getVisualizationSettings());
        String showSequenceLogosString="";
        boolean showOnlyMarkup=false;
        DataCollection include=null;
        if (settings!=null) {
            try {
                Parameter[] defaults=getOutputParameters(format);
                showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
                showOnlyMarkup=(Boolean)settings.getResolvedParameter("Only markup",defaults,engine);
                include=(DataCollection)settings.getResolvedParameter("Include",defaults,engine);              
            }
            catch (ExecutionError e) {throw e;}
            catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        if (include!=null && include.getMembersClass()!=collateType) include=null; // ignore non-compatible collection        
        boolean showSequenceLogos = includeLogosInOutput(showSequenceLogosString);
        showSequenceLogos = (showSequenceLogos && (collateType==Motif.class || collateType==Module.class));
        ArrayList<String> rows=new ArrayList<String>(entryNames.size());;
        if (include!=null) {
           for (String entry:include.getValues()) {
               if (entryNames.contains(entry)) rows.add(entry);
           }
        } else rows.addAll(entryNames);
        Collections.sort(rows);
        outputobject.append("# Collated Analysis",RAWDATA);
        if (headline!=null) outputobject.append(": "+headline,RAWDATA);
        outputobject.append("\n#\n",RAWDATA);
        // output column headers
        if (collateType==Motif.class) outputobject.append("#ID\tName\tClass",RAWDATA);
        else outputobject.append("#ID",RAWDATA);
        for (int j=0;j<columns.size();j++) {     
            outputobject.append("\t",RAWDATA);
            outputobject.append(columns.get(j),RAWDATA);            
        }
        if (showSequenceLogos) outputobject.append("\tLogo",RAWDATA);
        outputobject.append("\n",RAWDATA);
        // now output rows
        for (int i=0;i<rows.size();i++) {
            String dataname=rows.get(i);
            // if (include!=null && !include.contains(dataname)) continue; // this should be redundant now
            outputPrefixColumnsRaw(outputobject, dataname, engine); 
            for (int j=0;j<columns.size();j++) {
                outputobject.append("\t",RAWDATA);
                Object value=(showOnlyMarkup)?getCellMarkup(j, dataname):getCellValue(j, dataname);
                if (value!=null) {
                    if (value instanceof double[]) histogramrenderer.outputHistogramToRaw(outputobject, (double[])value);
                    else outputobject.append(value.toString(), RAWDATA);
                }
            }
            if (showSequenceLogos) {
                outputobject.append("\t",RAWDATA);
                Data data=engine.getDataItem(dataname);
                if (data instanceof Motif) outputobject.append(((Motif)data).getConsensusMotif(),RAWDATA);
                else if (data instanceof Module) outputobject.append(((Module)data).getModuleLogo(),RAWDATA);
            }
            outputobject.append("\n",RAWDATA);
            if (i%100==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                Thread.yield();
            }
            task.setStatusMessage("Executing operation: output ("+(i+1)+"/"+rows.size()+")");
            format.setProgress(i, rows.size()); //             
        }
        format.setProgress(100);
        return outputobject;
    }

    private void outputPrefixColumnsRaw(OutputData output, String dataname, MotifLabEngine engine) {
        if (collateType==Motif.class) {
            output.append(dataname, RAWDATA);
            output.append("\t", RAWDATA);                
            Data object=engine.getDataItem(dataname);
            String motifname=(object instanceof Motif)?((Motif)object).getShortName():null;
            output.append((motifname!=null)?motifname:"", RAWDATA);
            output.append("\t", RAWDATA);   
            String classname=(object instanceof Motif)?((Motif)object).getClassification():null;  
            output.append((classname!=null)?classname:"unknown", RAWDATA);
        
        } else output.append(dataname, RAWDATA); // not anything more interresting for Module or Sequence so far     
    }

    private void outputPrefixColumnsHTML(OutputData output, String dataname, MotifLabEngine engine) {
        if (collateType==Motif.class) {
            output.append("<td>", HTML);
            output.append(escapeHTML(dataname), HTML);
            output.append("</td>", HTML);
            output.append("<td>", HTML);
            Data object=engine.getDataItem(dataname);
            String motifname=(object instanceof Motif)?((Motif)object).getShortName():null;
            output.append((motifname!=null)?escapeHTML(motifname):"", HTML);
            output.append("</td>", HTML);

            String motifclass=(object instanceof Motif)?((Motif)object).getClassification():null;
            String motifclassname=null;
            if (motifclass==null) motifclass="unknown";
            else motifclassname=MotifClassification.getNameForClass(motifclass);
            if (motifclassname!=null) {
                motifclassname=escapeHTML(motifclassname);
                motifclassname=motifclassname.replace("\"", "&#34;");
            } // escape quotes
            output.append("<td"+((motifclassname!=null)?(" title=\""+motifclassname+"\""):"")+">"+escapeHTML(motifclass)+"</td>",HTML);           
        } else {
            output.append("<td>", HTML);
            output.append(escapeHTML(dataname), HTML);
            output.append("</td>", HTML);
        } // not anything more interresting for Module or Sequence so far
    }
    
    private int outputPrefixColumnsExcel(Row row, int column, String dataname, CellStyle style, MotifLabEngine engine) {
        if (collateType==Motif.class) {
            Cell cell = row.createCell(column++);
            cell.setCellValue(dataname);           
            cell = row.createCell(column++);
            Data object=engine.getDataItem(dataname);
            String motifname=(object instanceof Motif)?((Motif)object).getShortName():null;            
            cell.setCellValue((motifname!=null)?motifname:"");
            cell.setCellStyle(style);           
            cell = row.createCell(column++);
            String classname=(object instanceof Motif)?((Motif)object).getClassification():null;
            cell.setCellValue((classname!=null)?classname:"unknown");  
            cell.setCellStyle(style);            
        } else {
            Cell cell = row.createCell(column++);
            cell.setCellValue(dataname);
            cell.setCellStyle(style);            
        } // not anything more interresting for Module or Sequence so far
        return column;
    }    

    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        throw new ExecutionError("CollatedAnalysis can not be executed!");
    }
    
    @Override
    protected JPanel getDisplayPanel(MotifLabGUI gui, boolean modal) {
        JPanel displayPanel=new JPanel(new BorderLayout());
        JPanel headerPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
        String headerString="<html><h2>"+((headline!=null)?headline:"Collated analysis")+"</h2></html>";
        headerPanel.add(new JLabel(headerString));
        CollatedTableModel tablemodel=new CollatedTableModel(gui);
        GenericBrowserPanel panel;
        if (collateType==Motif.class) panel=new GenericMotifBrowserPanel(gui, tablemodel, modal);
        else if (collateType==Module.class) panel=new GenericModuleBrowserPanel(gui, tablemodel, modal);
        else if (collateType==Sequence.class) panel=new GenericSequenceBrowserPanel(gui, tablemodel, modal);
        else panel=new GenericMotifBrowserPanel(gui, tablemodel, modal);
        ArrayList<String> headertooltips=new ArrayList<String>(columns.size()+5); // the +5 is not a 'constant', its just to make some extra room for whatever may come next
        if (collateType==Motif.class) {headertooltips.add("Color");headertooltips.add("ID");headertooltips.add("Name");headertooltips.add("Class");}
        else if (collateType==Module.class) {headertooltips.add("Color");headertooltips.add("ID");}
        else if (collateType==Sequence.class) {headertooltips.add("Name");}
        for (int i=0;i<columns.size();i++) {
            String[] origin=originalColumns.get(i);
            if (origin!=null) headertooltips.add(origin[0]+" &rarr; "+origin[1]);
            else headertooltips.add(columns.get(i));
        }
        if (collateType==Motif.class || collateType==Module.class) headertooltips.add("Logo");
        JTable table=panel.getTable();
        ToolTipHeader header = new ToolTipHeader(table.getColumnModel(),headertooltips);
        table.setTableHeader(header);
        if (collateType==Motif.class) {
            table.getColumn("Class").setCellRenderer(new CellRenderer_Classification());
            table.getColumn("Name").setCellRenderer(new CellRenderer_MotifName(gui.getEngine()));
        }
        table.getTableHeader().setReorderingAllowed(false);
        AnnotatedValueRenderer renderer=new AnnotatedValueRenderer(gui.getVisualizationSettings());
        table.setDefaultRenderer(Object.class,renderer);
        if (table.getRowSorter() instanceof TableRowSorter) {
            TableRowSorter rowsorter=(TableRowSorter)table.getRowSorter();
            for (int i=0;i<table.getColumnCount();i++) {
                if (!Data.class.isAssignableFrom(table.getColumnClass(i))) rowsorter.setComparator(i, renderer); // I do not use this comparator for Data objects since these columns usually contain logos...
            }
        }
        panel.getTableScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.setPreferredSize(new java.awt.Dimension(700,500));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8,5,0,5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)
          ));
        displayPanel.add(headerPanel,BorderLayout.NORTH);
        displayPanel.add(panel,BorderLayout.CENTER);
        return displayPanel;
    }

//private class NaNComparator implements Comparator<Double> {
//    @Override
//    public int compare(Double value1, Double value2) {
//         if (value1==null && value2==null) return 0;
//         if (value1==null) return 1;
//         if (value2==null) return -1;
//         if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;
//         if (Double.isNaN(value1)) return 1;
//         if (Double.isNaN(value2)) return -1;
//         return value2.compareTo(value1); // Note that this sorts descending!
//    }
//}

private class CollatedTableModel extends AbstractTableModel {
    private String[] rows;
    private MotifLabEngine engine;
    private String[] columnnames;
    private VisualizationSettings settings;
    
    public CollatedTableModel(MotifLabGUI gui) {
        this.engine=gui.getEngine();
        this.settings=gui.getVisualizationSettings();
        rows=new String[entryNames.size()];
        rows=entryNames.toArray(rows);
        Arrays.sort(rows);
        if (collateType==Motif.class) {
            columnnames=new String[columns.size()+PREFIX_COLUMNS_FOR_MOTIF+POSTFIX_COLUMNS_FOR_MOTIF]; //
            columnnames[0]=" "; // this is for color            
            columnnames[1]="ID";
            columnnames[2]="Name";
            columnnames[3]="Class";
            for (int i=0;i<columns.size();i++) columnnames[i+PREFIX_COLUMNS_FOR_MOTIF]=columns.get(i);
            columnnames[columnnames.length-1]="Logo";
        } else if (collateType==Module.class) {
            columnnames=new String[columns.size()+PREFIX_COLUMNS_FOR_MODULE+POSTFIX_COLUMNS_FOR_MODULE];
            columnnames[0]=" "; // this is for color
            columnnames[1]="ID";            
            for (int i=0;i<columns.size();i++) columnnames[i+PREFIX_COLUMNS_FOR_MODULE]=columns.get(i);
            columnnames[columnnames.length-1]="Logo";
        } else {
            columnnames=new String[columns.size()+PREFIX_COLUMNS_FOR_SEQUENCE+POSTFIX_COLUMNS_FOR_SEQUENCE];
            columnnames[0]="Name";
            for (int i=0;i<columns.size();i++) columnnames[i+PREFIX_COLUMNS_FOR_SEQUENCE]=columns.get(i);
        }
    }


    @Override
    public Class getColumnClass(int columnIndex) {
        if (collateType==Motif.class) {
            if (columnIndex==0) return Color.class;
            else if (columnIndex==1) return String.class; // ID
            else if (columnIndex==2) return String.class; // Name
            else if (columnIndex==3) return String.class; // Class
            else if (columnIndex==columnnames.length-1) return Motif.class; // this is for the logo
            else return columnTypes.get(columnIndex-PREFIX_COLUMNS_FOR_MOTIF);
        } else if (collateType==Module.class) {
            if (columnIndex==0) return Color.class;
            else if (columnIndex==1) return String.class; // ID
            else if (columnIndex==columnnames.length-1) return Module.class; // this is for the logo
            else return columnTypes.get(columnIndex-PREFIX_COLUMNS_FOR_MODULE);
        } else {
            if (columnIndex==0) return Sequence.class; // Name
            else return columnTypes.get(columnIndex-PREFIX_COLUMNS_FOR_SEQUENCE);
        }
    }

    public final Motif getMotif(String id) {
        Data data=engine.getDataItem(id);
        if (data instanceof Motif) return (Motif)data;
        else return null;
    }
    
    public String getMotifName(String id) {
        Data data=engine.getDataItem(id);
        if (data instanceof Motif) return ((Motif)data).getShortName();
        else return "unknown";
    }
    
    public String getMotifClass(String id) {
        Data data=engine.getDataItem(id);
        if (data instanceof Motif) {
            String motifclass=((Motif)data).getClassification();
            if (motifclass!=null) return motifclass; else return "unknown";
        }
        else return "unknown";
    }

    public final Module getModule(String id) {
        Data data=engine.getDataItem(id);
        if (data instanceof Module) return (Module)data;
        else return null;
    }

    public final Sequence getSequence(String id) {
        Data data=engine.getDataItem(id);
        if (data instanceof Sequence) return (Sequence)data;
        else return null;
    }    
    
    public Color getFeatureColor(String featurename) {
        return settings.getFeatureColor(featurename);
    }    


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        String dataname=rows[rowIndex];
        if (collateType==Motif.class) {
            if (columnIndex==0) return getFeatureColor(dataname);
            else if (columnIndex==1) return dataname; // ID
            else if (columnIndex==2) return getMotifName(dataname); // Name
            else if (columnIndex==3) return getMotifClass(dataname); // Class
            else if (columnIndex==columnnames.length-1) return getMotif(dataname); // this is for the logo
            else columnIndex-=PREFIX_COLUMNS_FOR_MOTIF;
        } else if (collateType==Module.class) {
            if (columnIndex==0) return getFeatureColor(dataname);
            else if (columnIndex==1) return dataname; // ID
            else if (columnIndex==columnnames.length-1) return getModule(dataname); // this is for the logo
            else columnIndex-=PREFIX_COLUMNS_FOR_MODULE;
        } else {
            if (columnIndex==0) return getSequence(dataname);
            else columnIndex-=PREFIX_COLUMNS_FOR_SEQUENCE;
        }
        // not returned yet? Then the column refers to one in the tabledata
        return getCellData(columnIndex,dataname); // this value could be an AnnotatedValue or a "raw" value
    }

//    public Object getCellDataAt(int rowIndex, int columnIndex) {
//        String dataname=rows[rowIndex];
//        if (collateType==Motif.class) {
//            if (columnIndex==0) return getFeatureColor(dataname);
//            else if (columnIndex==1) return dataname; // ID
//            else if (columnIndex==2) return getMotifName(dataname); // Name
//            else if (columnIndex==3) return getMotifClass(dataname); // Class
//            else if (columnIndex==columnnames.length-1) return getMotif(dataname); // this is for the logo
//            else columnIndex-=PREFIX_COLUMNS_FOR_MOTIF;
//        } else if (collateType==Module.class) {
//            if (columnIndex==0) return getFeatureColor(dataname);
//            else if (columnIndex==1) return dataname; // ID
//            else if (columnIndex==columnnames.length-1) return getModule(dataname); // this is for the logo
//            else columnIndex-=PREFIX_COLUMNS_FOR_MODULE;
//        } else {
//            if (columnIndex==0) return getSequence(dataname);
//            else columnIndex-=PREFIX_COLUMNS_FOR_SEQUENCE;
//        }
//        // not returned yet? Then the column refers to one in the tabledata
//        return getCellData(columnIndex,dataname); // the value including markup.
//    }

    @Override
    public String getColumnName(int column) {
        return columnnames[column];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	return false;
    }

    @Override
    public int getColumnCount() {
        return columnnames.length;
    }

    @Override
    public int getRowCount() {
        return rows.length;
    }

}

private class CellRenderer_MotifName extends DefaultTableCellRenderer {
    MotifLabEngine engine;
    public CellRenderer_MotifName(MotifLabEngine engine) {
       super();
       this.engine=engine;
    }
    @Override
    public void setValue(Object value) {
        // NB the value here is not the motifID but the short name. That is why it fails!!!
        setText((String)value);
        Data data=engine.getDataItem((String)value);
        if (data instanceof Motif) {
            setToolTipText(((Motif)data).getLongName());
        }
    }
}

private class CellRenderer_Classification extends DefaultTableCellRenderer {
    public CellRenderer_Classification() {
       super();
    }
    @Override
    public void setValue(Object value) {
       setText((String)value);
       setToolTipText(MotifClassification.getFullLevelsStringAsHTML((String)value));
    }
}



    // ------------ Serialization ---------
    private static final long serialVersionUID = 1L;

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
         short currentinternalversion=1; // this is an internal version number for serialization of objects of this type
         out.writeShort(currentinternalversion);
         out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
         short currentinternalversion=in.readShort(); // the internalversion number is used to determine correct format of data
         if (currentinternalversion==1) {
             in.defaultReadObject();
         } else if (currentinternalversion>1) throw new ClassNotFoundException("Newer version");
    }
}
