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
import javax.swing.table.TableRowSorter;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.Graph;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.dataformat.DataFormat;
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
public class CompareMotifsToNumericDatasetAnalysis extends Analysis {
   private final static String typedescription="Analysis: compare motif track to numeric track";
    private final static String analysisName="compare motif track to numeric track";
    private final static String description="Compares a motif track to a numeric track and finds min/max/average values of the numeric track within the sites for each motif";
    private static final String SORT_BY_MOTIF="Motif ID";
    private static final String SORT_BY_MOTIF_AVERAGE="Average value";
    private static final String SORT_BY_MOTIF_SUM="Sum value";
    private static final String SORT_BY_COUNT_ABOVE="above";    
    private HashMap<String,double[]> statistics=null; // key is motif name. Value is double[]
    private String sequenceCollectionName=null;
    private String motifCollectionName=null;
    private String motifTrackName=null;
    private String numericTrackName=null;
    private int numberOfSequences=0;
    private transient double threshold=0; // count number of motifs with average score above this threshold (v1.06+)
    private short currentinternalversion=2; // this is an internal version number for serialization of objects of this type

    
    // These are indexes into the statistics values (double[])
    private static final int INDEX_MOTIF_TOTAL_COUNT=0;
    private static final int INDEX_MOTIF_BASES=1;
    private static final int INDEX_MOTIF_MIN=2;
    private static final int INDEX_MOTIF_MAX=3;
    private static final int INDEX_MOTIF_SUM=4;
    private static final int INDEX_MOTIF_COUNT_ABOVE=5;    

    private static final int SORTED_INDEX_MOTIF_ID=0;
    private static final int SORTED_INDEX_MOTIF_TOTAL_COUNT=1;
    private static final int SORTED_INDEX_MOTIF_BASES=2;
    private static final int SORTED_INDEX_MOTIF_MIN=3;
    private static final int SORTED_INDEX_MOTIF_MAX=4;
    private static final int SORTED_INDEX_MOTIF_AVERAGE=5;
    private static final int SORTED_INDEX_MOTIF_SUM=6;
    private static final int SORTED_INDEX_MOTIF_COUNT_ABOVE=7;

    public CompareMotifsToNumericDatasetAnalysis() {
        this.name="CompareMotifsToNumericDatasetAnalysis_temp";
        addParameter("Motif track",RegionDataset.class, null,new Class[]{RegionDataset.class},"A region track containing motif sites",true,false);
        addParameter("Motifs",MotifCollection.class, null,new Class[]{MotifCollection.class},"The motifs to consider in this analysis",true,false);
        addParameter("Numeric track",NumericDataset.class, null,new Class[]{NumericDataset.class},"The numeric track to compare against the motif sites",true,false);
        addParameter("Sequences",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"If specified, the analysis will be limited to the sequences in this collection",false,false);
        addParameter("Threshold",Double.class, new Double(0.0),new Double[]{-Double.MAX_VALUE,Double.MAX_VALUE},"One of the statistics computed will be based on the number of motif sites that have an average track score above this threshold.",false,false);
     }

    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Motif track","Numeric track","Motifs"};} 
    
    @Override
    public boolean canUseAsSourceProxy(Data data) {
        if (data instanceof RegionDataset) return ((RegionDataset)data).isMotifTrack(); // only allow Motif Tracks as input
        else return (data instanceof MotifCollection || data instanceof NumericDataset);
    }     
    
