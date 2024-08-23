/*
 
 
 */

package motiflab.engine.data.analysis;

import de.erichseifert.vectorgraphics2d.EPSGraphics2D;
import de.erichseifert.vectorgraphics2d.PDFGraphics2D;
import de.erichseifert.vectorgraphics2d.SVGGraphics2D;
import de.erichseifert.vectorgraphics2d.VectorGraphics2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import motiflab.engine.data.*;
import java.util.ArrayList;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterExporter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.dataformat.DataFormat_Excel;
import motiflab.engine.dataformat.DataFormat_HTML;
import motiflab.engine.dataformat.DataFormat_RawData;
import motiflab.gui.ModuleLogo;
import motiflab.gui.OutputPanel;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.MotifLogo;
import motiflab.gui.VisualizationSettings;
import motiflab.gui.prompt.Prompt;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * The parent class of all Analysis data objects
 * @author kjetikl
 */
public abstract class Analysis extends Data implements ParameterExporter {

    private static final long serialVersionUID  = -7466671501926581281L; // for backwards compatibility
        
    private static final String MOTIF_LOGO_NO="No";
    private static final String MOTIF_LOGO_NEW="New images";
    private static final String MOTIF_LOGO_SHARED="Shared images";
    private static final String MOTIF_LOGO_EMBEDDED="Embedded images";
    private static final String MOTIF_LOGO_INCLUDE="Yes";    
    private static final String MOTIF_LOGO_TEXT="Text";

    public static final String HTML=DataFormat_HTML.HTML;
    public static final String RAWDATA=DataFormat_RawData.RAWDATA;
    public static final String EXCEL=DataFormat_Excel.EXCEL;    

    protected transient ArrayList<Parameter> parameterFormats=new ArrayList<Parameter>();
    protected String name; 
    private static String typedescription="Analysis";
    protected final static int sequencelogoSize=24;

    
    @Override
    public String getName() {return name;} // the name of the data object
    
    /** Returns the name of the analysis performed by this Object
     * (e.g. motifcount or distribution). Note that it does not return
     * the unique name of the data object (use getName() for that)
     */
    public abstract String getAnalysisName(); // the name of the analysis (e.g. 'count' or 'distribution'
 
    
    /**
     * Returns a short textual description of this analysis
     * @return
     */
    public String getDescription() {return "";}
    
    
    /**
     * Returns an URL referring to a 'help page' document for this analysis
     * or a String containing the 'help page' document itself.
     * The help-page describes the purpose of the analysis and its various parameters
     * @return
     */

    public Object getHelp(MotifLabEngine engine) {
        if (engine!=null) {
            try {
               String safename=getAnalysisName().replace(" ", "_");
               return new java.net.URL(engine.getWebSiteURL()+"getHelp.php?type=Analysis&topic="+safename);
            } catch (Exception e) {}            
        }
        return null;
    }    
    
    /**
     * Returns a list of applicable DataFormat names for the output parameter with the given name.
     * If the returned list is non-null and non-empty, it means that the output parameter is only
     * relevant for the DataFormats included in this list
     * @param parameter
     * @return 
     */
    @Deprecated
    public String[] getOutputParameterFilter(String parameter) {
        return null;
    }    
    
    /** 
     * Performs the analysis
     */    
    public abstract void runAnalysis(OperationTask task) throws Exception;

    
    /** 
     * Returns the name of one or more Parameters which can be used as a proxys for a source
     * parameter. Note that if several parameters are returned they should be of different types
     */    
    public String[] getSourceProxyParameters() {return new String[0];}
  
    /** Returns TRUE if the given data object can be used as a source proxy for this analysis */
    @SuppressWarnings("unchecked")
    public boolean canUseAsSourceProxy(Data object) {
        String[] proxys=getSourceProxyParameters();
        if (proxys==null) return false;
        for (String proxy:proxys) {
            Parameter param=getParameterWithName(proxy);
            if (param==null) return false;
            Class paramtype=param.getType();
            if (paramtype==null) return false;
            if (paramtype.isAssignableFrom(object.getClass())) return true;
        }
        return false;
    }
    
    /** Returns TRUE if a data object of the given class can be used as a source proxy for this analysis */
    @SuppressWarnings("unchecked")    
    public boolean canUseAsSourceProxy(Class typeclass) {
        String[] proxys=getSourceProxyParameters();
        if (proxys==null) return false;        
        for (String proxy:proxys) {
            Parameter param=getParameterWithName(proxy);
            if (param==null) return false;
            Class paramtype=param.getType();
            if (paramtype==null) return false;
            if (paramtype.isAssignableFrom(typeclass)) return true;
        }
        return false;
    }    
    
    @Override
    public void rename(String newname) {
        this.name=newname;
    }  

    /** 
     * Returns the Class of the type of data objects that this analysis can export for collation
     * or NULL if the analysis can not export data
     */
    public Class getCollateType() {
        return null;
    }

    /** Returns the names of columns that can be collated */
    public String[] getColumnsExportedForCollation() {
        return new String[0];
    }
    
