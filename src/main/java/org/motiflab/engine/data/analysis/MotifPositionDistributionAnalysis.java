/*
 
 
 */

package org.motiflab.engine.data.analysis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.Graph;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.util.MotifPositionHistogramComputer;
import org.motiflab.gui.GenericMotifBrowserPanel;
import org.motiflab.gui.MotifLogo;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.VisualizationSettings;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifClassification;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifNumericMap;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;

/**
 *
 * @author kjetikl
 */
public class MotifPositionDistributionAnalysis extends Analysis {
    private final static String typedescription="Analysis: motif position distribution";
    private final static String analysisName="motif position distribution";
    private final static String description="Analyzes the positional distribution of each motif to see whether their binding sites have a tendency to occur a certain locations";
    private static final String SORT_BY_MOTIF="Motif ID";
    private static final String SORT_BY_STDDEV="Std.dev.";
    private static final String SORT_BY_KURTOSIS="Kurtosis";
    private HashMap<String,double[]> statistics=null; // key is motif name.
    private HashMap<String,double[]> histograms=null; // key is motif name.
    private String sequenceCollectionName=null;
    private String motifCollectionName=null;    
    private String motifTrackName=null;
    private String anchorString=null;
    private String motifanchorString=null; 
    private boolean flatten=false;    
    private int bins=100;
    private int numberOfSequences=0;
    private int anchor=USE_ANCHOR_TSS; // this will be set later
    
    private static final String ANCHOR_TSS="TSS";
    private static final String ANCHOR_TES="TES";
    private static final String ANCHOR_UPSTREAM="Upstream";
    private static final String ANCHOR_DOWNSTREAM="Downstream";
    private static final String ANCHOR_CENTER="Center";    
    
    private static final int USE_ANCHOR_TSS=0;
    private static final int USE_ANCHOR_TES=1;
    private static final int USE_ANCHOR_UPSTREAM=2;
    private static final int USE_ANCHOR_DOWNSTREAM=3;
    private static final int USE_ANCHOR_CENTER=4;    
    
    // These are indexes into the statistics values (double[])
    private static final int INDEX_MOTIF_TOTAL_COUNT=0;
    private static final int INDEX_MOTIF_SEQUENCE_COUNT=1;
    private static final int INDEX_STDDEV=2;
    private static final int INDEX_KURTOSIS=3;

    private static final int SORTED_INDEX_MOTIF_ID=0;
    private static final int SORTED_INDEX_MOTIF_TOTAL_COUNT=1;
    private static final int SORTED_INDEX_MOTIF_SEQUENCE_COUNT=2;
    private static final int SORTED_INDEX_STDDEV=3;
    private static final int SORTED_INDEX_KURTOSIS=4;
    
    private static final String HISTORGRAMS_IMAGES_NEW="New images";
    private static final String HISTOGRAMS_EMBED="Embedded images"; 
    private static final String HISTOGRAMS_NONE="No";     
    


