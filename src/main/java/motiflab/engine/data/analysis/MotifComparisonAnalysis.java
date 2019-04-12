/*
 
 
 */

package motiflab.engine.data.analysis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import javax.swing.JTable;
import motiflab.engine.data.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.Graph;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.util.MotifComparator;
import motiflab.gui.GenericMotifBrowserPanel;
import motiflab.gui.MotifLogo;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.VisualizationSettings;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;

/**
 *
 * @author kjetikl
 */
public class MotifComparisonAnalysis extends Analysis {
    private final static String typedescription="Analysis: motif similarity";
    private final static String analysisName="motif similarity";
    private final static String description="Compares a single motif to a collection of motifs using all available comparison metrics";
    private HashMap<String,HashMap<String,Double>> statistics=null; // key is metric name. Value is a new hash with key=MotifName and value=comparison value
    private String targetMotifName=null;    
    private String motifCollectionName=null;
    private int numberofmotifs=0;
    private String[] metrics=null; // lists the names of metrics used for the analysis
    private boolean usePvalue=false; // reserved for future use
    
    public static final String PARAMETER_TARGET_MOTIF="Target motif";
    public static final String PARAMETER_MOTIF_COLLECTION="Motifs";


    public MotifComparisonAnalysis() {
        this.name="MotifComparisonAnalysis_temp";
        addParameter(PARAMETER_TARGET_MOTIF,Motif.class, null,new Class[]{Motif.class},"The motif that other motifs should be compared to",true,false);
        addParameter(PARAMETER_MOTIF_COLLECTION,MotifCollection.class, null,new Class[]{MotifCollection.class},"The motifs that should be compared to the target",true,false);
      }

    @Override
    public String[] getSourceProxyParameters() {return new String[]{PARAMETER_TARGET_MOTIF,PARAMETER_MOTIF_COLLECTION};}
    
    @Override
    public boolean canUseAsSourceProxy(Data data) {
        return (data instanceof MotifCollection || data instanceof Motif);
    }  
    