    /** This method can be used to retrieve data from analyses that export data for collation      
     */
    public HashMap<String,Object> getColumnData(String column) throws ExecutionError {
        throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
    }
    
    /** This method can be used to retrieve information about columns from analyses that export data for collation      
     *  If the return type is NULL it means that the analysis does not export data from a column with the given name
     */
    public Class getColumnType(String column) {
        return null; // Default behaviour. This should be overridden in subclasses as necessary
    }    
    
    @Override
    public Object getValue() {
        return this;
    }
    
    @Override
    public String getValueAsParameterString() {
        return "N/A";
    }

    @Override
    public void importData(Data source) throws ClassCastException {
        this.name=((Analysis)source).name;
    }

    public static String getType() {return typedescription;}

    @Override
    public String getTypeDescription() {return "Analysis: "+getAnalysisName();}   

    /**
     * Output the data from this analysis in "raw data" format (plain text)
     * @param outputobject
     * @param engine
     * @param settings
     * @param task
     * @param format
     * @return
     * @throws ExecutionError
     * @throws InterruptedException 
     */
    public abstract OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException;

    /**
     * Output the data from this analysis in HTML format.
     * Subclasses of Analysis should override this method to output analysis-specific output. 
     * If the method is not overridden by the subclass, the default superclass implementation will base the HTML-output on the raw data created by calling formatRaw()
     * The following formatting rules will then apply:
     *     - If the first line of text starts with # it will be output as a header using the <H1> tag (without the # prefix) unless it also contains TABs
     *     - All lines containing TAB separated columns will be considered as "table rows" and formatted as HTML tables
     *     - table lines starting with # (i.e. lines starting with # and containing TABs) are considered as "table headers" and will be formatted using the <th> tag
     *     - All other lines will just be output as plain text (if they start with # this prefix will be removed)
     * 
     * @param outputobject
     * @param engine
     * @param settings
     * @param task
     * @param format
     * @return
     * @throws ExecutionError
     * @throws InterruptedException 
     */
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException{
        // The default implementation just reuses output from formatRaw() and converts it to simple HTML
        engine.createHTMLheader(getAnalysisName(), null, null, false, true, true, outputobject);
        OutputData rawdata=new OutputData();
        rawdata = formatRaw(rawdata, engine, settings, task, format);
        ArrayList<String> strings=rawdata.getContentsAsStrings();
        if (strings.isEmpty()) return outputobject;
        int index=0;
        // Check if the first line could be a title header and output this in an <H1> element. Tab-separated headers are turned into <th> table-headers below.
        if (strings.get(0).startsWith("#") && !strings.get(0).contains("\t")) { 
            outputobject.append("<H1>"+strings.get(0).substring(1) +"</H1>", HTML); // first line is a header!            
            index=1;
        }
        boolean renderingTable=false;
        for (int i=index;i<strings.size();i++) {
            if (i%10==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                task.setStatusMessage("Executing operation: output ("+i+"/"+strings.size()+")");
                format.setProgress(i, strings.size()); // 
                if (i%100==0) Thread.yield();
            }            
            String line=strings.get(i);
            if (line.contains("\t")) { // table row
                  if (!renderingTable) { // start new table if necessary
                      renderingTable=true;
                      outputobject.append("<table>\n", HTML);
                  }
                  boolean header=false;
                  if (line.startsWith("#")) {header=true;line=line.substring(1);}
                  if (header) {line=line.replaceAll("\t", "</th><th>");line="<tr><th>"+line+"</th></tr>\n";}
                  else {line=line.replaceAll("\t", "</td><td>");line="<tr><td>"+line+"</td></tr>\n";}  
                  outputobject.append(line, HTML);
            } else { // not a table row. 
                if (renderingTable) { // End current table if necessary
                    outputobject.append("</table>\n", HTML);
                    renderingTable=false;
                }
                if (line.startsWith("#")) {line=line.substring(1);}
                outputobject.append(line+"<br>\n", HTML);
            }
        }          
        if (renderingTable) {outputobject.append("</table>\n", HTML);} // end open table
        return outputobject;
    }
    
   /** Output the data from this analysis in Excel format.
     * Subclasses of Analysis should override this method to output analysis-specific output. 
     * If the method is not overridden by the subclass, the default superclass implementation will base the Excel-output on the raw data created by calling formatRaw()
     * The following formatting rules will then apply:
     *     - If the first line of text starts with # it will be output as a header in a large font (without the # prefix) unless it also contains TABs
     *     - Lines containing TAB separated columns will also be divided across columns in the Excel sheet. 
     *       Cells containing numbers or text, respectively, will be formatted as such. As will numbers followed by a percentage sign.
     *     - Table lines starting with # (i.e. lines starting with # and containing tabs) are considered as "table headers" and will be formatted with light yellow background and black borders
     *     - All other lines will just be output as plain text in one column (if they start with # this prefix will be removed)
     * @param outputobject
     * @param engine
     * @param settings
     * @param task
     * @param format
     * @return
     * @throws ExecutionError
     * @throws InterruptedException 
     */
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        OutputData rawdata=new OutputData();
        rawdata = formatRaw(rawdata, engine, settings, task, format);
        ArrayList<String> strings=rawdata.getContentsAsStrings();

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet();