    public MotifPositionDistributionAnalysis() {
        this.name="MotifPositionDistributionAnalysis_temp";
        addParameter("Motif track",RegionDataset.class, null,new Class[]{RegionDataset.class},"A region track containing motif sites to estimate positional distributions for",true,false);
        addParameter("Motifs",MotifCollection.class, null,new Class[]{MotifCollection.class},"The motifs to consider in this analysis",true,false);
        addParameter("Sequences",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"If specified, the analysis will be limited to sequences in this collection",false,false);
        addParameter("Alignment anchor",String.class, "TSS",new String[]{ANCHOR_TSS,ANCHOR_TES,ANCHOR_UPSTREAM,ANCHOR_DOWNSTREAM,ANCHOR_CENTER},"An alignment anchor for each sequence which will serve as the reference point when estimating the relative position of each motif site",false,false);
        addParameter("Include histograms",Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Specifies whether or not to store histograms for each motif in the analysis. This must be selected if you want to output histograms to HTML or Excel or use the histogram column in a collated analysis.",false,false);
        addParameter("Motif anchor",String.class, "Span",new String[]{"Upstream","Downstream","Center","Span"},"Specifies how to select the target bin(s) in the histogram in relation to the location of a motif region",false,false,true);
        addParameter("Support",Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, each bin will only be counted once for each sequence and the histogram will reflect the number of sequences that have a region for that bin, not the total number of regions that are assigned to the bin.",false,false,true);
        addParameter("Bins",Integer.class,100,new Integer[]{1,10000},"Specifies how many bins to divide the sequence range into for the histograms",false,false,true);
    }

    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Motif track","Motifs"};}
    
    @Override
    public boolean canUseAsSourceProxy(Data data) {
        if (data instanceof RegionDataset) return ((RegionDataset)data).isMotifTrack(); // only allow Motif Tracks as input
        else return (data instanceof MotifCollection);
    }  
    
    @Override
    public Parameter[] getOutputParameters(String dataformat) {       
        Parameter incPar  = new Parameter("Include",MotifCollection.class,null,new Class[]{MotifCollection.class},"Only include data from this collection",false,false);                        
        Parameter sortPar = new Parameter("Sort by",String.class,SORT_BY_KURTOSIS, new String[]{SORT_BY_MOTIF,SORT_BY_STDDEV,SORT_BY_KURTOSIS},"Sorting order for the results table",false,false);
        Parameter logos   = new Parameter("Logos",String.class,getMotifLogoDefaultOption(dataformat), getMotifLogoOptions(dataformat),"Include motif sequence logos in the output",false,false);
        Parameter histPar = new Parameter("Histograms",Boolean.class,Boolean.TRUE, new Boolean[]{Boolean.FALSE,Boolean.TRUE},"Include histograms in the output. Note that the histograms must have been computed when the analysis was executed in order for them to be output.",false,false);
        Parameter histImg = new Parameter("Histogram images",String.class,HISTOGRAMS_NONE, new String[]{HISTORGRAMS_IMAGES_NEW, HISTOGRAMS_EMBED, HISTOGRAMS_NONE},"Include histograms in the output. Note that the histograms must have been computed when the analysis was executed in order for them to be output.",false,false);        
        Parameter colPar  = new Parameter("Color boxes",Boolean.class,Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a box with the assigned color for the motif will be output as the first column",false,false);
        Parameter legend  = new Parameter("Legend",Boolean.class,Boolean.TRUE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a header with a title and analysis details will be included at the top of the Excel sheet.",false,false);       
        if (dataformat.equals(HTML)) return new Parameter[]{incPar,sortPar,logos,histImg,colPar};
        if (dataformat.equals(EXCEL)) return new Parameter[]{incPar,sortPar,logos,histPar,legend};
        if (dataformat.equals(RAWDATA)) return new Parameter[]{incPar,sortPar,logos};       
        return new Parameter[0];
    }
        

    @Override
    public String[] getResultVariables() {
        return new String[]{"Standard deviation","Kurtosis"};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename==null || variablename.isEmpty()) throw new ExecutionError("'"+getName()+"' does not have a result for ''");
        else if (variablename.equals("Standard deviation")) {
            MotifNumericMap map=new MotifNumericMap("temp",0);
            for (String motifname:statistics.keySet()) {
                double[] stats=statistics.get(motifname);
                map.setValue(motifname, stats[INDEX_STDDEV]);
            }
            return map;
        }
        else if (variablename.equals("Kurtosis")) {
            MotifNumericMap map=new MotifNumericMap("temp",0);
            for (String motifname:statistics.keySet()) {
                double[] stats=statistics.get(motifname);
                map.setValue(motifname, stats[INDEX_KURTOSIS]);
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
        if (histograms!=null) return new String[]{"Count","Sequences","Standard deviation","Kurtosis","Histogram"};
        else return new String[]{"Count","Sequences","Standard deviation","Kurtosis"};
    }

    @Override
    public Class getColumnType(String column) {
             if (column.equalsIgnoreCase("Count") || column.equalsIgnoreCase("Sequences")) return Integer.class;
        else if (column.equalsIgnoreCase("Standard deviation") || column.equalsIgnoreCase("Kurtosis")) return Double.class;
        else if (column.equalsIgnoreCase("Histogram") && histograms!=null) return double[].class;
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
            else if (column.equalsIgnoreCase("Standard deviation")) columnData.put(motifname, new Double(stat[INDEX_STDDEV]));
            else if (column.equalsIgnoreCase("Kurtosis")) columnData.put(motifname, new Double(stat[INDEX_KURTOSIS]));
            else if (column.equalsIgnoreCase("Histogram")) columnData.put(motifname, histograms.get(motifname));
        }
        return columnData;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MotifPositionDistributionAnalysis clone() {
        MotifPositionDistributionAnalysis newanalysis=new MotifPositionDistributionAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.statistics=(HashMap<String,double[]>)this.statistics.clone();
        newanalysis.sequenceCollectionName=this.sequenceCollectionName;
        newanalysis.motifCollectionName=this.motifCollectionName;
        newanalysis.motifTrackName=this.motifTrackName;
        newanalysis.numberOfSequences=this.numberOfSequences;          
        newanalysis.anchorString=this.anchorString;          
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        MotifPositionDistributionAnalysis other=((MotifPositionDistributionAnalysis)source);
        this.statistics=(HashMap<String,double[]>)other.statistics;
        this.sequenceCollectionName=other.sequenceCollectionName;
        this.motifCollectionName=other.motifCollectionName;
        this.motifTrackName=other.motifTrackName;
        this.numberOfSequences=other.numberOfSequences;    
        this.anchorString=other.anchorString;
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
            entry[SORTED_INDEX_STDDEV]=new Double(values[INDEX_STDDEV]);
            entry[SORTED_INDEX_KURTOSIS]=new Double(values[INDEX_KURTOSIS]);
            resultList.add(entry);
        }
        Collections.sort(resultList, new SortOrderComparator(sortorder));
        return resultList;
    }

    @Override
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        AnnotatedValueRenderer histogramrenderer=new AnnotatedValueRenderer(engine.getClient().getVisualizationSettings());
        format.setProgress(5);
        String sortorder=SORT_BY_MOTIF;
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        Color [] basecolors=vizSettings.getBaseColors();
        MotifLogo sequencelogo=new MotifLogo(basecolors,sequencelogoSize);
        String showSequenceLogosString="";
        String histogramFormat=null;
        boolean showColorBoxes=false;
        MotifCollection include=null;
        if (settings!=null) {
            try {
                Parameter[] defaults=getOutputParameters(format);
                sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
                showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
                showColorBoxes=(Boolean)settings.getResolvedParameter("Color boxes",defaults,engine); 
                include=(MotifCollection)settings.getResolvedParameter("Include",defaults,engine);  
                histogramFormat=(String)settings.getResolvedParameter("Histogram images",defaults,engine);  
                engine.logMessage("Histogram images="+histogramFormat);
                if (histogramFormat==null || histogramFormat.equalsIgnoreCase("No")) histogramFormat=null;
                else if (histogramFormat.toLowerCase().startsWith("embed")) histogramFormat="embed";
                else histogramFormat="png";                             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        if (histograms==null) histogramFormat=null; // we can't output histograms if they are not computed
        boolean showSequenceLogos = includeLogosInOutput(showSequenceLogosString);

        ArrayList<Object[]> resultList=assembleList(sortorder, include);
        engine.createHTMLheader("Motif Position Distribution Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<h1 class=\"headline\">Motif Position Distribution Analysis</h1>\n",HTML);
        outputobject.append("<div class=\"summary\">\n",HTML);
        outputobject.append("Positional distribution analysis with motifs from <span class=\"dataitem\">",HTML);
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
        outputobject.append("\n</div>\n<br>\n",HTML);
        outputobject.append("<table class=\"sortable\">\n",HTML);
        String logoheader=(showSequenceLogos)?"<th class=\"sorttable_nosort\">Logo</th>":"";
        String histogramheader=(histogramFormat!=null)?"<th class=\"sorttable_nosort\">Histogram</th>":"";
        outputobject.append("<tr>",HTML);
        if (showColorBoxes) outputobject.append("<th>&nbsp;</th>",HTML);               
        outputobject.append("<th>ID</th><th>Name</th><th class=\"sorttable_ip\">Class</th><th>Total</th><th>Sequences</th><th class=\"sorttable_numeric\">%</th><th>Std.dev.</th><th>Kurtosis</th>"+histogramheader+logoheader+"</tr>",HTML);
        //DecimalFormat decimalformatter=DataFormat.getDecimalFormatter(3);
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[SORTED_INDEX_MOTIF_ID];
            int seqcount=((Double)entry[SORTED_INDEX_MOTIF_SEQUENCE_COUNT]).intValue();
            int totalcount=((Double)entry[SORTED_INDEX_MOTIF_TOTAL_COUNT]).intValue();
            Double stddev=(Double)entry[SORTED_INDEX_STDDEV];
            Double kurtosis=(Double)entry[SORTED_INDEX_KURTOSIS];
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
            outputobject.append("<td class=\"num\">"+totalcount+"</td><td class=\"num\">"+seqcount+"</td><td class=\"num\">"+(int)((double)seqcount*100.0/(double)numberOfSequences)+"%</td><td class=\"num\">"+Graph.formatNumber(stddev,false)+"</td><td class=\"num\">"+Graph.formatNumber(kurtosis,false)+"</td>",HTML);
            if (histogramFormat!=null) {
               outputobject.append("<td>",HTML);
               double[] histogram=histograms.get(motifname);
               histogramrenderer.outputHistogramToHTML(outputobject, histogramFormat, histogram, engine);
               outputobject.append("</td>",HTML);
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
        AnnotatedValueRenderer histogramrenderer=new AnnotatedValueRenderer(vizSettings);       
        Color [] basecolors=vizSettings.getBaseColors();
        boolean border=(Boolean)vizSettings.getSettingAsType("motif.border", Boolean.TRUE);        
        MotifLogo sequencelogo=new MotifLogo(basecolors,sequencelogoSize);
        String showSequenceLogosString="";
        boolean includeLegend=false;
        Boolean showHistograms=false;
        int logoheight=19;
        MotifCollection include=null;
        if (settings!=null) {
            try {
                Parameter[] defaults=getOutputParameters(format);
                sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
                showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
                includeLegend=(Boolean)settings.getResolvedParameter("Legend",defaults,engine); 
                include=(MotifCollection)settings.getResolvedParameter("Include",defaults,engine);               
                showHistograms=(Boolean)settings.getResolvedParameter("Histograms",defaults,engine); 
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos = includeLogosInOutput(showSequenceLogosString);
        boolean showLogosAsImages = includeLogosInOutputAsImages(showSequenceLogosString);          
    
        if (histograms==null) showHistograms=false;
        ArrayList<Object[]> resultList=assembleList(sortorder,include);
        int rownum=0;
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Motif Position Distribution");
        CreationHelper helper = (showLogosAsImages||showHistograms)?workbook.getCreationHelper():null;
        Drawing drawing = (showLogosAsImages||showHistograms)?sheet.createDrawingPatriarch():null; 
             
        CellStyle title = getExcelTitleStyle(workbook);
        CellStyle tableheader = getExcelTableHeaderStyle(workbook);
        
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
        int histogramcolumn=0;
        row = sheet.createRow(rownum);
        String[] firstColumns=new String[]{"Motif ID","Name","Class","Total","Sequences","Std.dev.","Kurtosis"};
        outputStringValuesInCells(row, firstColumns, 0, tableheader);      
        col+=firstColumns.length;
        if (showHistograms) {
            histogramcolumn=col;
            outputStringValueInCell(row, col, "Histogram", tableheader);   
            col++;
        }
        if (showSequenceLogos) {
            logocolumn=col;
            outputStringValuesInCells(row, new String[]{"Logo"}, logocolumn, tableheader);
        }
        int maxlogowidth=0; // the number of bases in the longest motif 
        if (showHistograms) sheet.setColumnWidth(histogramcolumn, 3800);       
        if (showLogosAsImages) sheet.setColumnWidth(logocolumn, 10000);       
        for (int i=0;i<resultList.size();i++) {
            rownum++;
            row = sheet.createRow(rownum);
            col=0;            
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[SORTED_INDEX_MOTIF_ID];
            int seqcount=((Double)entry[SORTED_INDEX_MOTIF_SEQUENCE_COUNT]).intValue();
            int totalcount=((Double)entry[SORTED_INDEX_MOTIF_TOTAL_COUNT]).intValue();
            Double stddev=(Double)entry[SORTED_INDEX_STDDEV];
            Double kurtosis=(Double)entry[SORTED_INDEX_KURTOSIS];
            Motif motif=null;
            if (engine.dataExists(motifname, Motif.class)) motif=(Motif)engine.getDataItem(motifname);
            String shortname=(motif!=null)?motif.getShortName():motifname;
            String motifclass=(motif!=null)?motif.getClassification():null;
            if (motifclass==null) motifclass="unknown";
            outputStringValuesInCells(row, new String[]{motifname,shortname,motifclass}, col);
            col+=3;
            outputNumericValuesInCells(row, new double[]{totalcount,seqcount,stddev,kurtosis}, col);
            col+=4;
            if (showHistograms||showLogosAsImages) row.setHeightInPoints((short)(sheet.getDefaultRowHeightInPoints()*1.2));
            if (showHistograms) {
                double[] histogram=histograms.get(motifname);      
                try {
                    byte[] image=histogramrenderer.outputHistogramToByteArray(histogram);
                    int imageIndex=workbook.addPicture(image, XSSFWorkbook.PICTURE_TYPE_PNG);
                    ClientAnchor picanchor = helper.createClientAnchor();
                    picanchor.setCol1(histogramcolumn);
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
            }    
            if (showSequenceLogos && motif!=null) {
                if (showLogosAsImages) {
                    try {                        
                        sequencelogo.setMotif(motif);
                        int width=motif.getLength();
                        if (width>maxlogowidth) maxlogowidth=width;
                        byte[] image=getMotifLogoImageAsByteArray(sequencelogo, logoheight, border, "png");
                        int imageIndex=workbook.addPicture(image, XSSFWorkbook.PICTURE_TYPE_PNG);
                        ClientAnchor picanchor = helper.createClientAnchor();
                        picanchor.setCol1(logocolumn);
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
        
        autoSizeExcelColumns(sheet, 0, col-1, 800);
        if (!showLogosAsImages) sheet.autoSizeColumn((short)logocolumn);  

        if (includeLegend) {        
            sheet.createFreezePane(0,headerrows,0,headerrows);
            row=sheet.getRow(0);
            outputStringValueInCell(row, 0, "Motif position distribution analysis", title);
            StringBuilder firstLine=new StringBuilder();
            firstLine.append("Positional distribution analysis with motifs from \"");
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
            firstLine.append(".");
            row=sheet.getRow(2);
            outputStringValueInCell(row, 0, firstLine.toString(), null); 
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
        AnnotatedValueRenderer histogramrenderer=new AnnotatedValueRenderer(engine.getClient().getVisualizationSettings());
        String sortorder=SORT_BY_MOTIF;
        String showSequenceLogosString="";
        boolean showHistograms=true;
        MotifCollection include=null;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
             showHistograms=(Boolean)settings.getResolvedParameter("Histograms",defaults,engine);
             include=(MotifCollection)settings.getResolvedParameter("Include",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        if (histograms==null) showHistograms=false; // we can't output histograms if they are not computed
        boolean showSequenceLogos=includeLogosInOutput(showSequenceLogosString);

        ArrayList<Object[]> resultList=assembleList(sortorder, include);
        outputobject.append("#Positional distribution analysis with motifs from '"+motifCollectionName+"' and sites from '"+motifTrackName+"'",RAWDATA);
        outputobject.append(" on "+numberOfSequences+" sequence"+((numberOfSequences!=1)?"s":""),RAWDATA);
        if (sequenceCollectionName!=null) outputobject.append(" from collection '"+sequenceCollectionName+"'",RAWDATA);
        outputobject.append("\n\n#Motif ID, total number of motif occurrences, sequences containing the motif, std.dev., kurtosis",RAWDATA);
        if (showHistograms) outputobject.append(", histogram",RAWDATA);
        if (showSequenceLogos) outputobject.append(", motif consensus",RAWDATA);
        outputobject.append("\n",RAWDATA);
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[SORTED_INDEX_MOTIF_ID];
            int seqcount=((Double)entry[SORTED_INDEX_MOTIF_SEQUENCE_COUNT]).intValue();
            int totalcount=((Double)entry[SORTED_INDEX_MOTIF_TOTAL_COUNT]).intValue();
            Double stddev=(Double)entry[SORTED_INDEX_STDDEV];
            Double kurtosis=(Double)entry[SORTED_INDEX_KURTOSIS];
            outputobject.append(motifname,RAWDATA);
            outputobject.append("\t"+totalcount,RAWDATA);
            outputobject.append("\t"+seqcount,RAWDATA);
            outputobject.append("\t"+((stddev==null || Double.isNaN(stddev))?"":stddev),RAWDATA);
            outputobject.append("\t"+((kurtosis==null || Double.isNaN(kurtosis))?"":kurtosis),RAWDATA);
            if (showHistograms) {
               outputobject.append("\t",RAWDATA);
               double[] histogram=histograms.get(motifname);
               histogramrenderer.outputHistogramToRaw(outputobject, histogram);
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
    public void runAnalysis(OperationTask task) throws Exception {
        RegionDataset source=(RegionDataset)task.getParameter("Motif track");
        if (!source.isMotifTrack()) throw new ExecutionError("Motif position distribution analysis can only be performed on motif tracks");
        motifTrackName=source.getName();
        MotifCollection motifcollection=(MotifCollection)task.getParameter("Motifs");
        motifCollectionName=motifcollection.getName();
        statistics=new HashMap<String,double[]>(motifcollection.size());
        anchorString=(String)task.getParameter("Alignment anchor");
        
        Boolean includeHistograms=(Boolean)task.getParameter("Include histograms");
        if (includeHistograms==null) includeHistograms=Boolean.FALSE;
        Integer binsInteger=(Integer)task.getParameter("Bins");
        if (binsInteger!=null) bins=binsInteger;
        motifanchorString=(String)task.getParameter("Motif anchor");
        if (motifanchorString==null) motifanchorString="Span";
        Boolean flattenValue=(Boolean)task.getParameter("Support");
        if (flattenValue!=null) flatten=flattenValue;
        MotifPositionHistogramComputer computer=null;
        if (includeHistograms) {
            int alignmentanchor=MotifPositionHistogramComputer.ALIGN_TSS;
                 if (anchorString.equals(ANCHOR_TSS)) alignmentanchor=MotifPositionHistogramComputer.ALIGN_TSS;
            else if (anchorString.equals(ANCHOR_TES)) alignmentanchor=MotifPositionHistogramComputer.ALIGN_TES;
            else if (anchorString.equals(ANCHOR_UPSTREAM)) alignmentanchor=MotifPositionHistogramComputer.ALIGN_UPSTREAM;
            else if (anchorString.equals(ANCHOR_DOWNSTREAM)) alignmentanchor=MotifPositionHistogramComputer.ALIGN_DOWNSTREAM;          
            else if (anchorString.equals(ANCHOR_CENTER)) alignmentanchor=MotifPositionHistogramComputer.ALIGN_CENTER;          
            int motifanchor=MotifPositionHistogramComputer.ANCHOR_SPAN;
                 if (motifanchorString.equals("Upstream")) motifanchor=MotifPositionHistogramComputer.ANCHOR_UPSTREAM;
            else if (motifanchorString.equals("Downstream")) motifanchor=MotifPositionHistogramComputer.ANCHOR_DOWNSTREAM;
            else if (motifanchorString.equals("Center")) motifanchor=MotifPositionHistogramComputer.ANCHOR_CENTER;
            else if (motifanchorString.equals("Span")) motifanchor=MotifPositionHistogramComputer.ANCHOR_SPAN;        
            computer=new MotifPositionHistogramComputer(null, alignmentanchor, motifanchor, flatten, source);
            histograms=new HashMap<String, double[]>(motifcollection.getNumberofMotifs());
        }
        SequenceCollection collection=(SequenceCollection)task.getParameter("Sequences");
        if (collection==null) collection=task.getEngine().getDefaultSequenceCollection();
        else sequenceCollectionName=collection.getName();
        if (sequenceCollectionName!=null && sequenceCollectionName.equals(task.getEngine().getDefaultSequenceCollectionName())) sequenceCollectionName=null;
        numberOfSequences=collection.size();
        ArrayList<String> sequenceNames=collection.getAllSequenceNames();    

        anchor=0;
             if (anchorString.equals(ANCHOR_TSS)) anchor=USE_ANCHOR_TSS;
        else if (anchorString.equals(ANCHOR_TES)) anchor=USE_ANCHOR_TES;
        else if (anchorString.equals(ANCHOR_UPSTREAM)) anchor=USE_ANCHOR_UPSTREAM;
        else if (anchorString.equals(ANCHOR_DOWNSTREAM)) anchor=USE_ANCHOR_DOWNSTREAM;
        else if (anchorString.equals(ANCHOR_CENTER)) anchor=USE_ANCHOR_CENTER;   

        TaskRunner taskRunner=task.getEngine().getTaskRunner();
        task.setProgress(0L,motifcollection.size());
        long[] counters=new long[]{0,0,motifcollection.size()}; // counters[0]=motifs started, [1]=motifs completed, [2]=total number of motifs

        ArrayList<ProcessMotifTask> processTasks=new ArrayList<ProcessMotifTask>(motifcollection.size());
        for (int i=0;i<motifcollection.size();i++) {
            String motifname=motifcollection.getMotifNameByIndex(i);
            Data data=task.getEngine().getDataItem(motifname);
            if (data==null || !(data instanceof Motif)) throw new ExecutionError(motifname+" is not a known motif");
            Motif motif=(Motif)data;
            processTasks.add(new ProcessMotifTask(motif, sequenceNames, source, computer, task, counters));
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
    
    private Object[] processMotif(String motifname, ArrayList<String> sequenceNames, RegionDataset source, MotifPositionHistogramComputer computer) throws ExecutionError {
        DescriptiveStatistics descriptiveStatistics=new DescriptiveStatistics();
        int totalmotifcount=0;
        int sequencecount=0;

        for (String sequenceName:sequenceNames) {
            RegionSequenceData seq=(RegionSequenceData)source.getSequenceByName(sequenceName);
            if (anchor==USE_ANCHOR_TSS && seq.getTSS()==null) throw new ExecutionError("Sequence '"+seq.getSequenceName()+"' is missing TSS");
            if (anchor==USE_ANCHOR_TES && seq.getTES()==null) throw new ExecutionError("Sequence '"+seq.getSequenceName()+"' is missing TES");
            boolean present=false;          
            boolean reverse=(seq.getStrandOrientation()==Sequence.REVERSE);                
            for (Region r:seq.getOriginalRegions()) {
              if (r.getType().equals(motifname)) {
                  totalmotifcount++;
                  present=true;
                  double motifpos=r.getGenomicStart()+r.getLength()/2.0; // use center position of the motif as the anchor when calculating statistics (except for histogram which has its own motif anchor)
                  double relativemotifpos=0;
                       if (anchor==USE_ANCHOR_TSS) relativemotifpos=(reverse)?(seq.getTSS()-motifpos):(motifpos-seq.getTSS());
                  else if (anchor==USE_ANCHOR_TES) relativemotifpos=(reverse)?(seq.getTES()-motifpos):(motifpos-seq.getTES());
                  else if (anchor==USE_ANCHOR_UPSTREAM) relativemotifpos=(reverse)?(seq.getRegionEnd()-motifpos):(motifpos-seq.getRegionStart());
                  else if (anchor==USE_ANCHOR_DOWNSTREAM) relativemotifpos=(reverse)?(seq.getRegionStart()-motifpos):(motifpos-seq.getRegionEnd());
                  else if (anchor==USE_ANCHOR_CENTER) {
                      int sequenceCenter=(int)((seq.getRegionStart()+seq.getRegionEnd())/2.0);
                      relativemotifpos=(reverse)?(sequenceCenter-motifpos):(motifpos-sequenceCenter);
                  }
                  descriptiveStatistics.addValue(relativemotifpos);
              }
            } 
            if (present) sequencecount++;

        } // end for each sequence

        double[] motifstats=new double[4];
        motifstats[INDEX_MOTIF_TOTAL_COUNT]=totalmotifcount;
        motifstats[INDEX_MOTIF_SEQUENCE_COUNT]=sequencecount;
        motifstats[INDEX_STDDEV]=descriptiveStatistics.getStandardDeviation();
        motifstats[INDEX_KURTOSIS]=descriptiveStatistics.getKurtosis(); 
        double[] counts=null;
        if (computer!=null) {
           counts=new double[bins];
           computer.countRegions(counts,motifname,sequenceNames,false); 
           histograms.put(motifname,counts);               
        }     
        return new Object[]{motifstats,counts};
    }

    
    protected class ProcessMotifTask implements Callable<Motif> {
        final Motif motif;
        final ArrayList<String> sequenceNames;
        final RegionDataset motiftrack;
        final MotifPositionHistogramComputer computer;
        final long[] counters; // counters[0]=motifs started, [1]=motifs completed, [2]=total number of motifs.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final ExecutableTask task;  
        
        public ProcessMotifTask(Motif motif, ArrayList<String> sequenceNames, RegionDataset motiftrack, MotifPositionHistogramComputer computer, ExecutableTask task, long[] counters) {
           this.motif=motif;
           this.motiftrack=motiftrack;
           this.computer=computer;
           this.sequenceNames=sequenceNames;
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
            
            Object[] motifresults=processMotif(motif.getName(), sequenceNames, motiftrack, computer);          
            
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                double[] motifstats=(double[])motifresults[0];             
                statistics.put(motif.getName(),motifstats); 
                if (computer!=null) {
                   double[] counts=(double[])motifresults[1]; 
                   histograms.put(motif.getName(),counts);                   
                }
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
             if (sortorder.equals(SORT_BY_STDDEV)) {
                 Double value1=(Double)motif1[SORTED_INDEX_STDDEV];
                 Double value2=(Double)motif2[SORTED_INDEX_STDDEV];
                 if (value1==null && value2==null) return 0;
                 if (value1==null) return 1;
                 if (value2==null) return -1;
                 if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;                     
                 if (Double.isNaN(value1)) return 1;
                 if (Double.isNaN(value2)) return -1;
                 int res=value1.compareTo(value2); 
                 if (res!=0) return res;
                 else return ((Double)motif2[SORTED_INDEX_KURTOSIS]).compareTo(((Double)motif1[SORTED_INDEX_KURTOSIS])); // sorts descending
            } else if (sortorder.equals(SORT_BY_KURTOSIS)) {
                 Double value1=(Double)motif1[SORTED_INDEX_KURTOSIS];
                 Double value2=(Double)motif2[SORTED_INDEX_KURTOSIS];
                 if (value1==null && value2==null) return 0;
                 if (value1==null) return 1;
                 if (value2==null) return -1;             
                 if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;
                 if (Double.isNaN(value1)) return 1;
                 if (Double.isNaN(value2)) return -1;
                 int res=value2.compareTo(value1); // sorts descending
                 if (res!=0) return res;
                 else return ((Double)motif2[SORTED_INDEX_STDDEV]).compareTo(((Double)motif1[SORTED_INDEX_STDDEV])); // sorts descending
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
        StringBuilder headerString=new StringBuilder("<html>Positional distribution analysis with motifs from <b>");
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
        headerString.append("</html>");
        headerPanel.add(new JLabel(headerString.toString()));
        MotifTableModel tablemodel=new MotifTableModel(gui);
        final GenericMotifBrowserPanel panel=new GenericMotifBrowserPanel(gui, tablemodel, modal);
        JTable table=panel.getTable();        
        CellRenderer_Classification classrenderer=new CellRenderer_Classification();
        CellRenderer_Precision precisionRenderer=new CellRenderer_Precision();
        final CellRenderer_Histogram histogramRenderer=new CellRenderer_Histogram(gui,table);        
        table.getColumn("Class").setCellRenderer(classrenderer);
        table.getColumn("Std.dev.").setCellRenderer(precisionRenderer);
        table.getColumn("Kurtosis").setCellRenderer(precisionRenderer);
        table.getColumn("Histogram").setCellRenderer(histogramRenderer);
        table.getColumn("Histogram").setPreferredWidth(100);
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
        table.getRowSorter().toggleSortOrder(MotifTableModel.KURTOSIS);
        //table.getRowSorter().toggleSortOrder(MotifTableModel.KURTOSIS);

        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8,5,0,5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)
          ));
        displayPanel.add(headerPanel,BorderLayout.NORTH);
        displayPanel.add(panel,BorderLayout.CENTER);
        displayPanel.setPreferredSize(getDefaultDisplayPanelDimensions());
        JComboBox motifAnchorCombobox=new JComboBox(new String[]{"Upstream","Downstream","Center","Span"});
        if (motifanchorString!=null) motifAnchorCombobox.setSelectedItem(motifanchorString);
        else motifAnchorCombobox.setSelectedItem("Span"); // this will also trigger computation of histograms        
        motifAnchorCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selection=(String)((JComboBox)e.getSource()).getSelectedItem();
                histogramRenderer.setMotifAnchor(selection,true);
            }
        });
        JCheckBox flattenCheckbox=new JCheckBox("    Support",flatten);
        flattenCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
        flattenCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                histogramRenderer.setFlatten(((JCheckBox)e.getSource()).isSelected(),true);
            }
        });
        JLabel motifAnchorLabel = new JLabel("    Motif anchor ");
        panel.getControlsPanel().add(motifAnchorLabel);
        panel.getControlsPanel().add(motifAnchorCombobox);
        panel.getControlsPanel().add(flattenCheckbox);
        if (histograms!=null) {
           histogramRenderer.setHistograms(histograms);
        } else {
            histogramRenderer.setMotifAnchor(motifanchorString,false);
            histogramRenderer.setFlatten(flatten,false);
            histogramRenderer.updateHistograms();
        }
        if (gui.getEngine().getDataItem(motifTrackName,RegionDataset.class)==null) { // data has been deleted
            motifAnchorLabel.setEnabled(false);
            motifAnchorCombobox.setEnabled(false);
            motifAnchorCombobox.setToolTipText("Original dataset has been deleted");
            flattenCheckbox.setEnabled(false);
            flattenCheckbox.setToolTipText("Original dataset has been deleted");
        }
        return displayPanel;
    }

 
    
private class MotifTableModel extends AbstractTableModel {
    // the following are for
    private static final int COLOR=0;    
    private static final int ID=1;
    private static final int NAME=2;
    private static final int MOTIF_CLASS=3;    
    private static final int MOTIF_TOTAL_COUNT=4;
    private static final int MOTIF_SEQUENCE_COUNT=5;    
    private static final int STDDEV=6;
    private static final int KURTOSIS=7;    
    private static final int HISTOGRAM=8;       
    private static final int LOGO=9;
    
    private String[] columnNames=null;
    private String[] motifnames; // just to get them in a specific order
    private MotifLabEngine engine;
    private VisualizationSettings settings;

    public MotifTableModel(MotifLabGUI gui) {
        this.engine=gui.getEngine();
        settings=gui.getVisualizationSettings();
        columnNames=new String[]{" ","ID","Name","Class","Count","Sequences","Std.dev.","Kurtosis","Histogram","Logo"};        
        motifnames=new String[statistics.size()];      
        int i=0;
        for (String name:statistics.keySet()) {
           motifnames[i]=name;
           i++;
        }
    }

    public String[] getMotifNames() {
        return motifnames;
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
            case STDDEV:return Double.class;
            case KURTOSIS:return Double.class;  
            case HISTOGRAM:return String.class;                   
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
        double[] stat=statistics.get(motifname);
        switch (columnIndex) {
            case COLOR:return settings.getFeatureColor(motifname);            
            case ID:return motifname;
            case NAME:return getMotifName(motifname);    
            case MOTIF_CLASS:return getMotifClass(motifname);                
            case MOTIF_TOTAL_COUNT:return (int)stat[INDEX_MOTIF_TOTAL_COUNT];
            case MOTIF_SEQUENCE_COUNT:return (int)stat[INDEX_MOTIF_SEQUENCE_COUNT];                
            case STDDEV:return (double)stat[INDEX_STDDEV];
            case KURTOSIS:return (double)stat[INDEX_KURTOSIS]; // according to a crash report, a NullPointerException happened at this point.
            case HISTOGRAM:return motifname;                
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


private class CellRenderer_Histogram extends DefaultTableCellRenderer { 
    private final Color color=new Color(100,100,255);
    private final Color selectedcolor=new Color(200,200,255);
    private HashMap<String,double[]> cachedhistograms=null;
    private String currentValue=null;
    private MotifPositionHistogramComputer computer=null;
    private JTable table;
    private HashSet<String> sequenceFilter=null;
    private boolean isSelected=false;
    
    public CellRenderer_Histogram(MotifLabGUI gui, JTable table) {
       super();    
       this.table=table;
       Data dataset=gui.getEngine().getDataItem(motifTrackName);
       if (dataset instanceof RegionDataset) { // note that the original dataset could have been deleted by this point
           int computeralign=MotifPositionHistogramComputer.ALIGN_TSS;
                if (anchor==USE_ANCHOR_TSS) computeralign=MotifPositionHistogramComputer.ALIGN_TSS;
           else if (anchor==USE_ANCHOR_TES) computeralign=MotifPositionHistogramComputer.ALIGN_TES;
           else if (anchor==USE_ANCHOR_UPSTREAM) computeralign=MotifPositionHistogramComputer.ALIGN_UPSTREAM;
           else if (anchor==USE_ANCHOR_DOWNSTREAM) computeralign=MotifPositionHistogramComputer.ALIGN_DOWNSTREAM;
           else if (anchor==USE_ANCHOR_CENTER) computeralign=MotifPositionHistogramComputer.ALIGN_CENTER;
           computer=new MotifPositionHistogramComputer(gui,computeralign,MotifPositionHistogramComputer.ANCHOR_SPAN,true,(RegionDataset)dataset);
       }
       if (sequenceCollectionName!=null && !sequenceCollectionName.equalsIgnoreCase(gui.getEngine().getDefaultSequenceCollectionName())) {
           Data seqCol=gui.getEngine().getDataItem(sequenceCollectionName);
           if (seqCol instanceof SequenceCollection && ((SequenceCollection)seqCol).size()==numberOfSequences) {
               sequenceFilter=new HashSet<String>(numberOfSequences);
               sequenceFilter.addAll(((SequenceCollection)seqCol).getAllSequenceNames());   
           }
       }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        this.isSelected=isSelected;
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
    
    private double[] getHistogram(String type) {
       if (cachedhistograms==null) {return null;}
       else return cachedhistograms.get(type);
    }
    
    @Override
    public void setValue(Object value) {
       currentValue=(String)value;
       setText(""); // this is to prevent superclass from rendering the value in the cell as text
    }
 
    public void setMotifAnchor(String newanchor, boolean update) {
        if (computer==null) return; // this might happen if the RegionDataset has disappeared
             if (newanchor.equalsIgnoreCase("span")) computer.setAnchor(MotifPositionHistogramComputer.ANCHOR_SPAN);
        else if (newanchor.equalsIgnoreCase("center")) computer.setAnchor(MotifPositionHistogramComputer.ANCHOR_CENTER);
        else if (newanchor.equalsIgnoreCase("upstream")) computer.setAnchor(MotifPositionHistogramComputer.ANCHOR_UPSTREAM);
        else if (newanchor.equalsIgnoreCase("downstream")) computer.setAnchor(MotifPositionHistogramComputer.ANCHOR_DOWNSTREAM);
        if (update) updateHistograms();
    }

    public void setFlatten(boolean doFlatten, boolean update) {
        if (computer==null) return; // this might happen if the RegionDataset has disappeared
        computer.setFlatten(doFlatten);
        if (update) updateHistograms();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int width=this.getWidth();
        int height=this.getHeight();
        int maxbarheight=height-4;        
        double[] histogram=getHistogram(currentValue);
        if (histogram!=null) {
            AffineTransform save=((Graphics2D)g).getTransform();
            double scaleX=(double)(width-2)/(double)histogram.length;
            ((Graphics2D)g).scale(scaleX, 1); // scale X-direction so that logo fits irrespective of size
            g.setColor((isSelected)?selectedcolor:color);
            double max=0;           
            for (int i=0;i<histogram.length;i++) if (histogram[i]>max) max=histogram[i];
            for (int i=0;i<histogram.length;i++) {
                int barheight=(int)Math.round((histogram[i]/max)*maxbarheight);
                g.fillRect(i+1, 3+maxbarheight-barheight, 1, barheight);
            }
            ((Graphics2D)g).setTransform(save);
        } else {
            g.setColor(Color.RED);
            g.drawString("Processing...",5,height-4);
        }
        g.setColor(Color.BLACK); // border
        g.drawRect(0, 0, width-1, height-1);
    }
    
    public void updateHistograms() {
        if (computer==null) return;
        if (cachedhistograms==null) cachedhistograms=new HashMap<String, double[]>();
        else cachedhistograms.clear();
        table.repaint();
        SwingWorker worker=new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {     
                //HashSet<String> sequenceFilter=null;
                String[] motifnames=((MotifTableModel)table.getModel()).getMotifNames();
                for (int i=0;i<motifnames.length;i++) {                 
                   double[] counts=new double[bins];
                   computer.countRegions(counts,motifnames[i],sequenceFilter,false); 
                   cachedhistograms.put(motifnames[i],counts);
                   if (i%20==0) table.repaint();
                }
                return null;
            }
            @Override
            protected void done() {
                table.repaint();
            }
        };
        worker.execute();
    }
    
    public void setHistograms(HashMap<String, double[]> newhistograms) {
        cachedhistograms=new HashMap<String, double[]>(newhistograms.size());
        for (String key:newhistograms.keySet()) {
            double[] values=(double[])newhistograms.get(key).clone();
            cachedhistograms.put(key, values);
        }
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
