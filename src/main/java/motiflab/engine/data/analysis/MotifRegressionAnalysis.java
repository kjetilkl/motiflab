/*
 
 
 */

package motiflab.engine.data.analysis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import javax.swing.JTable;
import motiflab.engine.data.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.Graph;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.TaskRunner;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.operations.Operation_analyze;
import motiflab.gui.GenericMotifBrowserPanel;
import motiflab.gui.MotifLogo;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.VisualizationSettings;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFColor;

/**
 *
 * @author kjetikl
 */
public class MotifRegressionAnalysis extends Analysis {
    private final static String typedescription="Analysis: motif regression";
    private final static String analysisName="motif regression";
    private final static String description="Performs regression analysis of motifs against gene expression (or other sequence related values)";
    private static final String SORT_BY_MOTIF="Motif ID";
    private static final String SORT_BY_REGRESSION_COEFFICIENT="Regression coefficient";
    private static final String SORT_BY_R2_VALUE="R2";
    private HashMap<String,double[]> statistics=null; // key is motif name. Value is double[]
    private String sequenceCollectionName=null;
    private String motifCollectionName=null;    
    private String motifTrackName=null;
    private String sequenceMapName=null;
    private int numberOfSequences=0;
    private boolean ignoreMissing=false;
    private boolean normalize=false;    
    
    // These are indexes into the statistics values (double[])
    private static final int INDEX_MOTIF_TOTAL_COUNT=0;
    private static final int INDEX_MOTIF_TOTAL_SCORE=1;
    private static final int INDEX_MOTIF_SEQUENCE_COUNT=2;
    private static final int INDEX_REGRESSION_COEFFICIENT=3;
    private static final int INDEX_SIGNIFICANCE=4;
    private static final int INDEX_R_SQUARED=5;

    private static final int SORTED_INDEX_MOTIF_ID=0;
    private static final int SORTED_INDEX_MOTIF_TOTAL_COUNT=1;
    private static final int SORTED_INDEX_MOTIF_SEQUENCE_COUNT=2;
    private static final int SORTED_INDEX_REGRESSION_COEFFICIENT=3;
    private static final int SORTED_INDEX_R_SQUARED=4;

    public static final String PARAMETER_MOTIF_TRACK="Motif track";
    public static final String PARAMETER_MOTIFS="Motifs";
    public static final String PARAMETER_SEQUENCES="Sequences";
    public static final String PARAMETER_SEQUENCE_VALUES="Sequence values";
    public static final String PARAMETER_SKIP_NONREGULATED="Skip non-regulated";
    public static final String PARAMETER_NORMALIZE="Normalize";