        Color lightYellow=new Color(255,255,200);
        CellStyle title=getExcelTitleStyle(workbook);             
        CellStyle tableheader=getExcelTableHeaderStyle(workbook);
        CellStyle stylePercentage = workbook.createCellStyle(); // default stype
        stylePercentage.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        CellStyle tableheaderPercentage=createExcelStyle(workbook, BorderStyle.THIN, lightYellow, HorizontalAlignment.CENTER, true); 
        tableheaderPercentage.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        
        boolean hasTitle=false;
        int index=0;
        int rownum=0;        
        Row row = null;
        ArrayList<Object[]> extraLines=new ArrayList<>(); // this will hold lines that will be output in a second pass, after the columns have been scaled
        String outputTitle=null;
        // Check if the first line could be a title header
        if (strings.size()>0 && strings.get(0).startsWith("#") && !strings.get(0).contains("\t")) { 
            row = sheet.createRow(rownum); 
            outputTitle=strings.get(0).substring(1); 
            outputStringValueInCell(row, 0, " ", title); // this is just a placeholder for now        
            index=1;
            rownum++;
            hasTitle=true;
        }
        int maxcol=0;
        for (int i=index;i<strings.size();i++) {
            row = sheet.createRow(rownum);
            if (i%10==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                task.setStatusMessage("Executing operation: output ("+i+"/"+strings.size()+")");
                format.setProgress(i, strings.size()); // 
                if (i%100==0) Thread.yield();
            }            
            String line=strings.get(i);
            if (line.contains("\t")) { // table row
                  boolean header=false;
                  if (line.startsWith("#")) {header=true;line=line.substring(1);}
                  String[] parts=line.split("\t");
                  if (parts.length>maxcol) maxcol=parts.length;
                  for (int col=0;col<parts.length;col++) {
                      if (parts[col].endsWith("%")) { // this could be a number with percentage sign suffix. If so, format it as a percentage number
                            try { // is it a number
                              String valueString=parts[col].substring(0,parts[col].length()-1); // strip suffix
                              double value=Double.parseDouble(valueString);
                              value=value/100; // Divide by 100 here. The style will format it correctly in the output
                              outputNumericValueInCell(row, col, value, (header)?tableheaderPercentage:stylePercentage);
                          } catch (NumberFormatException e) {
                              // not a percentage number. Output as normal
                              outputStringValueInCell(row, col, parts[col], (header)?tableheader:null);
                          }                        
                          continue;
                      }
                      try { // is it a number
                          double value=Double.parseDouble(parts[col]);
                          outputNumericValueInCell(row, col, value, (header)?tableheader:null);
                      } catch (NumberFormatException e) {
                          outputStringValueInCell(row, col, parts[col], (header)?tableheader:null);
                      }
                  } 
            } else { // not a table row. Just output as one column 
                if (line.startsWith("#")) line=line.substring(1);
                outputStringValueInCell(row, 0, " ", null); // if this line is long, it can mess up the auto-size of the data columns, so I will add a placeholder here and add the real text later
                extraLines.add(new Object[]{line,rownum}); // store the text and linenumber so I can add the text back in a second pass later
            }
            rownum++;
        }          
        
        autoSizeExcelColumns(sheet, 0, maxcol-1, 800);
        if (outputTitle!=null) { // add title back after resizing columns to fit
           sheet.getRow(0).getCell(0).setCellValue(outputTitle);
        }
        
        if (extraLines!=null) { // add back comment lines that could have messed up resizing
            for (Object[] line:extraLines) {
                String text=(String)line[0];
                int rowIndex=(int)line[1];
                sheet.getRow(rowIndex).getCell(0).setCellValue(text);
            }
        }
        format.setProgress(95);  
        
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
        return outputobject;   
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
    
    
    public void cloneCommonSettings(Analysis analysis) {
        analysis.name=this.name;     
    }
    
    /**
     * Adds a regular parameter to the Analysis. This is used for initialization of Analysis objects and should only be called in a constructor or similar setup method
     */
    protected final void addParameter(String parameterName, Class type ,Object defaultValue, Object[] allowedValues, String description, boolean required, boolean hidden) {
        parameterFormats.add(new Parameter(parameterName,type,defaultValue,allowedValues,description,required,hidden));
    }    
    /**
     * Adds a regular parameter to the Analysis. This is used for initialization of Analysis objects and should only be called in a constructor or similar setup method
     */
    protected final void addParameter(String parameterName, Class type ,Object defaultValue, Object[] allowedValues, String description, boolean required, boolean hidden, boolean advanced) {
        parameterFormats.add(new Parameter(parameterName,type,defaultValue,allowedValues,description,required,hidden,advanced));
    }      
    
    /** Returns a list of output parameters that can be set when an Analysis is output
     * @param dataformat The name of the dataformat in which the analysis will be output, e.g. HTML, Excel or RawData. This can be used to filter which parameters are shown
     */
    public Parameter[] getOutputParameters(String dataformat) {
        return new Parameter[0];
    }
    