    @Override
    public Parameter[] getOutputParameters() {
        String[] sortBy=(currentinternalversion>=2)?new String[]{SORT_BY_MOTIF,SORT_BY_MOTIF_AVERAGE,SORT_BY_MOTIF_SUM,SORT_BY_COUNT_ABOVE}:new String[]{SORT_BY_MOTIF,SORT_BY_MOTIF_AVERAGE,SORT_BY_MOTIF_SUM};
        return new Parameter[] {
             new Parameter("Include",MotifCollection.class,null,new Class[]{MotifCollection.class},"Only include data from this collection",false,false),                        
             new Parameter("Sort by",String.class,SORT_BY_MOTIF_AVERAGE, sortBy,null,false,false),
             new Parameter("Logos",String.class,MOTIF_LOGO_NO, new String[]{MOTIF_LOGO_NO,MOTIF_LOGO_NEW,MOTIF_LOGO_SHARED,MOTIF_LOGO_TEXT},"Include sequence logos in the table",false,false),
             new Parameter("Color boxes",Boolean.class,Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a box with the assigned color for the motif will be output as the first column",false,false),      
             new Parameter("Legend",Boolean.class,Boolean.TRUE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a header with a title and analysis details will be included at the top of the Excel sheet.",false,false)       
        };
    }
    
    @Override
    public String[] getOutputParameterFilter(String parameter) {
        if (parameter.equals("Color boxes")) return new String[]{HTML};
        if (parameter.equals("Legend")) return new String[]{EXCEL};
        if (parameter.equals("Include") || parameter.equals("Logos") || parameter.equals("Sort by")) return new String[]{HTML,RAWDATA,EXCEL};        
        return null;
    }     

    @Override
    public String[] getResultVariables() {
        if (currentinternalversion==2) return new String[]{"sites","bases","min","max","average","sum","above","threshold"};
        return new String[]{"sites","bases","min","max","average","sum"};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        int statindex=-1;
        if (variablename==null || variablename.isEmpty()) throw new ExecutionError("'"+getName()+"' does not have a result for ''");
        else if (variablename.equals("threshold") && currentinternalversion>=2)  return new NumericVariable("temp",threshold);        
        else if (variablename.equals("sites")) statindex=INDEX_MOTIF_TOTAL_COUNT;
        else if (variablename.equals("bases"))  statindex=INDEX_MOTIF_BASES;
        else if (variablename.equals("min"))  statindex=INDEX_MOTIF_MIN;
        else if (variablename.equals("max"))  statindex=INDEX_MOTIF_MAX;
        else if (variablename.equals("sum"))  statindex=INDEX_MOTIF_SUM;
        else if (variablename.equals("above") && currentinternalversion>=2)  statindex=INDEX_MOTIF_COUNT_ABOVE;        
        else if (variablename.equals("average"))  {
            MotifNumericMap map=new MotifNumericMap("temp",0);
            for (String motifname:statistics.keySet()) {
                double[] stats=statistics.get(motifname);
                double average=stats[INDEX_MOTIF_SUM]/stats[INDEX_MOTIF_BASES];
                map.setValue(motifname, average);
            }
            return map;
        }
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
        // now make the map based on the selected statistic
        MotifNumericMap map=new MotifNumericMap("temp",0);
        for (String motifname:statistics.keySet()) {
            double[] stats=statistics.get(motifname);
            map.setValue(motifname, stats[statindex]);
        }
        return map;
    }

    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else if (variablename.equals("threshold")) return NumericVariable.class;
       else return MotifNumericMap.class;
    }

    @Override
    public Class getCollateType() {
        return Motif.class;
    }

    @Override
    public String[] getColumnsExportedForCollation() {
        if (currentinternalversion==2) return new String[]{"sites","bases","min","max","average","sum","above"};
        return new String[]{"sites","bases","min","max","average","sum"};
    }

    @Override
    public Class getColumnType(String column) {
             if (column.equalsIgnoreCase("sites") || column.equalsIgnoreCase("bases")) return Integer.class;
        else if (column.equalsIgnoreCase("min") || column.equalsIgnoreCase("max") || column.equalsIgnoreCase("average") || column.equalsIgnoreCase("sum")) return Double.class;
        else if (column.equalsIgnoreCase("above") && currentinternalversion>=2) return Double.class;
        else return null;
    }

    @Override
    public HashMap<String,Object> getColumnData(String column) throws ExecutionError {
        Class coltype=getColumnType(column);
        if (coltype==null) throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
        HashMap<String,Object> columnData=new HashMap<String, Object>();
        for (String motifname:statistics.keySet()) {
            double[] stat=statistics.get(motifname);
                 if (column.equalsIgnoreCase("sites")) columnData.put(motifname, new Integer((int)stat[INDEX_MOTIF_TOTAL_COUNT]));
            else if (column.equalsIgnoreCase("bases")) columnData.put(motifname, new Integer((int)stat[INDEX_MOTIF_BASES]));
            else if (column.equalsIgnoreCase("min")) columnData.put(motifname, new Double(stat[INDEX_MOTIF_MIN]));
            else if (column.equalsIgnoreCase("max")) columnData.put(motifname, new Double(stat[INDEX_MOTIF_MAX]));
            else if (column.equalsIgnoreCase("sum")) columnData.put(motifname, new Double(stat[INDEX_MOTIF_SUM]));            
            else if (column.equalsIgnoreCase("average")) columnData.put(motifname, new Double(stat[INDEX_MOTIF_SUM]/stat[INDEX_MOTIF_BASES]));
            else if (column.equalsIgnoreCase("above")) columnData.put(motifname, new Double(stat[INDEX_MOTIF_COUNT_ABOVE]));
        }
        return columnData;
    }  


    @Override
    public String getAnalysisName() {
        return analysisName;
    }

    @Override
    public String getDescription() {return description;}

    @Override
    @SuppressWarnings("unchecked")
    public CompareMotifsToNumericDatasetAnalysis clone() {
        CompareMotifsToNumericDatasetAnalysis newanalysis=new CompareMotifsToNumericDatasetAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.statistics=new HashMap<String,double[]>(this.statistics.size());
        for (String key:this.statistics.keySet()) {
            newanalysis.statistics.put(key,this.statistics.get(key).clone());
        }        
        newanalysis.sequenceCollectionName=this.sequenceCollectionName;
        newanalysis.motifCollectionName=this.motifCollectionName;
        newanalysis.motifTrackName=this.motifTrackName;
        newanalysis.numericTrackName=this.numericTrackName;
        newanalysis.numberOfSequences=this.numberOfSequences;
        newanalysis.currentinternalversion=this.currentinternalversion;
        newanalysis.threshold=this.threshold;
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        CompareMotifsToNumericDatasetAnalysis other=((CompareMotifsToNumericDatasetAnalysis)source);
        this.statistics=(HashMap<String,double[]>)other.statistics;
        this.sequenceCollectionName=other.sequenceCollectionName;
        this.motifCollectionName=other.motifCollectionName;
        this.motifTrackName=other.motifTrackName;
        this.numberOfSequences=other.numberOfSequences;
        this.numericTrackName=other.numericTrackName;
        this.currentinternalversion=other.currentinternalversion;
        this.threshold=other.threshold;
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
    private ArrayList<Object[]> assembleList(String sortorder, MotifCollection collection) {
        ArrayList<Object[]> resultList=new ArrayList<Object[]>(statistics.size());
        Set<String> keys=statistics.keySet();
        Iterator<String> iterator=keys.iterator();
        int i=0;
        while (iterator.hasNext()) {
            i++;
            String motifkey=iterator.next();
            if (collection!=null && !collection.contains(motifkey)) continue;
            double[] values=statistics.get(motifkey);
            Object[] entry=new Object[(currentinternalversion>=2)?8:7]; // one extra field added in v2 for a total of 8 (7 fields in v1)
            entry[SORTED_INDEX_MOTIF_ID]=motifkey;
            entry[SORTED_INDEX_MOTIF_TOTAL_COUNT]=new Double(values[INDEX_MOTIF_TOTAL_COUNT]);
            entry[SORTED_INDEX_MOTIF_BASES]=new Double(values[INDEX_MOTIF_BASES]);
            entry[SORTED_INDEX_MOTIF_MIN]=new Double(values[INDEX_MOTIF_MIN]);
            entry[SORTED_INDEX_MOTIF_MAX]=new Double(values[INDEX_MOTIF_MAX]);
            entry[SORTED_INDEX_MOTIF_SUM]=new Double(values[INDEX_MOTIF_SUM]);
            entry[SORTED_INDEX_MOTIF_AVERAGE]=new Double(values[INDEX_MOTIF_SUM]/values[INDEX_MOTIF_BASES]);
            if (currentinternalversion>=2) entry[SORTED_INDEX_MOTIF_COUNT_ABOVE]=new Double(values[INDEX_MOTIF_COUNT_ABOVE]);
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
        String showSequenceLogosString=MOTIF_LOGO_NO;
        boolean showColorBoxes=false;
        MotifCollection include=null;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
             showColorBoxes=(Boolean)settings.getResolvedParameter("Color boxes",defaults,engine);   
             include=(MotifCollection)settings.getResolvedParameter("Include",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos=(showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_NEW) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_SHARED) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_TEXT));
        if (currentinternalversion<2 && sortorder.equals(SORT_BY_COUNT_ABOVE)) sortorder=SORT_BY_MOTIF;
        
        ArrayList<Object[]> resultList=assembleList(sortorder, include);
        engine.createHTMLheader("Motif Track compared to Numeric Track", null, null, true, true, true, outputobject);
        outputobject.append("<h1 class=\"headline\">Motif Track compared to Numeric Track</h1>\n",HTML);
        outputobject.append("<div class=\"summary\">\n",HTML);
        outputobject.append("Motifs from <span class=\"dataitem\">",HTML);
        outputobject.append(motifCollectionName,HTML);
        outputobject.append("</span> and sites from <span class=\"dataitem\">",HTML);
        outputobject.append(motifTrackName,HTML);
        outputobject.append("</span> compared to <span class=\"dataitem\">",HTML);
        outputobject.append(numericTrackName,HTML);
        outputobject.append("</span> on "+numberOfSequences,HTML);
        outputobject.append(" sequence",HTML);
        outputobject.append((numberOfSequences!=1)?"s":"",HTML);
        if (sequenceCollectionName!=null) {
            outputobject.append(" from collection <span class=\"dataitem\">",HTML);
            outputobject.append(sequenceCollectionName,HTML);
            outputobject.append("</span>",HTML);
        }
        if (currentinternalversion>=2) {
            outputobject.append("<br>Threshold used for 'count above threshold' statistic = "+threshold,HTML);
        }        
        outputobject.append("\n</div>\n<br>\n",HTML);
        outputobject.append("<table class=\"sortable\">\n",HTML);
        String logoheader=(showSequenceLogos)?"<th class=\"sorttable_nosort\"> Logo </th>":"";
        outputobject.append("<tr>",HTML);
        if (showColorBoxes) outputobject.append("<th>&nbsp;</th>",HTML);        
        outputobject.append("<th>ID</th><th>Name</th><th class=\"sorttable_ip\">Class</th><th>Sites</th><th>Bases</th><th>Min</th><th>Max</th><th>Sum</th><th>Average</th>",HTML);
        if (currentinternalversion>=2) outputobject.append("<th>Count above</th>",HTML);
        outputobject.append(logoheader+"</tr>\n",HTML);
        //DecimalFormat decimalformatter=DataFormat.getDecimalFormatter(3);
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[SORTED_INDEX_MOTIF_ID];
            int sites=((Double)entry[SORTED_INDEX_MOTIF_TOTAL_COUNT]).intValue();
            int bases=((Double)entry[SORTED_INDEX_MOTIF_BASES]).intValue();
            Double min=(Double)entry[SORTED_INDEX_MOTIF_MIN];
            Double max=(Double)entry[SORTED_INDEX_MOTIF_MAX];
            Double sum=(Double)entry[SORTED_INDEX_MOTIF_SUM];
            Double average=(Double)entry[SORTED_INDEX_MOTIF_AVERAGE];
            int countAbove=(currentinternalversion>=2)?(((Double)entry[SORTED_INDEX_MOTIF_COUNT_ABOVE]).intValue()):0;
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
            outputobject.append("<td class=\"num\">"+sites+"</td><td class=\"num\">"+bases+"</td><td class=\"num\">"+Graph.formatNumber(min,false)+"</td><td class=\"num\">"+Graph.formatNumber(max,false)+"</td><td class=\"num\">"+Graph.formatNumber(sum,false)+"</td><td class=\"num\">"+Graph.formatNumber(average,false)+"</td>",HTML);
            if (currentinternalversion>=2) outputobject.append("<td class=\"num\">"+countAbove+"</td>",HTML);
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
            format.setProgress(i, resultList.size());
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
        String showSequenceLogosString=MOTIF_LOGO_NO;
        boolean includeLegend=false;
        int logoheight=19;
        MotifCollection include=null;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
             includeLegend=(Boolean)settings.getResolvedParameter("Legend",defaults,engine); 
             include=(MotifCollection)settings.getResolvedParameter("Include",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showLogosAsImages=(showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_NEW) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_SHARED));           
        boolean showSequenceLogos=(showLogosAsImages || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_TEXT));      
 