    public MotifRegressionAnalysis() {
        this.name="MotifRegressionAnalysis_temp";
        addParameter(PARAMETER_MOTIF_TRACK,RegionDataset.class, null,new Class[]{RegionDataset.class},"A track containing motif sites",true,false);
        addParameter(PARAMETER_MOTIFS,MotifCollection.class, null,new Class[]{MotifCollection.class},"The motifs to use for the regression analysis",true,false);
        addParameter(PARAMETER_SEQUENCE_VALUES,SequenceNumericMap.class, null,new Class[]{SequenceNumericMap.class},"A numeric map containing sequence values to regress against (for instance gene expression values)",true,false);
        addParameter(PARAMETER_SEQUENCES,SequenceCollection.class, null,new Class[]{SequenceCollection.class},"If specified, the analysis will be limited to sequence in this collection",false,false);
        addParameter(PARAMETER_SKIP_NONREGULATED,Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Only include sequences were the motif actually occurs in the regression analysis",false,false);
        addParameter(PARAMETER_NORMALIZE,Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Normalize sequence motifscore range to [0,1]",false,false);
    }

    @Override
    public String[] getSourceProxyParameters() {return new String[]{PARAMETER_MOTIF_TRACK,PARAMETER_MOTIFS,PARAMETER_SEQUENCE_VALUES};}
    
    @Override
    public boolean canUseAsSourceProxy(Data data) {
        if (data instanceof RegionDataset) return ((RegionDataset)data).isMotifTrack(); // only allow Motif Tracks as input
        else return (data instanceof MotifCollection || data instanceof SequenceNumericMap);
    }  
    
    @Override
    public Parameter[] getOutputParameters(String dataformat) {
        Parameter incPar  = new Parameter("Include",MotifCollection.class,null,new Class[]{MotifCollection.class},"Only include data from this collection",false,false);              
        Parameter sortPar = new Parameter("Sort by",String.class,SORT_BY_R2_VALUE, new String[]{SORT_BY_MOTIF,SORT_BY_REGRESSION_COEFFICIENT,SORT_BY_R2_VALUE},null,false,false);
        Parameter logos   = new Parameter("Logos",String.class,getMotifLogoDefaultOption(dataformat), getMotifLogoOptions(dataformat),"Include motif sequence logos in the table",false,false);
        Parameter colsPar = new Parameter("Color boxes",Boolean.class,Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a box with the assigned color for the motif will be output as the first column",false,false);
        Parameter legend = new Parameter("Legend",Boolean.class,Boolean.TRUE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a header with a title and analysis details will be included at the top of the Excel sheet.",false,false);                
        if (dataformat.equals(HTML)) return new Parameter[]{incPar,sortPar,logos,colsPar};
        if (dataformat.equals(EXCEL)) return new Parameter[]{incPar,sortPar,logos,legend};
        if (dataformat.equals(RAWDATA)) return new Parameter[]{incPar,sortPar,logos};
        return new Parameter[0];
    }
    
    @Override
    public String[] getOutputParameterFilter(String parameter) {
        if (parameter.equals("Color boxes")) return new String[]{HTML};
        if (parameter.equals("Legend")) return new String[]{EXCEL};    
        if (parameter.equals("Include") || parameter.equals("Sort by") || parameter.equals("Logos")) return new String[]{HTML,RAWDATA,EXCEL};        
        return null;
    }      

    @Override
    public String[] getResultVariables() {
        return new String[]{"Regression coefficient","R squared"};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename==null || variablename.isEmpty()) throw new ExecutionError("'"+getName()+"' does not have a result for ''");
        else if (variablename.equals("Regression coefficient")) {
            MotifNumericMap map=new MotifNumericMap("temp",0);
            for (String motifname:statistics.keySet()) {
                double[] stats=statistics.get(motifname);
                map.setValue(motifname, stats[INDEX_REGRESSION_COEFFICIENT]);
            }
            return map;
        }
        else if (variablename.equals("R squared")) {
            MotifNumericMap map=new MotifNumericMap("temp",0);
            for (String motifname:statistics.keySet()) {
                double[] stats=statistics.get(motifname);
                map.setValue(motifname, stats[INDEX_R_SQUARED]);
            }
            return map;
        }
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else return MotifNumericMap.class;
    }

    @Override
    public String getAnalysisName() {
        return analysisName;
    }

    @Override
    public String getDescription() {return description;}

    @Override
    public Class getCollateType() {
        return Motif.class;
    }

    @Override
    public String[] getColumnsExportedForCollation() {
        return new String[]{"Count","Sequences","Regression coefficient","R squared"};
    }

    @Override
    public Class getColumnType(String column) {
             if (column.equalsIgnoreCase("Count") || column.equalsIgnoreCase("Sequences")) return Integer.class;
        else if (column.equalsIgnoreCase("Regression coefficient") || column.equalsIgnoreCase("R squared")) return Double.class;
        else return null;
    }

    @Override
    public HashMap<String,Object> getColumnData(String column) throws ExecutionError {
        Class coltype=getColumnType(column);
        if (coltype==null) throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
        HashMap<String,Object> columnData=new HashMap<String, Object>();
        for (String motifname:statistics.keySet()) {
            double[] stat=statistics.get(motifname);
                 if (column.equalsIgnoreCase("Count")) columnData.put(motifname, new Integer((int)stat[INDEX_MOTIF_TOTAL_COUNT]));
            else if (column.equalsIgnoreCase("Sequences")) columnData.put(motifname, new Integer((int)stat[INDEX_MOTIF_SEQUENCE_COUNT]));
            else if (column.equalsIgnoreCase("Regression coefficient")) columnData.put(motifname, new Double(stat[INDEX_REGRESSION_COEFFICIENT]));
            else if (column.equalsIgnoreCase("R squared")) columnData.put(motifname, new Double(stat[INDEX_R_SQUARED]));
         }
        return columnData;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MotifRegressionAnalysis clone() {
        MotifRegressionAnalysis newanalysis=new MotifRegressionAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.statistics=new HashMap<String, double[]>(this.statistics.size());
        for (String key:this.statistics.keySet()) {
            newanalysis.statistics.put(key,this.statistics.get(key).clone());
        }
        newanalysis.sequenceCollectionName=this.sequenceCollectionName;
        newanalysis.motifCollectionName=this.motifCollectionName;
        newanalysis.motifTrackName=this.motifTrackName;
        newanalysis.sequenceMapName=this.sequenceMapName;
        newanalysis.numberOfSequences=this.numberOfSequences;
        newanalysis.ignoreMissing=this.ignoreMissing; 
        newanalysis.normalize=this.normalize;          
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        MotifRegressionAnalysis other=((MotifRegressionAnalysis)source);
        this.statistics=(HashMap<String,double[]>)other.statistics;
        this.sequenceCollectionName=other.sequenceCollectionName;
        this.motifCollectionName=other.motifCollectionName;
        this.motifTrackName=other.motifTrackName;
        this.sequenceMapName=other.sequenceMapName;
        this.numberOfSequences=other.numberOfSequences;
        this.ignoreMissing=other.ignoreMissing; 
        this.normalize=other.normalize;          
    }

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

    /** Constructs a sorted list with the motif name, total occurrence count and sequence count
     *  This method is contains code common for both formatHTML and formatRaw
     */
    private ArrayList<Object[]> assembleList(String sortorder, MotifCollection include) {
        ArrayList<Object[]> resultList=new ArrayList<Object[]>(statistics.size());
        Set<String> keys=statistics.keySet();
        Iterator<String> iterator=keys.iterator();
        int i=0;
        while (iterator.hasNext()) {
            i++;
            String motifkey=iterator.next();
            if (include!=null && !include.contains(motifkey)) continue;
            double[] values=statistics.get(motifkey);
            Object[] entry=new Object[5];
            entry[SORTED_INDEX_MOTIF_ID]=motifkey;
            entry[SORTED_INDEX_MOTIF_SEQUENCE_COUNT]=new Double(values[INDEX_MOTIF_SEQUENCE_COUNT]);
            entry[SORTED_INDEX_MOTIF_TOTAL_COUNT]=new Double(values[INDEX_MOTIF_TOTAL_COUNT]);
            entry[SORTED_INDEX_REGRESSION_COEFFICIENT]=new Double(values[INDEX_REGRESSION_COEFFICIENT]);
            entry[SORTED_INDEX_R_SQUARED]=new Double(values[INDEX_R_SQUARED]);
            resultList.add(entry);
        }
        Collections.sort(resultList, new SortOrderComparator(sortorder));
        return resultList;
    }

    @Override
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder=SORT_BY_MOTIF;
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        Color [] basecolors=vizSettings.getBaseColors();
        MotifLogo sequencelogo=new MotifLogo(basecolors,sequencelogoSize);
        String showSequenceLogosString="";
        boolean showColorBoxes=false;
        MotifCollection include=null;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
             showColorBoxes=(Boolean)settings.getResolvedParameter("Color boxes",defaults,engine); 
             include=(MotifCollection)settings.getResolvedParameter("Include",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos=includeLogosInOutput(showSequenceLogosString);

        ArrayList<Object[]> resultList=assembleList(sortorder,include);
        engine.createHTMLheader("Motif Regression Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<h1 class=\"headline\">Motif Regression Analysis</h1>\n",HTML);
        outputobject.append("<div class=\"summary\">\n",HTML);
        outputobject.append("Motif regression analysis with motifs from <span class=\"dataitem\">",HTML);
        outputobject.append(motifCollectionName,HTML);
        outputobject.append("</span> and sites from <span class=\"dataitem\">",HTML);
        outputobject.append(motifTrackName,HTML);
        outputobject.append("</span> on "+numberOfSequences,HTML);
        outputobject.append(" sequence"+((numberOfSequences!=1)?"s":""),HTML);
        if (sequenceCollectionName!=null) {
            outputobject.append(" from Collection <span class=\"dataitem\">",HTML);
            outputobject.append(sequenceCollectionName,HTML);
            outputobject.append("</span>",HTML);
        }
        outputobject.append("<br>Sequence values were taken from <span class=\"dataitem\">",HTML);
        outputobject.append(sequenceMapName,HTML);
        outputobject.append("</span>",HTML);
        if (ignoreMissing) outputobject.append("<br>Only sequences containing hits for a motif were included in the regression analysis for that motif",HTML);
        outputobject.append("</div><br>\n",HTML);
        outputobject.append("<table class=\"sortable\">\n",HTML);
        String logoheader=(showSequenceLogos)?"<th class=\"sorttable_nosort\"> Logo </th>":"";
        outputobject.append("<tr>",HTML);
        if (showColorBoxes) outputobject.append("<th>&nbsp;</th>",HTML);               
        outputobject.append("<th>ID</th><th>Name</th><th>Class</th><th>Total</th><th>Sequences</th><th class=\"sorttable_numeric\">%</th><th>Reg.Coeff</th><th><i>R<sup>2</sup></i></th>"+logoheader+"</tr>\n",HTML);
        //DecimalFormat decimalformatter=DataFormat.getDecimalFormatter(3);
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[SORTED_INDEX_MOTIF_ID];
            int seqcount=((Double)entry[SORTED_INDEX_MOTIF_SEQUENCE_COUNT]).intValue();
            int totalcount=((Double)entry[SORTED_INDEX_MOTIF_TOTAL_COUNT]).intValue();
            Double regCoeff=(Double)entry[SORTED_INDEX_REGRESSION_COEFFICIENT];
            Double rSquared=(Double)entry[SORTED_INDEX_R_SQUARED];
            Motif motif=null;
            if (engine.dataExists(motifname, Motif.class)) motif=(Motif)engine.getDataItem(motifname);
            String motifpresentationname=(motif!=null)?motif.getShortName():motifname;
            String motifclass=(motif!=null)?motif.getClassification():null;
            String motifclassname=null;
            if (motifclass==null) motifclass="unknown";
            else motifclassname=MotifClassification.getNameForClass(motifclass);
            if (motifclassname!=null) {
               motifclassname=escapeHTML(motifclassname);
               motifclassname=motifclassname.replace("\"", "&#34;");// escape quotes
            } 
            outputobject.append("<tr>",HTML);
            if (showColorBoxes) {
                Color color=Color.WHITE;               
                if (motif!=null) color=vizSettings.getFeatureColor(motifname);             
                String colorString=VisualizationSettings.convertColorToHTMLrepresentation(color);
                outputobject.append("<td><div style=\"width:10px;height:10px;border:1px solid #000;background-color:"+colorString+";\"></div></td>",HTML);
            }             
            outputobject.append("<td>"+escapeHTML(motifname)+"</td>",HTML);
            outputobject.append("<td>"+escapeHTML(motifpresentationname)+"</td>",HTML);
            outputobject.append("<td"+((motifclassname!=null)?(" title=\""+motifclassname+"\""):"")+">"+escapeHTML(motifclass)+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+totalcount+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+seqcount+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+(int)((double)seqcount*100f/(double)numberOfSequences)+"%</td>",HTML);
            outputobject.append("<td class=\"num\">"+Graph.formatNumber(regCoeff,false)+"</td><td class=\"num\">"+Graph.formatNumber(rSquared,false)+"</td>",HTML);
            if (showSequenceLogos) {
              if (motif instanceof Motif) {
                 sequencelogo.setMotif((Motif)motif);
                 outputobject.append("<td title=\"",HTML);
                 outputobject.append(sequencelogo.getMotifInfoTooltip(),HTML);
                 outputobject.append("\">",HTML);
                 outputobject.append(getMotifLogoTag((Motif)motif, outputobject, sequencelogo, showSequenceLogosString, engine),HTML);
                 outputobject.append("</td>",HTML);
              } else outputobject.append("<td>?</td>",HTML);
            }    
            outputobject.append("</tr>\n",HTML);            
            if (i%30==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                Thread.yield();
            }
            task.setStatusMessage("Executing operation: output ("+i+"/"+resultList.size()+")");
            format.setProgress(i, resultList.size()); // the +20 is just to not reach 100% in this loop
        }
        outputobject.append("</table>\n</body>\n</html>\n",HTML);
        format.setProgress(100);
        return outputobject;
    }
    
  @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder=SORT_BY_MOTIF;
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        Color [] basecolors=vizSettings.getBaseColors();
        boolean border=(Boolean)vizSettings.getSettingAsType("motif.border", Boolean.TRUE);        
        MotifLogo sequencelogo=new MotifLogo(basecolors,sequencelogoSize);
        String showSequenceLogosString="";
        boolean includeLegend=false;
        int logoheight=19;
        MotifCollection include=null;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
             includeLegend=(Boolean)settings.getResolvedParameter("Legend",defaults,engine); 
             include=(MotifCollection)settings.getResolvedParameter("Include",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos = includeLogosInOutput(showSequenceLogosString);
        boolean showLogosAsImages = includeLogosInOutputAsImages(showSequenceLogosString);         
            
        ArrayList<Object[]> resultList=assembleList(sortorder,include);
        int rownum=0;
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Motif regression");
        CreationHelper helper = (showLogosAsImages)?workbook.getCreationHelper():null;
        Drawing drawing = (showLogosAsImages)?sheet.createDrawingPatriarch():null; 
        
        
        CellStyle title = getExcelTitleStyle(workbook);
        CellStyle tableheader = getExcelTableHeaderStyle(workbook);
        
        // Make room for the header which will be added later

        Row row = null;
        int headerrows=6;
        if (includeLegend) {
            for (int j=0;j<headerrows;j++) {
               row = sheet.createRow(j); 
            }
            rownum=headerrows-1; // -1 because it will be incremented later on...
        }        
        int col=0;
        int logocolumn=0;
        row = sheet.createRow(rownum);
        String[] firstColumns=new String[]{"Motif ID","Name","Class","Count","Sequences","Reg. Coeff","R2"};
        outputStringValuesInCells(row, firstColumns, 0, tableheader);      
        col+=firstColumns.length;
        if (showSequenceLogos) {
            logocolumn=col;
            outputStringValuesInCells(row, new String[]{"Logo"}, logocolumn, tableheader);
            sheet.setColumnWidth(logocolumn, 10000); 
        }
        if (showLogosAsImages)  sheet.setColumnWidth(logocolumn, 10000); 
        int maxlogowidth=0; // the number of bases in the longest motif    
        for (int i=0;i<resultList.size();i++) {
            rownum++;
            row = sheet.createRow(rownum);
            col=0;            
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[SORTED_INDEX_MOTIF_ID];
            int sites=((Double)entry[SORTED_INDEX_MOTIF_TOTAL_COUNT]).intValue();
            int sequences=((Double)entry[SORTED_INDEX_MOTIF_SEQUENCE_COUNT]).intValue();
            Double regCoeff=(Double)entry[SORTED_INDEX_REGRESSION_COEFFICIENT];
            Double r2=(Double)entry[SORTED_INDEX_R_SQUARED];
            Motif motif=null;
            if (engine.dataExists(motifname, Motif.class)) motif=(Motif)engine.getDataItem(motifname);
            String shortname=(motif!=null)?motif.getShortName():motifname;
            String motifclass=(motif!=null)?motif.getClassification():null;
            if (motifclass==null) motifclass="unknown";
            outputStringValuesInCells(row, new String[]{motifname,shortname,motifclass}, col);
            col+=3;
            outputNumericValuesInCells(row, new double[]{sites,sequences,regCoeff,r2}, col);
            col+=4;  
            if (showSequenceLogos && motif!=null) {
                if (showLogosAsImages) {
                    try {
                        row.setHeightInPoints((short)(sheet.getDefaultRowHeightInPoints()*1.2));                        
                        sequencelogo.setMotif(motif);
                        int width=motif.getLength();
                        if (width>maxlogowidth) maxlogowidth=width;
                        byte[] image=getMotifLogoImageAsByteArray(sequencelogo, logoheight, border, "png");
                        int imageIndex=workbook.addPicture(image, org.apache.poi.xssf.usermodel.XSSFWorkbook.PICTURE_TYPE_PNG);
                        ClientAnchor anchor = helper.createClientAnchor();
                        anchor.setCol1(logocolumn);
                        anchor.setRow1(rownum);
                        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
                        Picture pict=drawing.createPicture(anchor, imageIndex);	
                        pict.resize();
                        int offsetX=25000;
                        int offsetY=25000;
                        anchor.setDx1(offsetX);
                        anchor.setDy1(offsetY);    
                        anchor.setDx2(anchor.getDx2()+offsetX);
                        anchor.setDy2(anchor.getDy2()+offsetY);  
                    } catch (Exception e) {e.printStackTrace(System.err);}
                }
                else outputStringValuesInCells(row, new String[]{motif.getConsensusMotif()}, logocolumn);
            }
            if (i%10==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                task.setStatusMessage("Executing operation: output ("+i+"/"+resultList.size()+")");
                format.setProgress(i, resultList.size()); // 
                if (i%100==0) Thread.yield();
            }
        }
        format.setProgress(95);
        autoSizeExcelColumns(sheet, 0, 6, 800);
        if (!showLogosAsImages) sheet.autoSizeColumn((short)logocolumn);          

        if (includeLegend) {        
            sheet.createFreezePane(0,headerrows,0,headerrows);
            row=sheet.getRow(0);
            outputStringValueInCell(row, 0, "Motif Regression Analysis", title);
            StringBuilder firstLine=new StringBuilder();
            firstLine.append("Motif regression analysis with motifs from \"");
            firstLine.append(motifCollectionName);
            firstLine.append("\" and sites from \"");
            firstLine.append(motifTrackName);
            firstLine.append("\"");

            firstLine.append(" on "+numberOfSequences);
            firstLine.append(" sequence"+((numberOfSequences!=1)?"s":""));
            if (sequenceCollectionName!=null) {
                firstLine.append(" from collection \"");
                firstLine.append(sequenceCollectionName);
                firstLine.append("\"");
            }
            row=sheet.getRow(2);
            outputStringValueInCell(row, 0, firstLine.toString(), null); 
  
            StringBuilder secondLine=new StringBuilder();
            secondLine.append("Sequence values were taken from \"");            
            secondLine.append("sequenceMapName");            
            secondLine.append("\".");   
            if (ignoreMissing) secondLine.append(" Only sequences containing hits for a motif were included in the regression analysis for that motif.");
            row=sheet.getRow(3);        
            outputStringValueInCell(row, 0, secondLine.toString(), null);
            
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
        return outputobject; 
    }        
    
    

    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder=SORT_BY_MOTIF;
        String showSequenceLogosString="";
        MotifCollection include=null;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
             include=(MotifCollection)settings.getResolvedParameter("Include",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos=includeLogosInOutput(showSequenceLogosString);

        ArrayList<Object[]> resultList=assembleList(sortorder, include);
        outputobject.append("#Motif regression analysis with motifs from '"+motifCollectionName+"' and sites from '"+motifTrackName+"'",RAWDATA);
        outputobject.append(" on "+numberOfSequences+" sequence"+((numberOfSequences!=1)?"s":""),RAWDATA);
        if (sequenceCollectionName!=null) outputobject.append(" from collection '"+sequenceCollectionName+"'",RAWDATA);
        outputobject.append("\n#Sequence values were taken from '"+sequenceMapName+"'",RAWDATA);
        if (ignoreMissing) outputobject.append("\n#Only sequences containing hits for a motif were included in the regression analysis for that motif",RAWDATA);
        outputobject.append("\n\n#Motif ID, total number of motif occurrences, sequences containing the motif, regression coefficient (beta), R^2",RAWDATA);
        if (showSequenceLogos) outputobject.append(", motif consensus",RAWDATA); 
        outputobject.append("\n",RAWDATA); 
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[SORTED_INDEX_MOTIF_ID];
            int seqcount=((Double)entry[SORTED_INDEX_MOTIF_SEQUENCE_COUNT]).intValue();
            int totalcount=((Double)entry[SORTED_INDEX_MOTIF_TOTAL_COUNT]).intValue();
            Double regCoeff=(Double)entry[SORTED_INDEX_REGRESSION_COEFFICIENT];
            Double rSquared=(Double)entry[SORTED_INDEX_R_SQUARED];
            outputobject.append(motifname,RAWDATA);
            outputobject.append("\t"+totalcount,RAWDATA);
            outputobject.append("\t"+seqcount,RAWDATA);
            outputobject.append("\t"+((regCoeff==null || Double.isNaN(regCoeff))?"":regCoeff),RAWDATA);
            outputobject.append("\t"+((rSquared==null || Double.isNaN(rSquared))?"":rSquared),RAWDATA);            
            if (showSequenceLogos) {
                Data motif=engine.getDataItem(motifname);
                if (motif!=null && motif instanceof Motif) outputobject.append("\t"+((Motif)motif).getConsensusMotif(),RAWDATA);
                else outputobject.append("\t?",RAWDATA);
            }   
            outputobject.append("\n",RAWDATA);           
            if (i%100==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                task.setStatusMessage("Executing operation: output ("+i+"/"+resultList.size()+")");
                format.setProgress(i, resultList.size()); // 
                Thread.yield();
            }
        }
        format.setProgress(100);
        return outputobject;
    }



    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        RegionDataset source=(RegionDataset)task.getParameter(PARAMETER_MOTIF_TRACK);
        if (!source.isMotifTrack()) throw new ExecutionError("Motif regression analysis can only be performed on motif tracks");
        motifTrackName=source.getName();
        MotifCollection motifcollection=(MotifCollection)task.getParameter(PARAMETER_MOTIFS);
        motifCollectionName=motifcollection.getName();
        statistics=new HashMap<String,double[]>(motifcollection.size());
        SequenceNumericMap geneExpression=(SequenceNumericMap)task.getParameter(PARAMETER_SEQUENCE_VALUES);
        sequenceMapName=geneExpression.getName();
        SequenceCollection collection=(SequenceCollection)task.getParameter(PARAMETER_SEQUENCES);
        if (collection==null) collection=task.getEngine().getDefaultSequenceCollection();
        else sequenceCollectionName=collection.getName();
        if (sequenceCollectionName!=null && sequenceCollectionName.equals(task.getEngine().getDefaultSequenceCollectionName())) sequenceCollectionName=null;
        numberOfSequences=collection.size();
        ArrayList<String> sequenceNames=collection.getAllSequenceNames();
        Boolean skipnr=(Boolean)task.getParameter(PARAMETER_SKIP_NONREGULATED);
        ignoreMissing=(skipnr==null)?false:skipnr.booleanValue();    
        Boolean donormalize=(Boolean)task.getParameter(PARAMETER_NORMALIZE);
        normalize=(donormalize==null)?false:donormalize.booleanValue();   
        
        TaskRunner taskRunner=task.getEngine().getTaskRunner();
        task.setProgress(0L,motifcollection.size());
        long[] counters=new long[]{0,0,motifcollection.size()}; // counters[0]=motifs started, [1]=motifs completed, [2]=total number of motifs

        ArrayList<ProcessMotifTask> processTasks=new ArrayList<ProcessMotifTask>(motifcollection.size());
        for (int i=0;i<motifcollection.size();i++) {
            String motifname=motifcollection.getMotifNameByIndex(i);
            Data data=task.getEngine().getDataItem(motifname);
            if (data==null || !(data instanceof Motif)) throw new ExecutionError(motifname+" is not a known motif");
            Motif motif=(Motif)data;
            processTasks.add(new ProcessMotifTask(motif, sequenceNames, source, geneExpression, task, counters));
        }
        List<Future<Motif>> futures=null;
        int countOK=0;            
        try {
            futures=taskRunner.invokeAll(processTasks); // this call apparently blocks until all tasks finish (either normally or by exceptions or being cancelled)                             
            for (Future<Motif> future:futures) {
                if (future.isDone() && !future.isCancelled()) {
                    future.get(); // this blocks until completion but the return value is not used
                    countOK++;
                }
            }
        } catch (Exception e) {  
           taskRunner.shutdownNow(); // Note: this will abort all executing tasks (even those that did not cause the exception), but that is OK. 
           if (e instanceof java.util.concurrent.ExecutionException) throw (Exception)e.getCause(); 
           else throw e; 
        }       
        if (countOK!=motifcollection.size()) {
            throw new ExecutionError("Some mysterious error occurred while performing analysis: "+getAnalysisName());
        }         
      
    }

   private double[] processMotif(String motifname, ArrayList<String> sequenceNames, RegionDataset source, SequenceNumericMap geneExpression) {
        SimpleRegression regression=new SimpleRegression();
        int totalmotifcount=0;
        int sequencecount=0;
        double totalMotifScore=0;         
        double minSeqScore=Double.MAX_VALUE;
        double maxSeqScore=-Double.MAX_VALUE;
        ArrayList<double[]> points=new ArrayList<double[]>();
        for (String sequenceName:sequenceNames) {
            RegionSequenceData seq=(RegionSequenceData)source.getSequenceByName(sequenceName);
            double motifSequenceScore=0;
            boolean present=false;
            double geneExpressionScore=geneExpression.getValue(sequenceName);
            for (Region r:seq.getOriginalRegions()) {
              if (r.getType().equals(motifname)) {
                  totalmotifcount++;
                  present=true;
                  motifSequenceScore+=r.getScore();
              }
            } 
            if (present || !ignoreMissing) {
                //regression.addData(motifSequenceScore, geneExpressionScore);
                points.add(new double[]{motifSequenceScore, geneExpressionScore});
                if (motifSequenceScore<minSeqScore) minSeqScore=motifSequenceScore;
                if (motifSequenceScore>maxSeqScore) maxSeqScore=motifSequenceScore;
            }  
            totalMotifScore+=motifSequenceScore;
            if (present) sequencecount++;                                      
        } // end for each sequence
        if (sequencecount>0) {
            double range=maxSeqScore-minSeqScore;
            for (int i=0;i<points.size();i++) {
                double[] point=points.get(i);
                if (normalize) point[0]=(point[0]-minSeqScore)/range; // normalize motif sequence score to [0,1] range                   
                regression.addData(point[0],point[1]);
            }
        }
        double[] motifstats=new double[6];
        motifstats[INDEX_MOTIF_TOTAL_COUNT]=totalmotifcount;
        motifstats[INDEX_MOTIF_TOTAL_SCORE]=totalMotifScore;
        motifstats[INDEX_MOTIF_SEQUENCE_COUNT]=sequencecount;
        motifstats[INDEX_REGRESSION_COEFFICIENT]=regression.getSlope();
        motifstats[INDEX_SIGNIFICANCE]=regression.getSignificance();
        motifstats[INDEX_R_SQUARED]=regression.getRSquare();
        
        return motifstats;
   } 
    
    
   protected class ProcessMotifTask implements Callable<Motif> {
        final Motif motif;
        final ArrayList<String> sequenceNames;
        final RegionDataset motiftrack;
        final SequenceNumericMap geneExpression;
        final long[] counters; // counters[0]=motifs started, [1]=motifs completed, [2]=total number of motifs.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final ExecutableTask task;  
        
        public ProcessMotifTask(Motif motif, ArrayList<String> sequenceNames, RegionDataset motiftrack, SequenceNumericMap geneExpression, ExecutableTask task, long[] counters) {
           this.motif=motif;
           this.motiftrack=motiftrack;
           this.sequenceNames=sequenceNames;
           this.geneExpression=geneExpression;
           this.counters=counters;
           this.task=task;
        }
         
        @Override
        @SuppressWarnings("unchecked")
        public Motif call() throws Exception {
            synchronized(counters) {
               counters[0]++; // number of sequences started  
            }        
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            
            double[] motifstats=processMotif(motif.getName(), sequenceNames, motiftrack, geneExpression);         
            
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed            
                statistics.put(motif.getName(),motifstats); 
                task.setStatusMessage("Executing analysis: "+getAnalysisName()+"  ("+counters[1]+"/"+counters[2]+")");
                task.setProgress(counters[1],counters[2]);                              
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();    
            return motif;
        }   
    }     


    private class SortOrderComparator implements Comparator<Object[]> {
        String sortorder=null;
        public SortOrderComparator(String order) {
            sortorder=order;
        }
        @Override
        public int compare(Object[] motif1, Object[] motif2) { //
             if (sortorder.equals(SORT_BY_REGRESSION_COEFFICIENT)) {
                 Double value1=(Double)motif1[SORTED_INDEX_REGRESSION_COEFFICIENT];
                 Double value2=(Double)motif2[SORTED_INDEX_REGRESSION_COEFFICIENT];
                 if (value1==null && value2==null) return 0;
                 if (value1==null) return 1;
                 if (value2==null) return -1;  
                 if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;
                 if (Double.isNaN(value1)) return 1;
                 if (Double.isNaN(value2)) return -1;
                 int res=value2.compareTo(value1); // Note that this sorts descending!
                 if (res!=0) return res;
                 else return ((Double)motif2[SORTED_INDEX_R_SQUARED]).compareTo(((Double)motif1[SORTED_INDEX_R_SQUARED])); // sorts descending
            } else if (sortorder.equals(SORT_BY_R2_VALUE)) {
                 Double value1=(Double)motif1[SORTED_INDEX_R_SQUARED];
                 Double value2=(Double)motif2[SORTED_INDEX_R_SQUARED];
                 if (value1==null && value2==null) return 0;
                 if (value1==null) return 1;
                 if (value2==null) return -1;        
                 if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;                     
                 if (Double.isNaN(value1)) return 1;
                 if (Double.isNaN(value2)) return -1;
                 int res=value2.compareTo(value1); // sorts descending
                 if (res!=0) return res;
                 else return ((Double)motif2[SORTED_INDEX_REGRESSION_COEFFICIENT]).compareTo(((Double)motif1[SORTED_INDEX_REGRESSION_COEFFICIENT])); // sorts descending
            } else { // sort by motif ID
                String motifname1=(String)motif1[SORTED_INDEX_MOTIF_ID];
                String motifname2=(String)motif2[SORTED_INDEX_MOTIF_ID];
                return motifname1.compareTo(motifname2);  // sorts ascending!
            }
        }
    }


    @Override
    protected Dimension getDefaultDisplayPanelDimensions() {
        return new Dimension(800,600);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected JPanel getDisplayPanel(final MotifLabGUI gui, boolean modal) {
        JPanel displayPanel=new JPanel(new BorderLayout());
        JPanel headerPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        StringBuilder headerString=new StringBuilder("<html>Motif regression analysis with motifs from <b>");
        headerString.append(motifCollectionName);
        headerString.append("</b> and sites from <b>");
        headerString.append(motifTrackName);
        headerString.append("</b><br>on ");
        headerString.append(numberOfSequences);
        headerString.append(" sequence"+((numberOfSequences!=1)?"s":""));
        if (sequenceCollectionName!=null) {
            headerString.append(" from collection <b>");
            headerString.append(sequenceCollectionName);
            headerString.append("</b>");
        }
        headerString.append("<br>Sequence values were taken from <b>");
        headerString.append(sequenceMapName);
        headerString.append("</b>");
        if (ignoreMissing) headerString.append("<br>Only sequences containing hits for a motif were included in the regression analysis for that motif");
        headerString.append("</html>");
        headerPanel.add(new JLabel(headerString.toString()));
        MotifRegressionTableModel tablemodel=new MotifRegressionTableModel(gui);
        final GenericMotifBrowserPanel panel=new GenericMotifBrowserPanel(gui, tablemodel, modal);
        CellRenderer_Classification classrenderer=new CellRenderer_Classification();
        CellRenderer_Precision precisionRendered=new CellRenderer_Precision();
        JTable table=panel.getTable();
        table.getColumn("Class").setCellRenderer(classrenderer);
        table.getColumn("Reg.Coeff").setCellRenderer(precisionRendered);
        table.getColumn("R\u00B2").setCellRenderer(precisionRendered);
        table.getColumn("ID").setPreferredWidth(60);
        table.getColumn("Class").setPreferredWidth(50);        
        table.getColumn("Count").setPreferredWidth(50);
        table.getColumn("Sequences").setPreferredWidth(88);       
        if (table.getRowSorter() instanceof TableRowSorter) {
            TableRowSorter rowsorter=(TableRowSorter)table.getRowSorter();
            NaNComparator nanComparator=new NaNComparator();
            for (int i=0;i<table.getColumnCount();i++) {
                if (table.getColumnClass(i)==Double.class) rowsorter.setComparator(i, nanComparator);
            }
        }
        table.getRowSorter().toggleSortOrder(MotifRegressionTableModel.R_SQUARED);
        //table.getRowSorter().toggleSortOrder(MotifRegressionTableModel.REGRESSION_COEFFICIENT);

        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8,5,0,5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)
          ));
        displayPanel.add(headerPanel,BorderLayout.NORTH);
        displayPanel.add(panel,BorderLayout.CENTER);
        panel.addExtraContextMenuItem("Perform Single Motif Regression", true, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] names=panel.getSelectedMotifNames();
                if (names!=null && names.length>0) performSingleMotifRegression(names[0], gui);
            }
        });
        displayPanel.setPreferredSize(getDefaultDisplayPanelDimensions());
        return displayPanel;
    }

    /** This method can be invoked by a context menu in the display panel for this analysis
     *  and allows the user to launch a SingleMotifRegression analysis on a selected motif 
     *  directly from the table context menu. The analysis will be launched as a task
     */
    private void performSingleMotifRegression(String motifname, MotifLabGUI gui) {
        OperationTask operationtask=new OperationTask("analyze");
        String targetName=gui.getGenericDataitemName(Analysis.class, null);        
        operationtask.setParameter(OperationTask.SOURCE_NAME, targetName);
        operationtask.setParameter(OperationTask.TARGET_NAME, targetName);
        operationtask.setParameter(OperationTask.ENGINE, gui.getEngine());   
        operationtask.setParameter(OperationTask.OPERATION_NAME, "analyze");
        operationtask.setParameter(Operation_analyze.ANALYSIS, gui.getEngine().getAnalysisForClass(SingleMotifRegressionAnalysis.class).getAnalysisName());
  
        ParameterSettings settings=new ParameterSettings();
        boolean allOK=true;
        Data data=gui.getEngine().getDataItem(motifname);
        if (!(data instanceof Motif)) allOK=false;
        data=gui.getEngine().getDataItem(motifTrackName);
        if (!(data instanceof RegionDataset)) allOK=false;
        String seqColName=(sequenceCollectionName!=null)?sequenceCollectionName:gui.getEngine().getDefaultSequenceCollectionName();
        data=gui.getEngine().getDataItem(seqColName);
        if (!(data instanceof SequenceCollection)) allOK=false;
        data=gui.getEngine().getDataItem(sequenceMapName);
        if (!(data instanceof SequenceNumericMap)) allOK=false;
        
        if (!allOK) {
            JOptionPane.showMessageDialog(gui.getComponent(), "This analysis can not be performed since some of the original data objects are missing", "Data Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        settings.setParameter(SingleMotifRegressionAnalysis.PARAMETER_MOTIF, motifname);
        settings.setParameter(SingleMotifRegressionAnalysis.PARAMETER_MOTIF_TRACK, motifTrackName);
        settings.setParameter(SingleMotifRegressionAnalysis.PARAMETER_SEQUENCES, sequenceCollectionName);
        settings.setParameter(SingleMotifRegressionAnalysis.PARAMETER_SEQUENCE_VALUES, sequenceMapName);
        settings.setParameter(SingleMotifRegressionAnalysis.PARAMETER_SKIP_NONREGULATED, ignoreMissing);
        settings.setParameter(SingleMotifRegressionAnalysis.PARAMETER_NORMALIZE, normalize);        
        operationtask.setParameter(Operation_analyze.PARAMETERS, settings);
        
        operationtask.addAffectedDataObject(targetName, SingleMotifRegressionAnalysis.class);
        gui.launchOperationTask(operationtask,gui.isRecording());    
           
    }
    
private class MotifRegressionTableModel extends AbstractTableModel {
    // the following are for
    private static final int COLOR=0;    
    private static final int ID=1;
    private static final int NAME=2;
    private static final int MOTIF_CLASS=3;    
    private static final int MOTIF_TOTAL_COUNT=4;
    private static final int MOTIF_SEQUENCE_COUNT=5;    
    private static final int REGRESSION_COEFFICIENT=6;
    private static final int R_SQUARED=7;    
    private static final int LOGO=8;
    // private static final int PVALUE=8;
    // private static final int MOTIF_TOTAL_SCORE=5;    
    
    private String[] columnNames=null;
    private String[] motifnames; // just to get them in a specific order
    private MotifLabEngine engine;
    private VisualizationSettings settings;

    public MotifRegressionTableModel(MotifLabGUI gui) {
        this.engine=gui.getEngine();
        settings=gui.getVisualizationSettings();
        columnNames=new String[]{" ","ID","Name","Class","Count","Sequences","Reg.Coeff","R\u00B2","Logo"};        
        motifnames=new String[statistics.size()];
        int i=0;
        for (String name:statistics.keySet()) {
           motifnames[i]=name;
           i++;
        }
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case COLOR:return Color.class;            
            case ID:return String.class;
            case NAME:return String.class;
            case MOTIF_CLASS:return String.class;                
            case MOTIF_TOTAL_COUNT:return Integer.class;
            case MOTIF_SEQUENCE_COUNT:return Integer.class;                
//            case MOTIF_TOTAL_SCORE:return Double.class;
            case REGRESSION_COEFFICIENT:return Double.class;
            case R_SQUARED:return Double.class;                
//            case PVALUE:return Double.class;
            case LOGO:return Motif.class;
            default:return Object.class;
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


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        String motifname=motifnames[rowIndex];
        double[] stat=(columnIndex==ID)?null:statistics.get(motifname);
        switch (columnIndex) {
            case COLOR:return settings.getFeatureColor(motifname);            
            case ID:return motifname;
            case NAME:return getMotifName(motifname);    
            case MOTIF_CLASS:return getMotifClass(motifname);                
            case MOTIF_TOTAL_COUNT:return (int)stat[INDEX_MOTIF_TOTAL_COUNT];
            case MOTIF_SEQUENCE_COUNT:return (int)stat[INDEX_MOTIF_SEQUENCE_COUNT];                
 //           case MOTIF_TOTAL_SCORE:return (double)stat[INDEX_MOTIF_TOTAL_SCORE];
            case REGRESSION_COEFFICIENT:return (double)stat[INDEX_REGRESSION_COEFFICIENT];
            case R_SQUARED:return (double)stat[INDEX_R_SQUARED];
 //           case PVALUE:return (double)stat[INDEX_SIGNIFICANCE];
            case LOGO:return getMotif(motifname);    
            default:return Object.class;
        }
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	return false;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return motifnames.length;
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
}// end class CellRenderer_Classification

private class CellRenderer_Precision extends DefaultTableCellRenderer {
    public CellRenderer_Precision() {
           super();
           this.setHorizontalAlignment(JLabel.RIGHT);
    }
    @Override
    public void setValue(Object value) {
           double val=(Double)value;
           if (value==null || Double.isNaN(val)) setText("");
           else setText(Graph.formatNumber(val, false));         
       }
}// end class CellRenderer_Classification

private class NaNComparator implements Comparator<Double> {
    @Override
    public int compare(Double value1, Double value2) {
         if (value1==null && value2==null) return 0;
         if (value1==null) return 1;
         if (value2==null) return -1;         
         if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;
         if (Double.isNaN(value1)) return 1;
         if (Double.isNaN(value2)) return -1;
         return value2.compareTo(value1); // note that this sorts the "wrong" way to avoid NaN's on top
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