    public Parameter[] getOutputParameters(DataFormat dataformat) {
        if (dataformat!=null) return getOutputParameters(dataformat.getName());
        else return new Parameter[0];
    }    

    // This will probably not be needed here anyway since analyses are immutable
    @Override
    public boolean containsSameData(Data other) {
        return other==this;
    }

    /**
     * Returns a GUI dialog capable of displaying the results of this analysis
     * This is a generic JDialog with a main panel and a CLOSE button.
     * The contents of the main panel is obtained through a call to getDisplayDialogPanel()
     * which should be overridden by specific Analysis subclasses
     */
    public Prompt getPrompt(MotifLabGUI gui, boolean modal) {
        AnalysisPrompt prompt=new AnalysisPrompt(gui, getDisplayPanel(gui, modal), modal);
        return prompt;
    }


    /**
     * Returns a JPanel that displays the contents of the Analysis.
     * The default implementation returns a panel containing the HTML-formatted
     * output of this Analysis. Specific Analysis-subclasses can override this method
     * to return better displays (even interactive ones).
     * @param gui
     * @return
     */
    protected JPanel getDisplayPanel(MotifLabGUI gui, boolean modal) {
        OutputData document=new OutputData("temp");
        try {document=formatHTML(document, gui.getEngine(), null, null, null);}
        catch (Exception e) {document.append("ERROR:"+e.getMessage(), HTML);e.printStackTrace(System.err);}
        document.setShowAsHTML(true);
        OutputPanel panel=new OutputPanel(document, gui);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8,5,0,5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)
        ));
        panel.setPreferredSize(getDefaultDisplayPanelDimensions());
        return panel;
    }

    protected Dimension getDefaultDisplayPanelDimensions() {
        return new Dimension(800,600);
    }
    
    private class AnalysisPrompt extends Prompt {
        public AnalysisPrompt(MotifLabGUI gui, JPanel panel, boolean modal) {
            super(gui, null, modal);
            String title=Analysis.this.getName()+"  ("+getAnalysisName()+")";
            setTitle(title);
            setMainPanel(panel);
            setPromptHeaderVisible(false);
            pack();
            focusOKButton();
        }

        @Override
        public Data getData() {
            return Analysis.this;
        }
        
        @Override
        public void setData(Data newdata) {
           return; 
        }         
        

        @Override
        public boolean onOKPressed() {
            return true;
        }

    }
    
    /**
     * Returns a list of options for how to handle motif logos
     * @param dataformat The name of the format that the analysis will be output in, e.g. Excel or HTML
     * @return 
     */
    public static String[] getMotifLogoOptions(String dataformat) {
        if (dataformat.equalsIgnoreCase(HTML)) return new String[]{MOTIF_LOGO_NO,MOTIF_LOGO_NEW,MOTIF_LOGO_SHARED,MOTIF_LOGO_TEXT};        
        else if (dataformat.equalsIgnoreCase(EXCEL))   return new String[]{MOTIF_LOGO_NO,MOTIF_LOGO_INCLUDE,MOTIF_LOGO_TEXT};
        else if (dataformat.equalsIgnoreCase(RAWDATA)) return new String[]{MOTIF_LOGO_NO,MOTIF_LOGO_TEXT};        
        else return new String[]{""};
    }
    
    /**
     * Returns the default option for how to output motif/module logos for the given data format
     * @param dataformat
     * @return 
     */
    public static String getMotifLogoDefaultOption(String dataformat) {
        if (dataformat.equalsIgnoreCase(RAWDATA)) return MOTIF_LOGO_TEXT;        
        else return MOTIF_LOGO_NO;
    }    
    
    /**
     * Returns true if the selection option means that a motif/module logo should be included in the output
     * either as an image or a textual representation
     * @param option
     * @return 
     */
    public static boolean includeLogosInOutput(String option) {
        return (option.equalsIgnoreCase(MOTIF_LOGO_NEW) || option.equalsIgnoreCase(MOTIF_LOGO_SHARED) || option.equalsIgnoreCase(MOTIF_LOGO_INCLUDE) || option.equalsIgnoreCase(MOTIF_LOGO_TEXT));
    }
    
    /**
     * Returns true if the selection option means that a motif/module logo should be included in the output
     * as regular text rather than an image
     * @param option
     * @return 
     */
    public static boolean includeLogosInOutputAsText(String option) {
        return (option.equalsIgnoreCase(MOTIF_LOGO_TEXT));
    }    
    
    /**
     * Returns true if the selection option means that a motif/module logo should be included in the output as an image
     * @param option
     * @return 
     */
    public static boolean includeLogosInOutputAsImages(String option) {
        return (option.equalsIgnoreCase(MOTIF_LOGO_NEW) || option.equalsIgnoreCase(MOTIF_LOGO_SHARED) || option.equalsIgnoreCase(MOTIF_LOGO_INCLUDE));
    }
    
    /**
     * Returns true if the selection option means that a motif/module logo should be included in the output as a shared image
     * (i.e. the name of the image file is based on the data object, and it is not recreated if it already exists)
     * @param option
     * @return 
     */
    public static boolean includeLogosInOutputAsSharedImages(String option) {
        return (option.equalsIgnoreCase(MOTIF_LOGO_SHARED));
    }     

    /** Creates a motif logo image for the given motif, saves it to a temp-file and return an IMG-tag that can
     *  be inserted in HTML-documents to display the image
     */
    protected String getMotifLogoTag(Motif motif, OutputData outputobject, MotifLogo sequencelogo, String logoFormat, MotifLabEngine engine) {
        int height=19; // an image height of 19 corresponds with a logo height of 22 which is "hardcoded" above (but probably should not be)
        if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_NO)) return "";
        else if (motif==null) return "?";
        else if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_TEXT)) return motif.getConsensusMotif();
        else { // logo as image
            VisualizationSettings settings=engine.getClient().getVisualizationSettings();
            String imageFormat=(String)settings.getSettingAsType("motif.imageFormat","gif");
            boolean border=(Boolean)settings.getSettingAsType("motif.border",Boolean.TRUE);        
            boolean scaleByIC=(Boolean)settings.getSettingAsType("motif.scaleByIC",Boolean.TRUE);                
//            height=(Integer)settings.getSettingAsType("motif.height", new Integer(19));  
            imageFormat=imageFormat.toLowerCase();
            if (!(imageFormat.equals("gif") || imageFormat.equals("png"))) imageFormat="gif";            
            int width=0;
            File imagefile=null;
            if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_SHARED)) {
                String logofileID=motif.getName();
                boolean sharedDependencyExists=(engine.getSharedOutputDependency(logofileID)!=null);
                OutputDataDependency dependency=outputobject.createSharedDependency(engine,logofileID, imageFormat, true); // returns new or existing shared dependency
                if (!sharedDependencyExists) { // the dependency has not been created before so we must save the image to file
                    imagefile=dependency.getFile();
                    sequencelogo.setMotif(motif);
                    width=sequencelogo.getDefaultMotifWidth();
                    try {
                        saveMotifLogoImage(imagefile,sequencelogo, height, border, imageFormat); // an image height of 19 corresponds with a logo height of 22 which is "hardcoded" above (but probably should not be)
                    } catch (IOException e) {
                        engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
                    }
                } else {
                    imagefile=new File(dependency.getInternalPathName());
                }            
            } else { // always save any logo to a new file
                imagefile=outputobject.createDependentFile(engine,imageFormat);
                sequencelogo.setMotif(motif);
                sequencelogo.setScaleByIC(scaleByIC);
                width=sequencelogo.getDefaultMotifWidth();
                try {              
                    saveMotifLogoImage(imagefile,sequencelogo, height, border, imageFormat); // an image height of 19 corresponds with a logo height of 22 which is "hardcoded" above (but probably should not be)
                } catch (IOException e) {
                    engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
                }           
            }           
            // specifying the size makes it look weird in internal HTML-browser and it does not seem to make much difference in external browsers, so I will just drop it.
            // String sizeString=(width>0)?(" height="+height+" width="+width):"";
            return "<img src=\"file:///"+imagefile.getAbsolutePath()+"\"/>";
        }
    }

    private void saveMotifLogoImage(File file, MotifLogo sequencelogo, int motifheight, boolean border, String imageFormat) throws IOException {
        int width=sequencelogo.getDefaultMotifWidth();
        BufferedImage image=new BufferedImage(width, motifheight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, motifheight);
        sequencelogo.setDrawBorder(false); // I will paint the border myself to avoid problems
        sequencelogo.paintLogo(g);
        if (border) {
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, width-1, motifheight-1);
        }
        if (imageFormat==null) imageFormat="gif";
        imageFormat=imageFormat.toLowerCase();
        if (!(imageFormat.equals("gif") || imageFormat.equals("png"))) imageFormat="gif";
        OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
        ImageIO.write(image, imageFormat, output);
        output.close();               
        g.dispose();
    }

    public byte[] getMotifLogoImageAsByteArray(MotifLogo sequencelogo, int motifheight, boolean border, String imageFormat) throws IOException {
        int width=sequencelogo.getDefaultMotifWidth();
        BufferedImage image=new BufferedImage(width, motifheight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, motifheight);
        sequencelogo.setDrawBorder(false); // I will paint the border myself to avoid problems
        sequencelogo.paintLogo(g);
        if (border) {
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, width-1, motifheight-1);
        }
        org.apache.commons.io.output.ByteArrayOutputStream outputStream=new org.apache.commons.io.output.ByteArrayOutputStream();
        ImageIO.write(image, imageFormat, outputStream);
        g.dispose();     
        byte[] array=outputStream.toByteArray();
        outputStream.close();
        return array;
    }
    
    public byte[] getModuleLogoImageAsByteArray(ModuleLogo modulelogo, int moduleheight, boolean border, String imageFormat) throws IOException {
        Font modulemotiffont=ModuleLogo.getModuleMotifFont();
        BufferedImage test=new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        FontMetrics modulelogometrics=test.getGraphics().getFontMetrics(modulemotiffont);
        int width=ModuleLogo.getLogoWidth(modulelogometrics,modulelogo.getModule())+2;
        BufferedImage image=new BufferedImage(width, moduleheight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.setBackground(new Color(255, 255, 255, 0)); // make the image translucent white       
        g.clearRect(0,0, width+2, moduleheight+2); // bleed a little just in case
        modulelogo.paintModuleLogo(g,0,0);
        if (border) {
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, width-1, moduleheight-1);
        }
        org.apache.commons.io.output.ByteArrayOutputStream outputStream=new org.apache.commons.io.output.ByteArrayOutputStream();
        ImageIO.write(image, imageFormat, outputStream);
        g.dispose();     
        byte[] array=outputStream.toByteArray();
        outputStream.close();
        return array;
    }    
    
    protected String getModuleLogoTag(Module module, OutputData outputobject, String logoFormat, MotifLabEngine engine) {
        if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_NO)) return "";
        else if (module==null) return "?";        
        else if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_TEXT)) return escapeHTML(module.getModuleLogo());
        else { // logo as image
            VisualizationSettings settings=engine.getClient().getVisualizationSettings();
            String imageFormat=(String)settings.getSettingAsType("module.imageFormat","png");
            boolean border=(Boolean)settings.getSettingAsType("module.border",Boolean.TRUE);            
//            height=(Integer)settings.getSettingAsType("module.height", new Integer(19)); 
            imageFormat=imageFormat.toLowerCase();
            if (!(imageFormat.equals("gif") || imageFormat.equals("png") || imageFormat.equals("svg"))) imageFormat="png";            
            
            int height=0,width=0;
            File imagefile=null;
            if (logoFormat.equalsIgnoreCase(MOTIF_LOGO_SHARED)) {
                String logofileID=module.getName();
                boolean sharedDependencyExists=(engine.getSharedOutputDependency(logofileID)!=null);
                OutputDataDependency dependency=outputobject.createSharedDependency(engine,logofileID, imageFormat,true); // returns new or existing shared dependency
                if (!sharedDependencyExists) { // the dependency has not been created before so we must save the image to file
                    imagefile=dependency.getFile();
                    try {
                        int[] size=savModuleLogoImage(imagefile,module,engine.getClient().getVisualizationSettings(), border, imageFormat); //
                        height=size[0];
                        width=size[1];
                    } catch (IOException e) {
                        engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
                    }
                } else {
                    imagefile=new File(dependency.getInternalPathName());
                }
            } else { // always save any logo to a new file
                imagefile=outputobject.createDependentFile(engine,imageFormat);
                try {
                    int[] size=savModuleLogoImage(imagefile,module,engine.getClient().getVisualizationSettings(), border, imageFormat); //
                    height=size[0];
                    width=size[1];                
                } catch (IOException e) {
                    engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
                }
            }     
            // specifying the size makes it look weird in internal HTML-browser and it does not seem to make much difference in external browsers, so I will just drop it.
            // String sizeString=(width>0)?(" height="+height+" width="+width):"";
            return "<img src=\"file:///"+imagefile.getAbsolutePath()+"\"/>";            
        }
                    
    }
    

    private int[] savModuleLogoImage(File file, Module module, VisualizationSettings settings, boolean border, String imageFormat) throws IOException {
        Font modulemotiffont=ModuleLogo.getModuleMotifFont();
        BufferedImage test=new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        FontMetrics modulelogometrics=test.getGraphics().getFontMetrics(modulemotiffont);
        int width=ModuleLogo.getLogoWidth(modulelogometrics,module)+2;
        int height=28; // I think this will be OK...
        if (imageFormat==null) imageFormat="png";
        imageFormat=imageFormat.toLowerCase();
        if (!(imageFormat.equals("gif") || imageFormat.equals("png") || imageFormat.equals("svg"))) imageFormat="png";            
        
        if (imageFormat.equals("gif") || imageFormat.equals("png")) {
            BufferedImage image=new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=image.createGraphics();
            g.setBackground(new Color(255, 255, 255, 0)); // make the image translucent white       
            g.clearRect(0,0, width+2, height+2); // bleed a little just in case
            ModuleLogo.paintModuleLogo(g, module, 5, 7, settings, null,0); //
            OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
            ImageIO.write(image, imageFormat, output);
            output.close(); 
            g.dispose();
            return new int[]{height,width};         
        } else {                     
            VectorGraphics2D g=null;
                 if (imageFormat.equals("svg")) g = new SVGGraphics2D(0, 0, width, height);
            else if (imageFormat.equals("pdf")) g = new PDFGraphics2D(0, 0, width, height);
            else if (imageFormat.equals("eps")) g = new EPSGraphics2D(0, 0, width, height);
            else throw new IOException("Unknown image format: "+imageFormat);
            g.setClip(0, 0, width,height);
            ModuleLogo.paintModuleLogo(g, module, 5, 7, settings, null,0); //                              
            FileOutputStream fileStream = new FileOutputStream(file);
            try {
                fileStream.write(g.getBytes());
            } finally {
                fileStream.close();
            } 
            g.dispose();  
            return new int[]{height,width};     
        }                       
        
    }

    protected String escapeHTML(String string) {
        if (string==null) return "";
        if (string.contains("&")) string=string.replace("&", "&amp;"); // this must be first
        if (string.contains("<")) string=string.replace("<", "&lt;");
        if (string.contains(">")) string=string.replace(">", "&gt;");
        return string;
    }    
    
    