        if (currentinternalversion<2 && sortorder.equals(SORT_BY_COUNT_ABOVE)) sortorder=SORT_BY_MOTIF;
        
        ArrayList<Object[]> resultList=assembleList(sortorder,include);
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
        int headerrows=(currentinternalversion>=2)?6:5;
        if (includeLegend) {
            for (int j=0;j<headerrows;j++) {
               row = sheet.createRow(j); 
            }
            rownum=headerrows-1; // -1 because it will be incremented later on...
        }        
        int col=0;
        int logocolumn=0;
        row = sheet.createRow(rownum);
        String[] firstColumns=new String[]{"Motif ID","Name","Class","Sites","Bases","Min","Max","Sum","Average"};
        outputStringValuesInCells(row, firstColumns, 0, tableheader);      
        col+=firstColumns.length;
        if (currentinternalversion>=2) {
            outputStringValueInCell(row, col, "Count above", tableheader);   
            col++;
        }
        if (showSequenceLogos) {
            logocolumn=col;
            outputStringValuesInCells(row, new String[]{"Logo"}, logocolumn, tableheader);
        }
        int maxlogowidth=0; // the number of bases in the longest motif    
        for (int i=0;i<resultList.size();i++) {
            rownum++;
            row = sheet.createRow(rownum);
            col=0;            
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[SORTED_INDEX_MOTIF_ID];
            int sites=((Double)entry[SORTED_INDEX_MOTIF_TOTAL_COUNT]).intValue();
            int bases=((Double)entry[SORTED_INDEX_MOTIF_BASES]).intValue();
            Double min=(Double)entry[SORTED_INDEX_MOTIF_MIN];
            Double max=(Double)entry[SORTED_INDEX_MOTIF_MAX];
            Double sum=(Double)entry[SORTED_INDEX_MOTIF_SUM];
            Double average=(Double)entry[SORTED_INDEX_MOTIF_AVERAGE];
            int countAbove=(currentinternalversion>=2)?(((Double)entry[SORTED_INDEX_MOTIF_COUNT_ABOVE]).intValue()):0;
            Motif motif=null;
            if (engine.dataExists(motifname, Motif.class)) motif=(Motif)engine.getDataItem(motifname);
            String shortname=(motif!=null)?motif.getShortName():motifname;
            String motifclass=(motif!=null)?motif.getClassification():null;
            if (motifclass==null) motifclass="unknown";
            outputStringValuesInCells(row, new String[]{motifname,shortname,motifclass}, col);
            col+=3;
            outputNumericValuesInCells(row, new double[]{sites,bases,min,max,sum,average}, col);
            col+=6;
            if (currentinternalversion>=2) {
                outputNumericValuesInCells(row, new double[]{countAbove}, col++);
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
        for (short i=0;i<=col;i++) {
            sheet.autoSizeColumn(i);
        }
        if (!showLogosAsImages) sheet.autoSizeColumn((short)logocolumn);   
        
        if (includeLegend) {        
            sheet.createFreezePane(0,headerrows,0,headerrows);
            row=sheet.getRow(0);
            outputStringValueInCell(row, 0, "Motif track compared to Numeric track", title);
            StringBuilder firstLine=new StringBuilder();
            firstLine.append("Motifs from \"");
            firstLine.append(motifCollectionName);
            firstLine.append("\" and sites from \"");
            firstLine.append(motifTrackName);
            firstLine.append("\" compared to \"");
            firstLine.append(numericTrackName);
            firstLine.append("\"");

            firstLine.append(" on "+numberOfSequences);
            firstLine.append(" sequence"+((numberOfSequences!=1)?"s":""));
            if (sequenceCollectionName!=null) {
                firstLine.append(" from collection \"");
                firstLine.append(sequenceCollectionName);
                firstLine.append("\"");
            }
            firstLine.append(".");

            row=sheet.getRow(2);
            outputStringValueInCell(row, 0, firstLine.toString(), null); 
            if (currentinternalversion>=2) {
                row=sheet.getRow(3);
                outputStringValueInCell(row, 0, "Threshold used for 'count above threshold' statistic = "+threshold, null);
            }
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
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder=SORT_BY_MOTIF;
        String showSequenceLogosString=MOTIF_LOGO_NO;
        MotifCollection include=null; 
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
             include=(MotifCollection)settings.getResolvedParameter("Include",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos=(showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_NEW) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_SHARED) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_TEXT));

        ArrayList<Object[]> resultList=assembleList(sortorder, include);
        outputobject.append("#Motifs from '"+motifCollectionName+"' and sites from '"+motifTrackName+"'",RAWDATA);
        outputobject.append(" compared to numeric track '"+numericTrackName+"' on "+numberOfSequences+" sequence"+((numberOfSequences!=1)?"s":""),RAWDATA);
        if (sequenceCollectionName!=null) outputobject.append(" from collection '"+sequenceCollectionName+"'",RAWDATA);
        if (currentinternalversion>=2) outputobject.append("\n#Threshold for 'count above' statistic = "+threshold,RAWDATA);
        outputobject.append("\n\n#Motif ID, sites, bases, min, max, sum, average",RAWDATA);
        if (currentinternalversion>=2) outputobject.append(", count above threshold",RAWDATA); 
        if (showSequenceLogos) outputobject.append(", motif consensus",RAWDATA); 
        outputobject.append("\n",RAWDATA);         
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[SORTED_INDEX_MOTIF_ID];
            //if (include!=null && !include.contains(motifname)) continue;            
            int sites=((Double)entry[SORTED_INDEX_MOTIF_TOTAL_COUNT]).intValue();
            int bases=((Double)entry[SORTED_INDEX_MOTIF_BASES]).intValue();
            Double min=(Double)entry[SORTED_INDEX_MOTIF_MIN];
            Double max=(Double)entry[SORTED_INDEX_MOTIF_MAX];
            Double sum=(Double)entry[SORTED_INDEX_MOTIF_SUM];
            Double average=(Double)entry[SORTED_INDEX_MOTIF_AVERAGE];
            int countAbove=(currentinternalversion>=2)?(((Double)entry[SORTED_INDEX_MOTIF_COUNT_ABOVE]).intValue()):0;
            outputobject.append(motifname,RAWDATA);
            outputobject.append("\t"+sites,RAWDATA);
            outputobject.append("\t"+bases,RAWDATA);
            outputobject.append("\t"+((min==null || Double.isNaN(min))?"":min),RAWDATA);
            outputobject.append("\t"+((max==null || Double.isNaN(max))?"":max),RAWDATA);              
            outputobject.append("\t"+((sum==null || Double.isNaN(sum))?"":sum),RAWDATA);
            outputobject.append("\t"+((average==null || Double.isNaN(average))?"":average),RAWDATA);
            if (currentinternalversion>=2) outputobject.append("\t"+countAbove,RAWDATA);
            if (showSequenceLogos) {
                Data motif=engine.getDataItem(motifname);
                if (motif!=null && motif instanceof Motif) outputobject.append("\t"+((Motif)motif).getConsensusMotif(),RAWDATA);
                else outputobject.append("\t?",RAWDATA);
            }   
            outputobject.append("\n",RAWDATA);              
            if (i%100==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                Thread.yield();
            }
            task.setStatusMessage("Executing operation: output ("+i+"/"+resultList.size()+")");
            format.setProgress(i, resultList.size()); //
        }
        format.setProgress(100);
        return outputobject;
    }

    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        RegionDataset source=(RegionDataset)task.getParameter("Motif track");
        if (!source.isMotifTrack()) throw new ExecutionError("Motif regression analysis can only be performed on motif tracks");
        motifTrackName=source.getName();
        MotifCollection motifcollection=(MotifCollection)task.getParameter("Motifs");
        motifCollectionName=motifcollection.getName();
        statistics=new HashMap<String,double[]>(motifcollection.size());
        NumericDataset numericTrack=(NumericDataset)task.getParameter("Numeric track");
        numericTrackName=numericTrack.getName();
        Double thresholdDouble=(Double)task.getParameter("Threshold");
        if (thresholdDouble!=null) threshold=thresholdDouble.doubleValue();        
        SequenceCollection collection=(SequenceCollection)task.getParameter("Sequences");
        if (collection==null) collection=task.getEngine().getDefaultSequenceCollection();
        else sequenceCollectionName=collection.getName();
        if (sequenceCollectionName!=null && sequenceCollectionName.equals(task.getEngine().getDefaultSequenceCollectionName())) sequenceCollectionName=null;
        numberOfSequences=collection.size();
        ArrayList<String> sequenceNames=collection.getAllSequenceNames();
        int counter=0;
        for (String motifname:motifcollection.getAllMotifNames()) { // initialize datastructure for each motif
            double[] motifstats=new double[6];
            motifstats[INDEX_MOTIF_TOTAL_COUNT]=0;
            motifstats[INDEX_MOTIF_BASES]=0;
            motifstats[INDEX_MOTIF_SUM]=0;
            motifstats[INDEX_MOTIF_MIN]=Double.MAX_VALUE;
            motifstats[INDEX_MOTIF_MAX]=-Double.MAX_VALUE;
            motifstats[INDEX_MOTIF_COUNT_ABOVE]=0;            
            statistics.put(motifname,motifstats);
        }

        for (String sequenceName:sequenceNames) {
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            task.setStatusMessage("Executing analysis: "+getAnalysisName()+" ("+counter+"/"+numberOfSequences+")");
            task.setProgress(counter, numberOfSequences); //
            if (counter%30==0) Thread.yield();
            RegionSequenceData regionSequence=(RegionSequenceData)source.getSequenceByName(sequenceName);
            NumericSequenceData numericSequence=(NumericSequenceData)numericTrack.getSequenceByName(sequenceName);
            for (Region r:regionSequence.getOriginalRegions()) {
              String motifname=r.getType();
              int start=r.getRelativeStart();
              int end=r.getRelativeEnd();
              if (start<0) start=0;
              if (end>=regionSequence.getSize()) end=regionSequence.getSize()-1;
              double min=Double.MAX_VALUE;
              double max=-Double.MAX_VALUE;
              double sum=0;
              for (int i=start;i<=end;i++) {
                  double numericvalue=numericSequence.getValueAtRelativePosition(i);
                  if (Double.isNaN(numericvalue)) continue;
                  if (numericvalue<min) min=numericvalue;
                  if (numericvalue>max) max=numericvalue;
                  sum+=numericvalue;
              }
              double[] motifstats=statistics.get(motifname);
              if (motifstats==null) continue; // not of a type that we want to use
              motifstats[INDEX_MOTIF_TOTAL_COUNT]++;
              motifstats[INDEX_MOTIF_BASES]+=r.getLength();
              motifstats[INDEX_MOTIF_SUM]+=sum;
              if (sum/(double)r.getLength()>=threshold) motifstats[INDEX_MOTIF_COUNT_ABOVE]++;
              if (min<motifstats[INDEX_MOTIF_MIN]) motifstats[INDEX_MOTIF_MIN]=min;
              if (max>motifstats[INDEX_MOTIF_MAX]) motifstats[INDEX_MOTIF_MAX]=max;
            }
            counter++;
        } // end for each sequence
        for (String key:statistics.keySet()) {
             double[] motifstats=statistics.get(key);
             if (motifstats[INDEX_MOTIF_TOTAL_COUNT]==0) {
                 motifstats[INDEX_MOTIF_MIN]=Double.NaN;
                 motifstats[INDEX_MOTIF_MAX]=Double.NaN;
                 motifstats[INDEX_MOTIF_SUM]=Double.NaN;
             }
        }        
    }



    private class SortOrderComparator implements Comparator<Object[]> {
            String sortorder=null;
            public SortOrderComparator(String order) {
                sortorder=order;
            }
            @Override
            public int compare(Object[] motif1, Object[] motif2) { //
                 if (currentinternalversion<2 && sortorder.equals(SORT_BY_COUNT_ABOVE)) sortorder=SORT_BY_MOTIF_AVERAGE;
                 if (sortorder.equals(SORT_BY_COUNT_ABOVE)) {
                     Double value1=(Double)motif1[SORTED_INDEX_MOTIF_COUNT_ABOVE];
                     Double value2=(Double)motif2[SORTED_INDEX_MOTIF_COUNT_ABOVE];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;     
                     if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;
                     if (Double.isNaN(value1)) return 1;
                     if (Double.isNaN(value2)) return -1;
                     int res=value2.compareTo(value1); // Note that this sorts descending!
                     if (res!=0) return res;
                     else return ((Double)motif2[SORTED_INDEX_MOTIF_SUM]).compareTo(((Double)motif1[SORTED_INDEX_MOTIF_SUM])); // sorts descending
                } else if (sortorder.equals(SORT_BY_MOTIF_AVERAGE)) {
                     Double value1=(Double)motif1[SORTED_INDEX_MOTIF_AVERAGE];
                     Double value2=(Double)motif2[SORTED_INDEX_MOTIF_AVERAGE];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;     
                     if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;
                     if (Double.isNaN(value1)) return 1;
                     if (Double.isNaN(value2)) return -1;
                     int res=value2.compareTo(value1); // Note that this sorts descending!
                     if (res!=0) return res;
                     else return ((Double)motif2[SORTED_INDEX_MOTIF_SUM]).compareTo(((Double)motif1[SORTED_INDEX_MOTIF_SUM])); // sorts descending
                } else if (sortorder.equals(SORT_BY_MOTIF_SUM)) {
                     Double value1=(Double)motif1[SORTED_INDEX_MOTIF_SUM];
                     Double value2=(Double)motif2[SORTED_INDEX_MOTIF_SUM];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;     
                     if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;
                     if (Double.isNaN(value1)) return 1;
                     if (Double.isNaN(value2)) return -1;
                     int res=value2.compareTo(value1); // sorts descending
                     if (res!=0) return res;
                     else return ((Double)motif2[SORTED_INDEX_MOTIF_AVERAGE]).compareTo(((Double)motif1[SORTED_INDEX_MOTIF_AVERAGE])); // sorts descending
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
        StringBuilder headerString=new StringBuilder("<html>Motifs from <b>");
        headerString.append(motifCollectionName);
        headerString.append("</b> and sites from <b>");
        headerString.append(motifTrackName);
        headerString.append("</b> compared to <b>");
        headerString.append(numericTrackName);
        headerString.append("</b> on ");
        headerString.append(numberOfSequences);
        headerString.append(" sequence");
        headerString.append((numberOfSequences!=1)?"s":"");
        if (sequenceCollectionName!=null) {
            headerString.append(" from Collection <b>");
            headerString.append(sequenceCollectionName);
            headerString.append("</b>");
        }
        if (currentinternalversion>=2) {
            headerString.append("<br>Threshold used for 'count above threshold' statistic = <b>");
            headerString.append(threshold);
            headerString.append("</b>");
        }        
        headerString.append("</html>");
        headerPanel.add(new JLabel(headerString.toString()));
        AnalysisTableModel tablemodel=new AnalysisTableModel(gui);
        final GenericMotifBrowserPanel panel=new GenericMotifBrowserPanel(gui, tablemodel, modal);
        CellRenderer_Classification classrenderer=new CellRenderer_Classification();
        CellRenderer_Precision precisionRendered=new CellRenderer_Precision();
        JTable table=panel.getTable();
        table.getColumn("Class").setCellRenderer(classrenderer);
        table.getColumn("Average").setCellRenderer(precisionRendered);
        table.getColumn("Sum").setCellRenderer(precisionRendered);
        table.getColumn("ID").setPreferredWidth(60);
        table.getColumn("Class").setPreferredWidth(50);
        table.getColumn("Sites").setPreferredWidth(50);
        table.getColumn("Bases").setPreferredWidth(50);
        if (currentinternalversion>=2) table.getColumn("Above").setPreferredWidth(50);
        if (table.getRowSorter() instanceof TableRowSorter) {
            TableRowSorter rowsorter=(TableRowSorter)table.getRowSorter();
            NaNComparator nanComparator=new NaNComparator();
            for (int i=0;i<table.getColumnCount();i++) {
                if (table.getColumnClass(i)==Double.class) rowsorter.setComparator(i, nanComparator);
            }
        }
        table.getRowSorter().toggleSortOrder(AnalysisTableModel.MOTIF_AVERAGE);
        table.getTableHeader().setReorderingAllowed(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8,5,0,5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)
          ));
        displayPanel.add(headerPanel,BorderLayout.NORTH);
        displayPanel.add(panel,BorderLayout.CENTER);;
        displayPanel.setPreferredSize(getDefaultDisplayPanelDimensions());
        return displayPanel;
    }