    @Override
    public Parameter[] getOutputParameters() {
        return new Parameter[] {
             new Parameter("Metrics",String.class,"", null,"<html>Explicitly list the metrics to include in the output (separated with commas).<br>If this is empty, all metrics will be output.<br></html>",false,false),
             new Parameter("Sort by",String.class,"ID", null,"This could be 'ID' or one of the metric names",false,false),
             new Parameter("Logos",String.class,MOTIF_LOGO_NO, new String[]{MOTIF_LOGO_NO,MOTIF_LOGO_NEW,MOTIF_LOGO_SHARED,MOTIF_LOGO_TEXT},"Include sequence logos in the table",false,false),
             new Parameter("Color boxes",Boolean.class,Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a box with the assigned color for the motif will be output as the first column",false,false), 
             new Parameter("Legend",Boolean.class,Boolean.TRUE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a header with a title and analysis details will be included at the top of the Excel sheet.",false,false)       
        };
    }
    
    @Override
    public String[] getOutputParameterFilter(String parameter) {
        if (parameter.equals("Color boxes")) return new String[]{HTML};
        if (parameter.equals("Legend")) return new String[]{EXCEL};
        if (parameter.equals("Metrics") || parameter.equals("Sort by") || parameter.equals("Logos")) return new String[]{HTML,RAWDATA,EXCEL};        
        return null;
    }     

    @Override
    public String[] getResultVariables() {
        return (metrics!=null)?metrics:new String[0]; 
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename==null || variablename.isEmpty() || statistics==null || !statistics.containsKey(variablename)) throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
        HashMap<String,Double> result=statistics.get(variablename);
        MotifNumericMap map=new MotifNumericMap("temp",0);
        for (String motifname:result.keySet()) {
            double value=result.get(motifname);
            map.setValue(motifname, value);
        }
        return map;        
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
        return (metrics!=null)?metrics:new String[0];
    }

    @Override
    public Class getColumnType(String column) {
        if (metrics==null) return null;
        for (String m:metrics) {
            if (m.equalsIgnoreCase(column)) return Double.class;
        }
        return null;
    }

    @Override
    public HashMap<String,Object> getColumnData(String column) throws ExecutionError {
        Class coltype=getColumnType(column);
        if (coltype==null) throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
        HashMap<String,Double> result=statistics.get(column);
        HashMap<String,Object> columnData=new HashMap<String, Object>();
        for (String motifname:result.keySet()) {
            columnData.put(motifname, result.get(motifname));
        }
        return columnData;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MotifComparisonAnalysis clone() {
        MotifComparisonAnalysis newanalysis=new MotifComparisonAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.statistics=new HashMap<String,HashMap<String,Double>>(this.statistics.size());
        for (String key:this.statistics.keySet()) {
            HashMap<String,Double> metric=(HashMap<String,Double>)this.statistics.get(key).clone();
            newanalysis.statistics.put(key,metric);
        }
        newanalysis.targetMotifName=this.targetMotifName;
        newanalysis.motifCollectionName=this.motifCollectionName;
        newanalysis.numberofmotifs=this.numberofmotifs;
        newanalysis.metrics=this.metrics.clone(); 
        newanalysis.usePvalue=this.usePvalue;          
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        MotifComparisonAnalysis other=((MotifComparisonAnalysis)source);
        this.statistics=(HashMap<String,HashMap<String,Double>>)other.statistics;
        this.targetMotifName=other.targetMotifName;    
        this.motifCollectionName=other.motifCollectionName;    
        this.metrics=other.metrics;    
        this.numberofmotifs=other.numberofmotifs;
        this.usePvalue=other.usePvalue;           
    }

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

    /** Constructs a sorted list with the motif name and all selected metrics
     *  This method contains code common for both formatHTML and formatRaw
     */
    private ArrayList<Object[]> assembleList(ArrayList<String> includeMetrics, String sortkey, boolean ascending) {
        ArrayList<Object[]> resultList=new ArrayList<Object[]>(numberofmotifs);
        HashMap<String,Double> firstMetric=statistics.get(metrics[0]);
        Set<String> keys=firstMetric.keySet();
        Iterator<String> iterator=keys.iterator();
        int i=0;
        int size=includeMetrics.size();
        int sortColumn=0;
        for (int j=0;j<size;j++) if (sortkey.equalsIgnoreCase(includeMetrics.get(j))) {sortColumn=j+1;break;}
        while (iterator.hasNext()) {
            i++;
            String motifkey=iterator.next();
            Object[] entry=new Object[size+1];
            entry[0]=motifkey;
            for (int j=0;j<size;j++) {
               String metricName=includeMetrics.get(j);
               HashMap<String,Double> results=statistics.get(metricName);
               double metricvalue=results.get(motifkey);
               entry[j+1]=new Double(metricvalue);
            }
            resultList.add(entry);
        }
        Collections.sort(resultList, new SortOrderComparator(sortColumn, ascending));
        return resultList;
    }

    @Override
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortkey="ID";
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        Color [] basecolors=vizSettings.getBaseColors();
        MotifLogo sequencelogo=new MotifLogo(basecolors,sequencelogoSize);
        String metricsString="";
        ArrayList<String> includeMetrics=new ArrayList<String>();
        String showSequenceLogosString=MOTIF_LOGO_NO;
        boolean showColorBoxes=false;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             metricsString=(String)settings.getResolvedParameter("Metrics",defaults,engine);
             sortkey=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
             showColorBoxes=(Boolean)settings.getResolvedParameter("Color boxes",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos=(showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_NEW) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_SHARED) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_TEXT));        
        HashMap<String,String> abbrmap=new HashMap<String, String>();
        String[] metricnames=engine.getAllMotifComparatorNames(false);
        String[] metricabbr=engine.getAllMotifComparatorNames(true);
        for (int i=0;i<metricnames.length;i++) {
            abbrmap.put(metricnames[i],metricabbr[i]);
            if (sortkey.equalsIgnoreCase(metricnames[i]) || sortkey.equalsIgnoreCase(metricabbr[i])) sortkey=metricnames[i];
        }          
        if (metricsString.trim().isEmpty()) includeMetrics.addAll(Arrays.asList(metrics));
        else {
            String[] list=metricsString.trim().split("\\s*,\\s*");
            for (String inc:list) {
                boolean found=false;
                for (int j=0;j<metricnames.length;j++) {
                    if (inc.equalsIgnoreCase(metricnames[j]) || inc.equalsIgnoreCase(metricabbr[j])) {
                        includeMetrics.add(metricnames[j]);
                        found=true;
                        break;
                    }
                }
                if (!found) throw new ExecutionError("Motif Similarity Analysis has no result for metric: "+inc);
            }            
        }
        boolean ascending=true;
        MotifComparator comp=engine.getMotifComparator(sortkey);
        if (comp!=null && !comp.isDistanceMetric()) ascending=false;
        ArrayList<Object[]> resultList=assembleList(includeMetrics, sortkey, ascending);
        engine.createHTMLheader("Motif Similarity Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<h1 class=\"headline\">Motif Similarity Analysis</h1>\n",HTML);
        outputobject.append("<div class=\"summary\">\n",HTML);
        outputobject.append("Comparing target motif <span class=\"dataitem\">",HTML);
        outputobject.append(targetMotifName,HTML);
        outputobject.append("</span> to motifs from collection <span class=\"dataitem\">",HTML);
        outputobject.append(motifCollectionName,HTML);
        outputobject.append("</span>",HTML);
        outputobject.append("</div><br>\n",HTML);
        outputobject.append("<table class=\"sortable\">\n",HTML);
        outputobject.append("<tr>",HTML);
        if (showColorBoxes) outputobject.append("<th>&nbsp;</th>",HTML);               
        outputobject.append("<th>ID</th><th>Name</th><th>Class</th>",HTML);
        for (String metric:includeMetrics) {
          outputobject.append("<th title=\">",HTML);  
          outputobject.append(metric,HTML);  
          outputobject.append("\">",HTML);  
          outputobject.append(abbrmap.get(metric),HTML);  
          outputobject.append("</th>",HTML);  
        }
        if (showSequenceLogos) outputobject.append("<th class=\"sorttable_nosort\"> Logo </th>",HTML);
        outputobject.append("</tr>\n",HTML);
        //DecimalFormat decimalformatter=DataFormat.getDecimalFormatter(3);
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[0];
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
            for (int j=1;j<entry.length;j++) {
                double value=(Double)entry[j];
                outputobject.append("<td class=\"num\">"+Graph.formatNumber(value,false)+"</td>",HTML);
            }
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
            format.setProgress(i, resultList.size()); // 
        }
        outputobject.append("</table>\n</body>\n</html>\n",HTML);
        format.setProgress(100);
        return outputobject;
    }

    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortkey="ID";
        String showSequenceLogosString=MOTIF_LOGO_NO;
        String metricsString="";
        ArrayList<String> includeMetrics=new ArrayList<String>();
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             metricsString=(String)settings.getResolvedParameter("Metrics",defaults,engine);
             sortkey=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos=(showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_NEW) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_SHARED) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_TEXT));        
        HashMap<String,String> abbrmap=new HashMap<String, String>();
        String[] metricnames=engine.getAllMotifComparatorNames(false);
        String[] metricabbr=engine.getAllMotifComparatorNames(true);
        for (int i=0;i<metricnames.length;i++) {
            abbrmap.put(metricnames[i],metricabbr[i]);
            if (sortkey.equalsIgnoreCase(metricnames[i]) || sortkey.equalsIgnoreCase(metricabbr[i])) sortkey=metricnames[i];
        } 
        if (metricsString.trim().isEmpty()) includeMetrics.addAll(Arrays.asList(metrics));
        else {           
            String[] list=metricsString.trim().split("\\s*,\\s*");
            for (String inc:list) {
                boolean found=false;
                for (int j=0;j<metricnames.length;j++) {                  
                    if (inc.equalsIgnoreCase(metricnames[j]) || inc.equalsIgnoreCase(metricabbr[j])) {
                        includeMetrics.add(metricnames[j]);
                        found=true;
                        break;
                    }
                }
                if (!found) throw new ExecutionError("Motif Similarity Analysis has no result for metric: "+inc);
            }            
        }
        boolean ascending=true;
        MotifComparator comp=engine.getMotifComparator(sortkey);
        if (comp!=null && !comp.isDistanceMetric()) ascending=false;
        ArrayList<Object[]> resultList=assembleList(includeMetrics, sortkey, ascending);
        outputobject.append("#Comparing target motif '"+targetMotifName+"' against motifs from '"+motifCollectionName+"'",RAWDATA);
        outputobject.append("\n\n#Motif ID",RAWDATA);
        for (String metric:includeMetrics) {
           outputobject.append(",",RAWDATA);
           outputobject.append(metric,RAWDATA);
        }
        if (showSequenceLogos) outputobject.append(", motif consensus",RAWDATA); 
        outputobject.append("\n",RAWDATA); 
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[0];
            outputobject.append(motifname,RAWDATA);
            for (int j=1;j<entry.length;j++) {
                double value=(Double)entry[j];
                outputobject.append("\t"+value,RAWDATA);
            }           
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
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortkey="ID";
        String metricsString="";
        String showSequenceLogosString=MOTIF_LOGO_NO;
        boolean includeLegend=true;
        int logoheight=19;
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        Color [] basecolors=vizSettings.getBaseColors();
        boolean border=(Boolean)vizSettings.getSettingAsType("motif.border", Boolean.TRUE);        
        MotifLogo sequencelogo=new MotifLogo(basecolors,sequencelogoSize);   
        ArrayList<String> includeMetrics=new ArrayList<String>();        
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             metricsString=(String)settings.getResolvedParameter("Metrics",defaults,engine);
             sortkey=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);             
             includeLegend=(Boolean)settings.getResolvedParameter("Legend",defaults,engine);
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showLogosAsImages=(showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_NEW) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_SHARED));           
        boolean showSequenceLogos=(showLogosAsImages || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_TEXT));      
        HashMap<String,String> abbrmap=new HashMap<String, String>();
        String[] metricnames=engine.getAllMotifComparatorNames(false);
        String[] metricabbr=engine.getAllMotifComparatorNames(true);
        for (int i=0;i<metricnames.length;i++) {
            abbrmap.put(metricnames[i],metricabbr[i]);
            if (sortkey.equalsIgnoreCase(metricnames[i]) || sortkey.equalsIgnoreCase(metricabbr[i])) sortkey=metricnames[i];
        } 
        if (metricsString.trim().isEmpty()) includeMetrics.addAll(Arrays.asList(metrics));
        else {           
            String[] list=metricsString.trim().split("\\s*,\\s*");
            for (String inc:list) {
                boolean found=false;
                for (int j=0;j<metricnames.length;j++) {                  
                    if (inc.equalsIgnoreCase(metricnames[j]) || inc.equalsIgnoreCase(metricabbr[j])) {
                        includeMetrics.add(metricnames[j]);
                        found=true;
                        break;
                    }
                }
                if (!found) throw new ExecutionError("Motif Similarity Analysis has no result for metric: "+inc);
            }            
        }
        boolean ascending=true;
        MotifComparator comp=engine.getMotifComparator(sortkey);
        if (comp!=null && !comp.isDistanceMetric()) ascending=false;
        ArrayList<Object[]> resultList=assembleList(includeMetrics, sortkey, ascending);
        int rownum=0;
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(outputobject.getName());
        CreationHelper helper = (showLogosAsImages)?workbook.getCreationHelper():null;
        Drawing drawing = (showLogosAsImages)?sheet.createDrawingPatriarch():null;       
        
        CellStyle title=createExcelStyle(workbook, HSSFCellStyle.BORDER_NONE, (short)0, HSSFCellStyle.ALIGN_LEFT, false);      
        addFontToExcelCellStyle(workbook, title, null, (short)(workbook.getFontAt((short)0).getFontHeightInPoints()*2.5), true, false);
        CellStyle tableheader=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.LIGHT_YELLOW.index, HSSFCellStyle.ALIGN_CENTER, true);      
                  
        
        // Make room for the header which will be added later

        Row row = null;
        int headerrows=5;
        if (includeLegend) {
            for (int j=0;j<headerrows;j++) {
               row = sheet.createRow(j); 
            }
            rownum=headerrows-1; // -1 because it will be incremented later on...
        }        
        int col=0;
        int logocolumn=0;
        row = sheet.createRow(rownum);
        outputStringValuesInCells(row, new String[]{"Motif ID","Name","Class"}, 0, tableheader);      
        col+=3;
        for (String metric:includeMetrics) {
           outputStringValueInCell(row, col, abbrmap.get(metric), tableheader);
           col++;
        }
        if (showSequenceLogos) {
            logocolumn=col;
            outputStringValuesInCells(row, new String[]{"Logo"}, logocolumn, tableheader);
        } 
        int maxlogowidth=0; // the number of bases in the longest motif     
        if (showLogosAsImages) sheet.setColumnWidth(logocolumn, 10000);
        for (int i=0;i<resultList.size();i++) {
            rownum++;
            row = sheet.createRow(rownum);
            col=0;
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[0];
            String shortname="";
            String motifclass="";
            Motif motif=null;
            if (engine.dataExists(motifname, Motif.class)) {
                motif=(Motif)engine.getDataItem(motifname);
                motifclass=motif.getClassification();
                if (motifclass==null) motifclass="unknown";  
                shortname=motif.getShortName();
            }            
            outputStringValuesInCells(row, new String[]{motifname,shortname,motifclass}, col);
            col+=3;
            for (int j=1;j<entry.length;j++) {
                double value=(Double)entry[j];
                outputNumericValueInCell(row, col, value, null);
                col++;
            }              
               
            if (showSequenceLogos && motif!=null) {
                if (showLogosAsImages) {
                    try {
                        row.setHeightInPoints((short)(sheet.getDefaultRowHeightInPoints()*1.2));                        
                        sequencelogo.setMotif(motif);
                        int width=motif.getLength();
                        if (width>maxlogowidth) maxlogowidth=width;
                        byte[] image=getMotifLogoImageAsByteArray(sequencelogo, logoheight, border, "png");
                        int imageIndex=workbook.addPicture(image, HSSFWorkbook.PICTURE_TYPE_PNG);
                        ClientAnchor anchor = helper.createClientAnchor();
                        anchor.setCol1(logocolumn);
                        anchor.setRow1(rownum);
                        anchor.setAnchorType(ClientAnchor.MOVE_DONT_RESIZE);
                        Picture pict=drawing.createPicture(anchor, imageIndex);	
                        pict.resize();
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
        for (int j=0;j<logocolumn-1;j++) {
           sheet.autoSizeColumn((short)j);
        }
        if (!showLogosAsImages) sheet.autoSizeColumn((short)logocolumn);   
        //if (maxlogowidth>0) sheet.setColumnWidth(logocolumn, (int)(maxlogowidth*1.5*256)); // this does not work as I had hoped. It will resize the motif logos      
        
        // Add the header on top of the page
        if (includeLegend) {        
            sheet.createFreezePane(0,headerrows,0,headerrows);
            row=sheet.getRow(0);
            outputStringValueInCell(row, 0, "Motif similarity", title);
            StringBuilder firstLine=new StringBuilder();
            firstLine.append("Comparing target motif \"");
            firstLine.append(targetMotifName);
            firstLine.append("\" against motifs from \"");
            firstLine.append(motifCollectionName);
            firstLine.append("\""); 
            row=sheet.getRow(2);
            outputStringValueInCell(row, 0, firstLine.toString(), null);                 
        }
        
        // now write to the outputobject. The binary Excel file is included as a dependency in the otherwise empty OutputData object.
        File excelFile=outputobject.createDependentBinaryFile(engine,"xls");        
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
    public void runAnalysis(OperationTask task) throws Exception {
        Motif targetMotif=(Motif)task.getParameter(PARAMETER_TARGET_MOTIF);
        targetMotifName=targetMotif.getName();
        MotifCollection motifcollection=(MotifCollection)task.getParameter(PARAMETER_MOTIF_COLLECTION);       
        motifCollectionName=motifcollection.getName();
        numberofmotifs=motifcollection.size();
        ArrayList<MotifComparator> comparators=task.getEngine().getAllMotifComparators();
        metrics=task.getEngine().getAllMotifComparatorNames(false);
        statistics=new HashMap<String,HashMap<String,Double>>(metrics.length);
        ArrayList<Motif> collection=motifcollection.getAllMotifs(task.getEngine());
        int csize=collection.size();
        int size=csize*metrics.length;
        int i=0;
        for (MotifComparator motifcomparator:comparators) {
           HashMap<String,Double> results=new HashMap<String, Double>(motifcollection.size());
           for (Motif otherMotif:collection) {
              double[] value=motifcomparator.compareMotifs(targetMotif, otherMotif);
              results.put(otherMotif.getName(), value[0]); // value[0] is the match score (best of direct and reverse orientation)
              if (i%30==0) {
                  task.setProgress(i,size);
                  task.setStatusMessage("Executing operation: "+getAnalysisName()+" ("+((int)(i/metrics.length))+"/"+csize+")"); // reporting is a bit different
              }
              i++;              
           } // end for each motif
           statistics.put(motifcomparator.getName(), results);          
           task.checkExecutionLock(); // checks to see if this task should suspend execution
           if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
           Thread.yield();                   
        } // end for each metrics       
        task.setProgress(100); //          
    }


    private class SortOrderComparator implements Comparator<Object[]> {
        int sortcolumn=0;
        boolean ascending=true;
        public SortOrderComparator(int column, boolean ascending) {
            sortcolumn=column;
            this.ascending=ascending;
        }
        @Override
        public int compare(Object[] motif1, Object[] motif2) {
             Object value1=motif1[sortcolumn];
             Object value2=motif2[sortcolumn];
             int res=0;
             if (value1 instanceof Double) {
                res=((Double)value1).compareTo((Double)value2);              
             } else {
                res=((String)value1).compareTo((String)value2);
             }  
             if (ascending) return res; else return -res;
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
        StringBuilder headerString=new StringBuilder("<html>Comparing target motif <b>");
        headerString.append(targetMotifName);
        headerString.append("</b> against motifs from <b>");
        headerString.append(motifCollectionName);
        headerString.append("</b>");
        headerString.append("</html>");
        headerPanel.add(new JLabel(headerString.toString()));
        MotifTableModel tablemodel=new MotifTableModel(gui);
        final GenericMotifBrowserPanel panel=new GenericMotifBrowserPanel(gui, tablemodel, modal);
        CellRenderer_Classification classrenderer=new CellRenderer_Classification();
        JTable table=panel.getTable();
        table.getColumn("Class").setCellRenderer(classrenderer);
        table.getColumn("ID").setPreferredWidth(60);
        table.getColumn("Class").setPreferredWidth(50);        
        table.getRowSorter().toggleSortOrder(1);

        panel.setBorder(BorderFactory.createCompoundBorder(
             BorderFactory.createEmptyBorder(8,5,0,5),
             BorderFactory.createBevelBorder(BevelBorder.RAISED)
        ));
        displayPanel.add(headerPanel,BorderLayout.NORTH);
        displayPanel.add(panel,BorderLayout.CENTER);
        displayPanel.setPreferredSize(getDefaultDisplayPanelDimensions());
        return displayPanel;
    }

    
private class MotifTableModel extends AbstractTableModel {
    // the following are for
    private static final int COLOR=0;    
    private static final int ID=1;
    private static final int NAME=2;  
    private static final int MOTIF_CLASS=3;
    private final int LOGO;
     
    private String[] columnNames=null;
    private String[] motifnames; // just to get them in a specific order
    private MotifLabEngine engine;
    private VisualizationSettings settings;

    public MotifTableModel(MotifLabGUI gui) {
        this.engine=gui.getEngine();
        settings=gui.getVisualizationSettings();
        columnNames=new String[metrics.length+5];
        columnNames[0]=" ";
        columnNames[1]="ID";
        columnNames[2]="Name";
        columnNames[3]="Class";
        columnNames[metrics.length+4]="Logo";
        System.arraycopy(metrics, 0, columnNames, 4, metrics.length);
        if (statistics.size()>0 && metrics!=null && metrics.length>0) {
            HashMap<String,Double> result=statistics.get(metrics[0]); // use first metric to extract motif names
            motifnames=new String[result.size()];
            int i=0;
            for (String name:result.keySet()) {
               motifnames[i]=name;
               i++;
            }
        } else motifnames=new String[0];
        LOGO=(metrics.length+4); // 4 is for the 4 standard columns: COLOR,ID,NAME,CLASS
    }

    @Override
    public Class getColumnClass(int columnIndex) {
             if (columnIndex==COLOR) return Color.class;
        else if (columnIndex==ID) return String.class;
        else if (columnIndex==NAME) return String.class;
        else if (columnIndex==MOTIF_CLASS) return String.class;
        else if (columnIndex==LOGO) return Motif.class;
        else return Double.class;
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
             if (columnIndex==COLOR) return settings.getFeatureColor(motifname);       
        else if (columnIndex==ID) return motifname;
        else if (columnIndex==NAME) return getMotifName(motifname);
        else if (columnIndex==MOTIF_CLASS) return getMotifClass(motifname); 
        else if (columnIndex==LOGO) return getMotif(motifname);  
        else {            
            String metric=getColumnName(columnIndex);
            return statistics.get(metric).get(motifname);
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