// -------------- Convenience methods for exporting to EXCEL format -----------------
    
    protected void outputStringValuesInCells(Row row, String[] values, int start) {
       outputStringValuesInCells(row, values, start, null); 
    }    
    
    protected void outputStringValuesInCells(Row row, String[] values, int start, CellStyle style) {
        for (int j=0;j<values.length;j++) {
            outputStringValueInCell(row,j+start,values[j],style);
        }
    }
    
    protected void outputStringValueInCell(Row row, int column, String value, CellStyle style) {
        Cell cell=row.getCell(column);
        if (cell==null) cell=row.createCell(column);
        cell.setCellValue(value);
        if (style!=null) cell.setCellStyle(style);               
    }   
    
    protected void outputRichStringValueInCell(Row row, int column, RichTextString value, CellStyle style) {
        Cell cell=row.getCell(column);
        if (cell==null) cell=row.createCell(column);
        cell.setCellValue(value);
        if (style!=null) cell.setCellStyle(style);               
    }       
    
    protected void outputNumericValuesInCells(Row row, double[] values, int start) {
       outputNumericValuesInCells(row, values, start, null); 
    }  
    
    protected void outputNumericValuesInCells(Row row, double[] values, int start, CellStyle style) {
        for (int j=0;j<values.length;j++) {
            outputNumericValueInCell(row, j+start, values[j], style);
        }
    }  
    protected void outputNumericValueInCell(Row row, int col, double value, CellStyle style) {
        Cell cell=row.createCell(col);
        if (!Double.isNaN(value)) cell.setCellValue(value);
        if (style!=null) cell.setCellStyle(style);
    }       
    
    protected CellStyle createExcelStyle(XSSFWorkbook workbook, BorderStyle border, Color backgroundColor, HorizontalAlignment alignment, boolean bold) {
        CellStyle style = workbook.createCellStyle();
        if (border!=null) {
            style.setBorderBottom(border);  
            style.setBorderTop(border);
            style.setBorderLeft(border);  
            style.setBorderRight(border);  
        }
        if (backgroundColor!=null) {            
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setFillForegroundColor(new XSSFColor(backgroundColor,null)); 
        }
        if (alignment!=null) style.setAlignment(alignment);
        if (bold) {
            addFontToExcelCellStyle(workbook, style, null, (short)0, true, false);
        }
        return style;
    }
    
    protected CellStyle createExcelStyle(XSSFWorkbook workbook, BorderStyle border, Color foregroundColor, Color backgroundColor, HorizontalAlignment alignment, VerticalAlignment valignment) {
        CellStyle style = workbook.createCellStyle();
        if (border!=null) {
            style.setBorderBottom(border);  
            style.setBorderTop(border);
            style.setBorderLeft(border);  
            style.setBorderRight(border);  
        }
        if (foregroundColor!=null) {
            XSSFFont font = workbook.createFont();
            font.setColor(new XSSFColor(foregroundColor,null));
            style.setFont(font);
        }
        if (backgroundColor!=null) {            
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setFillForegroundColor(new XSSFColor(backgroundColor,null)); 
        }
        if (alignment!=null) style.setAlignment(alignment);
        if (valignment!=null) style.setVerticalAlignment(valignment);
        return style;
    }     
    
    
    protected void addFontToExcelCellStyle(XSSFWorkbook workbook, CellStyle style, String fontName, short fontHeight, boolean bold, boolean italics) {
        XSSFFont font = workbook.createFont();
        if (fontName!=null) font.setFontName(fontName);
        if (fontHeight>0) font.setFontHeightInPoints(fontHeight);
        font.setBold(bold);
        font.setItalic(italics);
        style.setFont(font); 
    }
    
    public CellStyle getExcelTitleStyle(XSSFWorkbook workbook) {        
        CellStyle title=createExcelStyle(workbook, BorderStyle.NONE, null, HorizontalAlignment.LEFT, false); 
        addFontToExcelCellStyle(workbook, title, null, (short)(workbook.getFontAt((short)0).getFontHeightInPoints()*2.5), true, false);
        return title;
    }
    
    public CellStyle getExcelHeaderStyle(XSSFWorkbook workbook, double fontScaleFactor, boolean bold, boolean italics) {        
        CellStyle title=createExcelStyle(workbook, BorderStyle.NONE, null, HorizontalAlignment.LEFT, false); 
        addFontToExcelCellStyle(workbook, title, null, (short)(workbook.getFontAt((short)0).getFontHeightInPoints()*fontScaleFactor), bold, italics);
        return title;
    }    
    
    public CellStyle getExcelTableHeaderStyle(XSSFWorkbook workbook) {
        return createExcelStyle(workbook, BorderStyle.THIN, new Color(255,255,200), HorizontalAlignment.CENTER, true); // using light yellow background color
    }

    public CellStyle getExcelPercentageStyle(XSSFWorkbook workbook) {
        CellStyle stylePercentage = workbook.createCellStyle(); // default stype       
        stylePercentage.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        return stylePercentage;
    }
    
    public CellStyle getExcelBorderedStyle(XSSFWorkbook workbook) {
        return createExcelStyle(workbook, BorderStyle.THIN, null, null, false); // using light yellow background color
    }    
    
    public static XDDFSolidFillProperties getExcelFillColor(Color color) {       
        if (color.getAlpha()==255) { // regular opaque color
            XDDFColor scolor=XDDFColor.from(new byte[]{(byte)color.getRed(),(byte)color.getGreen(),(byte)color.getBlue()});
            return new XDDFSolidFillProperties(scolor);        
        } 
//        else { // some transparency is needed, but I don't know how to apply alpha on a regular color :-(
//            int blue  = color.getBlue();
//            int red   = color.getRed();
//            int green = color.getGreen();
//            int alpha = color.getAlpha();              
//            XDDFColor scolor=XDDFColor.from(PresetColor.WHITE);
//            XDDFSolidFillProperties fillProperties = new XDDFSolidFillProperties(scolor);
//            CTSolidColorFillProperties ctSolidColorFillProperties = (CTSolidColorFillProperties)fillProperties.getXmlObject();    
//            CTPresetColor ctPresetColor = ctSolidColorFillProperties.addNewPrstClr();
//            CTPositiveFixedPercentage alphaValue = ctPresetColor.addNewAlpha();
//            CTPercentage blueValue = ctPresetColor.addNewBlue();
//            CTPercentage greenValue = ctPresetColor.addNewGreen();
//            CTPercentage redValue = ctPresetColor.addNewRed();
//            // convert value from 0 (transparent) to 255 (opaque) to 0-1000000
//            blue = (int)(blue*(100000.0/255.0));
//            green = (int)(green*(100000.0/255.0));
//            red = (int)(red*(100000.0/255.0));
//            alpha = (int)(alpha*(100000.0/255.0));             
//            alphaValue.setVal(alpha); // the value range is from 0 (transparent) to 100000 (opaque)
//            blueValue.setVal(blue); //
//            redValue.setVal(red); //  
//            greenValue.setVal(green); //  
//            return fillProperties;
//        }  
        else return new XDDFSolidFillProperties(XDDFColor.from(new byte[]{(byte)color.getRed(),(byte)color.getGreen(),(byte)color.getBlue()})); // fallback without alpha
    }    
    
    protected void autoSizeExcelColumns(XSSFSheet sheet, int startCol, int endCol, int padding) {
        for (int i=startCol; i<=endCol; i++) {
            sheet.autoSizeColumn(i);
            if (padding>0) {
                try {sheet.setColumnWidth(i, sheet.getColumnWidth(i)+padding); }
                catch (java.lang.IllegalArgumentException argX) {}
            }
        }
    }
    
    protected void autoSizeExcelColumns(XSSFSheet sheet, int startCol, int endCol, int padding, int minimum) {
        for (int i=startCol; i<=endCol; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            if (padding>0) {
                int newWidth = width+padding;
                if (newWidth<minimum) newWidth = minimum;
                try {sheet.setColumnWidth(i, newWidth); }
                catch (java.lang.IllegalArgumentException argX) {}
            } else {
                if (width<minimum) try {sheet.setColumnWidth(i,minimum); }
                catch (java.lang.IllegalArgumentException argX) {}
            }
        }
    }    
    
    protected void forceExcelFormulaRecalculation(XSSFWorkbook workbook, XSSFSheet onlyThisSheet) {
        XSSFFormulaEvaluator evaluator = (XSSFFormulaEvaluator) workbook.getCreationHelper().createFormulaEvaluator();
        if (onlyThisSheet!=null) {
            for (Row row : onlyThisSheet) {
               for (Cell cell : row) {
                   if (cell.getCellType() == CellType.FORMULA) evaluator.evaluateFormulaCell(cell);                   
               }
            }           
        } else {
            for (Sheet sheet: workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.FORMULA) evaluator.evaluateFormulaCell(cell);                   
                    }
                }
            }
        }      
    }
    
    private static char[] alphabet=new char[]{'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};
    /**
     * Returns the "name" of a column in an Excel sheet corresponding to the column number (1-indexed!)
     * E.g. Column 1 is A, Column 2 is B, Column 28 is AB, Column 52 is AZ and Column 87 is CI
     * @param columnNumber Starting at 1
     * @return 
     */
    public static String getExcelColumnNameForIndex(int columnNumber) { // 
        StringBuilder columnLetter = new StringBuilder();
        while (columnNumber > 0) {
            int remainder = (columnNumber - 1) % 26;
            columnLetter.insert(0, (char)(remainder + 'A'));
            columnNumber = (columnNumber - 1) / 26;
        }
        return columnLetter.toString();

    }
    
    
}