private class AnalysisTableModel extends AbstractTableModel {
    // the following are for
    private static final int COLOR=0;    
    private static final int ID=1;
    private static final int NAME=2;
    private static final int MOTIF_CLASS=3;
    private static final int MOTIF_TOTAL_COUNT=4;
    private static final int MOTIF_BASES=5;
    private static final int MOTIF_MIN=6;
    private static final int MOTIF_MAX=7;
    private static final int MOTIF_SUM=8;
    private static final int MOTIF_AVERAGE=9;
    private static final int MOTIF_ABOVE=10;
    private static final int LOGO=11;


    private String[] columnNames=null;
    private String[] motifnames; // just to get them in a specific order
    private MotifLabEngine engine;
    private VisualizationSettings settings;

    public AnalysisTableModel(MotifLabGUI gui) {
        this.engine=gui.getEngine();
        settings=gui.getVisualizationSettings();
        if (currentinternalversion>=2) {
            columnNames=new String[]{" ","ID","Name","Class","Sites","Bases","Min","Max","Sum","Average","Above","Logo"};          
        } else {
            columnNames=new String[]{" ","ID","Name","Class","Sites","Bases","Min","Max","Sum","Average","Logo"};
        }

        motifnames=new String[statistics.size()];
        int i=0;
        for (String name:statistics.keySet()) {
           motifnames[i]=name;
           i++;
        }
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        if (currentinternalversion<2 && columnIndex==MOTIF_ABOVE) columnIndex=LOGO; // ad hoc version compatibility fix
        switch (columnIndex) {
            case COLOR:return Color.class;            
            case ID:return String.class;
            case NAME:return String.class;
            case MOTIF_CLASS:return String.class;
            case MOTIF_TOTAL_COUNT:return Integer.class;
            case MOTIF_BASES:return Integer.class;
            case MOTIF_MIN:return Double.class;
            case MOTIF_MAX:return Double.class;
            case MOTIF_SUM:return Double.class;
            case MOTIF_AVERAGE:return Double.class;
            case MOTIF_ABOVE:return Integer.class;                
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
        if (currentinternalversion<2 && columnIndex==MOTIF_ABOVE) columnIndex=LOGO; // ad hoc version compatibility fix        
        String motifname=motifnames[rowIndex];
        double[] stat=(columnIndex==ID)?null:statistics.get(motifname);
        switch (columnIndex) {
            case COLOR:return settings.getFeatureColor(motifname);
            case ID:return motifname;
            case NAME:return getMotifName(motifname);
            case MOTIF_CLASS:return getMotifClass(motifname);
            case MOTIF_TOTAL_COUNT:return (int)stat[INDEX_MOTIF_TOTAL_COUNT];
            case MOTIF_BASES:return (int)stat[INDEX_MOTIF_BASES];
            case MOTIF_MIN:return (double)stat[INDEX_MOTIF_MIN];
            case MOTIF_MAX:return (double)stat[INDEX_MOTIF_MAX];
            case MOTIF_SUM:return (double)stat[INDEX_MOTIF_SUM];
            case MOTIF_ABOVE:return (int)stat[INDEX_MOTIF_COUNT_ABOVE];    
            case MOTIF_AVERAGE:return (stat[INDEX_MOTIF_BASES]==0)?Double.NaN:(double)stat[INDEX_MOTIF_SUM]/stat[INDEX_MOTIF_BASES];
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
         return value2.compareTo(value1); // Note that this sorts descending!
    }
}

    // ------------ Serialization ---------
    private static final long serialVersionUID = 1L;

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
         out.writeShort(currentinternalversion);
         out.defaultWriteObject();
         out.writeDouble(threshold);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
         currentinternalversion=in.readShort(); // the internalversion number is used to determine correct format of data
         if (currentinternalversion>2) throw new ClassNotFoundException("Newer version");
         in.defaultReadObject();
         threshold=0;
         if (currentinternalversion==2) {
             threshold=in.readDouble();            
         }
    }
}
