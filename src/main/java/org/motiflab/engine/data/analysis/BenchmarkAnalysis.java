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
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.Graph;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.ToolTipHeader;
import org.motiflab.gui.VisualizationSettings;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlCursor;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceGroup;
import org.motiflab.engine.data.SequencePartition;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTErrBars;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumData;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumRef;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumVal;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.chart.STErrBarType;
import org.openxmlformats.schemas.drawingml.x2006.chart.STErrValType;

/**
 *
 * @author kjetikl
 */
public class BenchmarkAnalysis extends Analysis {
    private final static String typedescription="Analysis: Benchmark";
    private final static String analysisName="benchmark";
    private final static String description="Evaluates the performance of several region prediction tracks against a target";
    private ArrayList<String> predictionTrackNames=new ArrayList<String>(); // Note that this can contain either 'raw' tracknames or aggregated tracknames
    private ArrayList<String> groupnames=new ArrayList<String>();
    // The 'benchmark' HashMap below contains performance statistics for each method (track).
    // The key is the name of the method (or rather trackname) but if a SequenceCollection
    // or SequencePartition is provided by the 'Group' parameter the key will be a concatenation
    // of the trackname and the SequenceCollection/cluster-name
    private HashMap<String, Statistics> benchmark=new HashMap<String, Statistics>(); // key is trackname (Region Dataset) or trackname+"\t"+clustername
    private String answerTrackName=null;
    private final String[] variables = new String[]{};
    private static final Color[] defaultcolorslist=new Color[]{Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW,Color.MAGENTA,Color.CYAN,Color.LIGHT_GRAY,Color.BLACK,Color.ORANGE,Color.PINK,Color.DARK_GRAY};
    private boolean aggregate=false;
    private transient double siteOverlapFraction=0.25; // minimum overlap required to call a True Positive on the site level
    private transient HashMap<String,int[]> storage=null; // used for temporary storage of counts while processing. Key="sequencename,trackname" The results are later transferred to the "benchmark" field.
    
    public BenchmarkAnalysis() {
        this.name="BenchmarkAnalysis_temp";
        addParameter("Answer",RegionDataset.class, null,new Class[]{RegionDataset.class},"A region track containing the 'correct answer'. Each existing region track will be compared against this answer track.",true,false);
        addParameter("Groups",SequenceGroup.class, null,new Class[]{SequenceCollection.class,SequencePartition.class},"If provided, the analysis will estimate performance for the sequences in the collection or for each cluster in the partition",false,false);
        addParameter("Aggregate",Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Aggregate results from same method by calculating mean and standard deviation. To use this feature the datatracks for the same method must have names on the form 'name_number'. E.g. if you have 3 tracks named 'method1_1','method1_2' and 'method1_3' then these 3 tracks will be aggregated into one result called 'method1'.",false,false);
        addParameter("Site overlap",Double.class, new Double(0.25),new Double[]{0.0,1.0},"The minimum fraction of a target site that is required to be overlapped by a prediction in order to call the prediction TRUE at the site level",false,false);
    }
    
    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Answer"};}    
    
    /** Returns a list of output parameters that can be set when an Analysis is output */
    @Override
    public Parameter[] getOutputParameters(String dataformat) {
         Statistics temp=new Statistics();
         String[] stats=temp.getStatisticNames();
         StringBuilder string=new StringBuilder("<html>This format-string specifies which metrics to include in the output.<br>Recognized performance metrics are:<br><br>");
         for (String stat:stats) string.append(stat+"<br>");
         string.append("</html>");
         Parameter parMetrics=new Parameter("Metrics",String.class,"Sn,Sp,PPV,ASP,PC,Acc,CC,sSN,sPPV,sASP",null,string.toString(),false,false);
         Parameter parAbbr=new Parameter("Use abbreviations",Boolean.class,Boolean.TRUE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},null,false,false);         
         Parameter parX=new Parameter("X-axis",String.class,"Method",new String[]{"Method","Statistic/Groups"},null,false,false);
         Parameter parZ=new Parameter("Graph scale",Integer.class,100,new Integer[]{10,2000},"Scale of graphics plot (in percent)",false,false);
         Parameter parL=new Parameter("Legend",String.class,"",null,"<html>This argument can be used to position the legend box.<br>If used, its value should be either should be either \"normal\", \"none\", a single positive value or three values separated by comma.<br>The first numeric value will be the %-scale the legend box should be displayed at.<br>The optional second and third values are interpreted as a coordinate at which to display the legend box.<br>Positive coordinates are offset down and to the right relative to an origin in the upper left corner.<br>Negative values are offset left and upwards relative to an origin in the lower right corner.</html>",false,false);
         Parameter parC=new Parameter("Color boxes",Boolean.class,Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a box with the assigned color for the track will be output as the first column in the table",false,false);               
         
         if (dataformat.equals(HTML)) return new Parameter[]{parMetrics,parAbbr,parX,parZ,parC,parL};
         else if (dataformat.equals(EXCEL)) return new Parameter[]{}; // {parMetrics,parAbbr};
         else if (dataformat.equals(RAWDATA)) return new Parameter[]{parMetrics,parAbbr};
         else return new Parameter[0];
    }
    
//    @Override
//    public String[] getOutputParameterFilter(String parameter) {
//        if (parameter.equals("X-axis")
//         || parameter.equals("Graph scale")
//         || parameter.equals("Legend")
//         || parameter.equals("Color boxes")
//        ) return new String[]{HTML};
//        if (parameter.equals("Metrics") || parameter.equals("Use abbreviations")) return new String[]{HTML,EXCEL,RAWDATA};        
//        return null;
//    }

    @Override
    public String getAnalysisName() {
        return analysisName;
    }

    @Override
    public String getDescription() {return description;}



    @Override
    public String[] getResultVariables() {
        return variables;
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
         throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else return NumericVariable.class; // all exported values in this analysis are numerical
    }


    @Override
    @SuppressWarnings("unchecked")
    public BenchmarkAnalysis clone() {
        BenchmarkAnalysis newanalysis=new BenchmarkAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.benchmark=(HashMap<String, Statistics>)this.benchmark.clone();
        newanalysis.predictionTrackNames=(ArrayList<String>)this.predictionTrackNames.clone();
        newanalysis.groupnames=(ArrayList<String>)this.groupnames.clone();
        newanalysis.answerTrackName=this.answerTrackName;
        newanalysis.aggregate=this.aggregate;    
        newanalysis.siteOverlapFraction=this.siteOverlapFraction;
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        this.benchmark=((BenchmarkAnalysis)source).benchmark;
        this.predictionTrackNames=((BenchmarkAnalysis)source).predictionTrackNames;
        this.groupnames=((BenchmarkAnalysis)source).groupnames;
        this.answerTrackName=((BenchmarkAnalysis)source).answerTrackName;
        this.aggregate=((BenchmarkAnalysis)source).aggregate;
        this.siteOverlapFraction=((BenchmarkAnalysis)source).siteOverlapFraction;
    }

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

     
    
    @Override
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings outputSettings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        ArrayList<String> graphStatistics=new ArrayList<String>();
        String xaxis="Method";
        int scalepercent=100;
        boolean abbreviate=true;
        String legendScaleString="";
        Object legend=null;
        boolean showColorBoxes=false;
        if (outputSettings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format.getName());
             String formatString=(String)outputSettings.getResolvedParameter("Metrics",defaults,engine);
             abbreviate=(Boolean)outputSettings.getResolvedParameter("Use abbreviations",defaults,engine);
             xaxis=(String)outputSettings.getResolvedParameter("X-axis",defaults,engine);
             scalepercent=(Integer)outputSettings.getResolvedParameter("Graph scale",defaults,engine);
             String[] metrics=formatString.trim().split("\\s*,\\s*");
             for (String metric:metrics) {                
                 if (isLongNameForStatistic(metric)) {
                    graphStatistics.add(metric);
                 } if (isShortNameForStatistic(metric)) {
                    graphStatistics.add(getLongNameForStatistic(metric)); 
                 }
             }
             legendScaleString=(String)outputSettings.getResolvedParameter("Legend",defaults,engine);                 
             showColorBoxes=(Boolean)outputSettings.getResolvedParameter("Color boxes",defaults,engine);                  
          } 
          catch (ExecutionError e) {throw e;} 
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        if (legendScaleString==null || legendScaleString.trim().isEmpty()) legendScaleString="normal"; else legendScaleString=legendScaleString.trim();
        if (legendScaleString.equalsIgnoreCase("none")) legend=null;
        else if (legendScaleString.equalsIgnoreCase("normal")) legend=legendScaleString;
        else {
            String[] parts=legendScaleString.split("\\s*,\\s*");
            if (!(parts.length==1 || parts.length==3)) throw new ExecutionError("The \"Legend\" parameter should be either \"normal\" or \"none\" or consist of either 1 single value or 3 comma-separated values");
            double[] includeLegend=new double[3];
            for (int i=0;i<parts.length;i++) {
                try {
                    includeLegend[i]=Double.parseDouble(parts[i]);
                } catch (Exception e) {throw new ExecutionError("Unable to parse expected numerical value for \"Legend\" parameter: "+parts[i]);}
            }
            includeLegend[0]=includeLegend[0]/100.0; // this was a percentage number
            legend=includeLegend;
        }        
        double scale=(scalepercent==100)?1.0:(((double)scalepercent)/100.0);
        engine.createHTMLheader("Benchmark Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<h1 class=\"headline\">Benchmark evaluation against \""+answerTrackName+"\"</h1><br>",HTML);
        formatSummaryTablesWithGraphs(outputobject,engine, graphStatistics, xaxis, scale, abbreviate, legend, showColorBoxes);
        outputobject.append("</body>\n</html>\n",HTML);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }



  /**
   * Creates a graph based on the current data and saves it to file (if file is not null)
   * @param file
   * @param engine
   * @param settings
   * @param statistic
   * @param groupname
   * @param groupbymethod
   * @return
   * @throws IOException
   */
    @SuppressWarnings("unchecked")
    private BufferedImage createGraphImage(File file, MotifLabEngine engine, ArrayList<String> statistics, String groupname, boolean groupbymethod, boolean abbreviate, double scale, Object legend) throws IOException {
        int graphheight=250; // height of graph in pixels (just the histogram);
        int translateX=50; // the X coordinate for the top of the graph
        int translateY=30; // the Y coordinate for the top of the graph
        int barwidth=7;
        int bardistance=9; // the distance from start of one bar to start of next (i.e. interdistance = bardistance-barwidth)
        int bargroupspacing=28;        
        int bargroupwidth=0; // to be calculated below
        int barshadow=0;
        boolean gradientfill=false;
        boolean drawbox=false;
        boolean drawGridX=true;
        boolean drawGridY=true;        
        String statistic=null; // name of statistic if only a single one is selected
        if (statistics.size()==1) statistic=statistics.get(0);
        if (groupname!=null && groupname.isEmpty()) groupname=null;
        VisualizationSettings settings=engine.getClient().getVisualizationSettings();
        Object barwidthSetting=settings.getSetting("barchart.barwidth");
        Object bardistancedSetting=settings.getSetting("barchart.bardistance");
        Object bargroupspacingSetting=settings.getSetting("barchart.bargroupspacing");
        Object gradientfillSetting=settings.getSetting("barchart.gradientfill");
        Object shadowSetting=settings.getSetting("barchart.shadow");
        Object borderSetting=settings.getSetting("barchart.border");
        Object boxSetting=settings.getSetting("barchart.box");
        Object gridXSetting=settings.getSetting("barchart.gridX");
        Object gridYSetting=settings.getSetting("barchart.gridY");
        if (barwidthSetting instanceof Integer) barwidth=Math.max(1, (Integer)barwidthSetting);
        if (bardistancedSetting instanceof Integer) bardistance=Math.max(0, (Integer)bardistancedSetting);
        if (bargroupspacingSetting instanceof Integer) bargroupspacing=Math.max(0, (Integer)bargroupspacingSetting);
        if (gradientfillSetting instanceof Boolean) gradientfill=(Boolean)gradientfillSetting;
        if (boxSetting instanceof Boolean) drawbox=(Boolean)boxSetting;
        if (gridXSetting instanceof Boolean) drawGridX=(Boolean)gridXSetting;
        if (gridYSetting instanceof Boolean) drawGridY=(Boolean)gridYSetting;
        if (shadowSetting instanceof Integer) barshadow=Math.max(0, (Integer)shadowSetting);
            
        ArrayList<String> methodnames=settings.sortFeaturesAccordingToOrderInMasterList(predictionTrackNames, true, aggregate);
        // determine which names will correspond to ticks along the X-axis and which will correspond to differently colored bars
        ArrayList<String> xAxisElements; // names corresponding to ticks on the X-axis
        ArrayList<String> barElements; // names corresponding to differently colored bars
        if (groupbymethod) { // group results by method along X-axis
            xAxisElements=methodnames;
            barElements=new ArrayList<String>();
            if (statistic!=null) { // show single statistic with bars=groups
               if (groupname!=null) barElements.add(groupname); // show results for single group
               else {
                   if (groupnames.isEmpty() || groupnames.size()>1) barElements.add(""); // The empty name reserved a slot for 'all' groups combined
                   barElements.addAll(groupnames);
               }
            } else { // show all statistics with bars=statistics
               for (String s:statistics) {
                   barElements.add(abbreviate?getShortNameForStatistic(s):s);
               }
            }
        } else { // group results by statistic along X-axis (or by sequence-groups for single statistic)
            barElements=methodnames;
            xAxisElements=new ArrayList<String>();
            if (statistic!=null) { // show single statistic with bars=groups and xAxis=sequence groups
               if (groupname!=null) xAxisElements.add(groupname); // show results for single group
               else {
                   if (groupnames.isEmpty() || groupnames.size()>1) xAxisElements.add(""); // The empty name reserved a slot for 'all' groups combined
                   xAxisElements.addAll(groupnames);
               }
            } else { // show all statistics with bars=methods and xAxis=statistic
               for (String s:statistics) {
                   xAxisElements.add(abbreviate?getShortNameForStatistic(s):s);
               }
            }
        } // -- END: determine which names will correspond to ticks along the X-axis and which will correspond to differently colored bars
        
        // determine number of groups along the X-axis and number of bars in each group
        // and value-range of graph as well as physical size
        int numbargroups=xAxisElements.size();
        double minvalue=getMinUsedValue(statistic, statistics, groupname, groupbymethod, methodnames, xAxisElements,  barElements); // get a minimum value to use (other than just using -1.0 for CC)
        double maxvalue=getMaxUsedValue(statistic, statistics, groupname, groupbymethod, methodnames, xAxisElements,  barElements); // get a minimum value to use (other than just using -1.0 for CC)
        if (minvalue>0) minvalue=0;
        if (maxvalue<=1) maxvalue=1.05; // 1.05 is just to give a little room above 1.0 but not enough for another tick
        int numbars=barElements.size(); // number of bars in each group
        String[] barElementsToArray=new String[barElements.size()];
        barElementsToArray=barElements.toArray(barElementsToArray);
        Dimension legendDimension=Graph.getLegendDimension(barElementsToArray,null);
        int legendwidth=legendDimension.width;
        if (legendwidth<50) legendwidth=50; // this is only used to set image-width not to draw the actual legend box
        int legendheight=legendDimension.height;
        bargroupwidth=(numbars-1)*bardistance+barwidth;
        double legendScale=1.0;
        if (legend instanceof double[]) legendScale=((double[])legend)[0];
        legendwidth=(int)Math.round(legendwidth*legendScale);
        legendheight=(int)Math.round(legendheight*legendScale);               
        int graphwidth=numbargroups*bargroupwidth+(numbargroups)*bargroupspacing; // this includes spacing on both sides of end groups!
        int width=translateX+graphwidth+((legend instanceof String)?(50+legendwidth):16); //
        int largestNameSize=Graph.findLongestString(xAxisElements, null);
        int height=translateY+graphheight+largestNameSize+30;
        if (height<translateY+legendheight) height=(translateY+legendheight+30);

        //BufferedImage image=new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
        BufferedImage image=new BufferedImage((int)Math.round(width*scale),(int)Math.round(height*scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.scale(scale, scale);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width+10, height+10); // I fill a little wider just in case or rounding errors when scaling
        g.setColor(Color.BLACK);

        // create graph
        Graph graph=new Graph(g, 0, 1, minvalue, maxvalue, graphwidth, graphheight, translateX, translateY);
        //g.setColor(Color.lightGray);
        g.setColor(Color.BLACK);
        //graph.drawYaxisWithTicks(graph.getXforValue(0), false, false);
        graph.drawAxes(Graph.BOX, (drawGridX)?Graph.DOTTED:Graph.NONE, Graph.NONE, false,false,true,false,false);
        if (minvalue<0 && drawGridX) {
            graph.drawHorizontalGridLine(graph.getYforValue(0), Graph.SOLID, Color.lightGray); // the Color.lightGray here is darker than Graph.lightGray
            graph.drawAxes(Graph.BOX, Graph.NONE, Graph.NONE, false,false,true,false,false); // redraw axes above
        } 
        int xpos=translateX+(int)(bargroupspacing/2.0);
        int tickpos=graph.getYforValue(minvalue)+1;
        int legendXpos=translateX+graphwidth+30;
        int legendYpos=translateY;
        if (legend instanceof double[]) {            
            double offsetX=((double[])legend)[1];
            double offsetY=((double[])legend)[2];
            legendwidth=(int)Math.round(legendwidth*legendScale);
            legendheight=(int)Math.round(legendheight*legendScale);
            legendXpos=(offsetX>=0)?(int)(graph.getXforValue(0)+offsetX):(int)(graph.getXforValue(1.0)+offsetX-legendwidth);
            legendYpos=(offsetY>=0)?(int)(graph.getYforValue(maxvalue)+offsetY):(int)(graph.getYforValue(minvalue)+offsetY-legendheight);  
        }       
        if (groupbymethod) {  // group results by method along X-axis
            if (statistic==null) { // show all statistics
               Color[] colorslist=getSystemColorsForStatistics(statistics,settings);
               for (int i=0;i<xAxisElements.size();i++) {
                   String methodname=xAxisElements.get(i);
                   String key=methodname;
                   if (groupname!=null && !groupname.isEmpty()) key+="\t"+groupname;
                   Statistics results=benchmark.get(key);
                   double[] values=results.getStatisticsAsArray(statistics);
                   double[] stdevs=results.getStatisticsStandardDeviationsAsArray(statistics);
                   if (drawGridY && i<xAxisElements.size()-1) graph.drawVerticalGridLine(xpos+bargroupwidth+(int)((float)bargroupspacing/2.0), Graph.DOTTED, Graph.lightGray); // draw grid line first so that it is behind                   
                   graph.drawBars(values, stdevs, colorslist, getBarBorderColors(colorslist,borderSetting), barwidth, bardistance, xpos, gradientfill,barshadow);
                   graph.drawVerticalStringXTick(methodname, xpos+(int)((float)bargroupwidth/2.0)-1,tickpos,true);
                   xpos+=(bargroupspacing+bargroupwidth);
               }
               if (legend!=null) {
                   String[] barElementsArray=new String[barElements.size()];
                   AffineTransform current=g.getTransform();
                   g.translate(legendXpos, legendYpos);         
                   g.scale(legendScale, legendScale);
                   graph.drawLegendBox(barElements.toArray(barElementsArray), colorslist, 0, 0, true);
                   g.setTransform(current);
               }
            } else { // show specific statistic
               String[] groupnamesarray=new String[barElements.size()];
               if (xAxisElements.isEmpty()) groupnamesarray=barElements.toArray(groupnamesarray);
               for (int i=0;i<xAxisElements.size();i++) {
                   String methodname=xAxisElements.get(i);
                   double[] values=new double[barElements.size()];
                   double[] stdev=new double[barElements.size()];
                   for (int j=0;j<barElements.size();j++) {
                       String thisgroup=barElements.get(j);
                       String key=methodname;
                       if (thisgroup.equals("")) {
                          groupnamesarray[j]="All";
                       } else {
                           key=key+"\t"+thisgroup;
                           groupnamesarray[j]=thisgroup;
                       }
                       Statistics results=benchmark.get(key);
                       values[j]=results.getStatisticValue(statistic);
                       stdev[j]=results.getStatisticStandardDeviation(statistic);
                   }
                   if (drawGridY && i<xAxisElements.size()-1) graph.drawVerticalGridLine(xpos+bargroupwidth+(int)((float)bargroupspacing/2.0), Graph.DOTTED, Graph.lightGray);                   
                   graph.drawBars(values, stdev, defaultcolorslist, getBarBorderColors(defaultcolorslist,borderSetting),barwidth, bardistance, xpos,gradientfill,barshadow);
                   graph.drawVerticalStringXTick(methodname, xpos+(int)((float)bargroupwidth/2.0)-1,tickpos,true);
                   xpos+=(bargroupspacing+bargroupwidth);
               }
               if (legend!=null) {
                   AffineTransform current=g.getTransform();                   
                   g.translate(legendXpos, legendYpos);         
                   g.scale(legendScale, legendScale);                   
                   graph.drawLegendBox(groupnamesarray, defaultcolorslist, 0, 0, true);
                   g.setTransform(current);                   
               }
           }
        } else { // group results by statistic along X-axis (or by sequence-groups for single statistic)
            Color[] methodcolors=null;
            if (settings!=null) { // if in GUI use the selected track-colors for the methods
                methodcolors=new Color[methodnames.size()];
                for (int m=0;m<methodcolors.length;m++) methodcolors[m]=settings.getForeGroundColor(methodnames.get(m));
            }
            if (statistic==null) { // show all statistics
               for (int i=0;i<xAxisElements.size();i++) {
                   String statisticname=xAxisElements.get(i);
                   double[] values=new double[methodnames.size()];
                   double[] stdev=new double[methodnames.size()];
                   for (int j=0;j<methodnames.size();j++) {
                     String key=methodnames.get(j);
                     if (groupname!=null && !groupname.isEmpty()) key+="\t"+groupname;
                     Statistics results=benchmark.get(key);
                     values[j]=results.getStatisticValue(statisticname);
                     stdev[j]=results.getStatisticStandardDeviation(statisticname);
                   }
                   if (drawGridY && i<xAxisElements.size()-1) graph.drawVerticalGridLine(xpos+bargroupwidth+(int)((float)bargroupspacing/2.0), Graph.DOTTED, Graph.lightGray);                                     
                   Color[] colors=(methodcolors!=null)?methodcolors:defaultcolorslist;
                   graph.drawBars(values, stdev, colors, getBarBorderColors(colors,borderSetting), barwidth, bardistance, xpos,gradientfill,barshadow);
                   if (isShortNameForStatistic(statisticname)) graph.drawHorizontalStringXTick(statisticname, xpos+(int)((float)bargroupwidth/2.0),tickpos);
                   else graph.drawVerticalStringXTick(statisticname, xpos+(int)((float)bargroupwidth/2.0)-1, tickpos, true);
                   xpos+=(bargroupspacing+bargroupwidth);
               }
               if (legend!=null) {
                   AffineTransform current=g.getTransform();                   
                   g.translate(legendXpos, legendYpos);         
                   g.scale(legendScale, legendScale);                   
                   String[] barElementsArray=new String[barElements.size()]; 
                   graph.drawLegendBox(barElements.toArray(barElementsArray), (methodcolors!=null)?methodcolors:defaultcolorslist, 0, 0, true);
                   g.setTransform(current);
               }
            } else { // show specific statistic
               for (int i=0;i<xAxisElements.size();i++) {
                   String thisgroup=xAxisElements.get(i);
                   double[] values=new double[methodnames.size()];
                   double[] stdev=new double[methodnames.size()];
                   String tickString=(thisgroup.equals(""))?"All":thisgroup;
                   for (int j=0;j<methodnames.size();j++) {
                       String methodname=methodnames.get(j);
                       String key=methodname;
                       if (!thisgroup.equals("")) {
                           key=key+"\t"+thisgroup;
                       }
                       Statistics results=benchmark.get(key);
                       values[j]=results.getStatisticValue(statistic);
                       stdev[j]=results.getStatisticStandardDeviation(statistic);
                   }
                   if (drawGridY && i<xAxisElements.size()-1) graph.drawVerticalGridLine(xpos+bargroupwidth+(int)((float)bargroupspacing/2.0), Graph.DOTTED, Graph.lightGray);                                      
                   Color[] colors=(methodcolors!=null)?methodcolors:defaultcolorslist;
                   graph.drawBars(values, stdev,colors,getBarBorderColors(colors,borderSetting), barwidth, bardistance, xpos, gradientfill,barshadow);
                   graph.drawVerticalStringXTick(tickString, xpos+(int)((float)bargroupwidth/2.0)-1,tickpos,true);
                   xpos+=(bargroupspacing+bargroupwidth);
               }
               if (legend!=null) {
                   AffineTransform current=g.getTransform();                   
                   g.translate(legendXpos, legendYpos);         
                   g.scale(legendScale, legendScale);                   
                   String[] barElementsArray=new String[barElements.size()];
                   graph.drawLegendBox(barElements.toArray(barElementsArray), (methodcolors!=null)?methodcolors:defaultcolorslist, 0, 0, true);
                   g.setTransform(current);
               }
            }               
        }


        // draw X-axis anew just to update the line
        g.setColor(Color.BLACK);
        int ypos=graph.getYforValue(minvalue);
        g.drawLine(translateX, ypos, translateX+graphwidth, ypos);
        if (drawbox) graph.drawBoundingBox();        
        // write the image to file
        if (file!=null) {
            OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
            ImageIO.write(image, "png", output);
            output.close(); 
        }
        g.dispose();
        return image;
    }

    private double getMinUsedValue(String statistic, ArrayList<String>statistics, String groupname, boolean groupbymethod, ArrayList<String> methodnames, ArrayList<String> xAxisElements, ArrayList<String> barElements) {
        double min=Double.MAX_VALUE;
        if (groupbymethod) {  // group results by method along X-axis
            if (statistic==null) { // show all statistics
               for (int i=0;i<xAxisElements.size();i++) {
                   String methodname=xAxisElements.get(i);
                   String key=methodname;
                   if (groupname!=null && !groupname.isEmpty()) key+="\t"+groupname;
                   Statistics results=benchmark.get(key);
                   double[] values=results.getStatisticsAsArray(statistics);
                   double[] stddev=results.getStatisticsStandardDeviationsAsArray(statistics);
                   for (int j=0;j<values.length;j++) values[j]-=stddev[j];
                   double valmin=getMinValue(values);
                   if (valmin<min) min=valmin;
               }
            } else { // show specific statistic
               for (int i=0;i<xAxisElements.size();i++) {
                   String methodname=xAxisElements.get(i);
                   double[] values=new double[barElements.size()];
                   for (int j=0;j<barElements.size();j++) {
                       String thisgroup=barElements.get(j);
                       String key=methodname;
                       if (!thisgroup.equals("")) key=key+"\t"+thisgroup;
                       Statistics results=benchmark.get(key);
                       values[j]=results.getStatisticValue(statistic);
                       values[j]-=results.getStatisticStandardDeviation(statistic);
                   }
                   double valmin=getMinValue(values);
                   if (valmin<min) min=valmin;
               }
            }
        } else { // group results by statistic along X-axis (or by sequence-groups for single statistic)
            if (statistic==null) { // show all statistics
               for (int i=0;i<xAxisElements.size();i++) {
                   String statisticname=xAxisElements.get(i);
                   double[] values=new double[methodnames.size()];
                   for (int j=0;j<methodnames.size();j++) {
                     String key=methodnames.get(j);
                     if (groupname!=null && !groupname.isEmpty()) key+="\t"+groupname;
                     Statistics results=benchmark.get(key);
                     values[j]=results.getStatisticValue(statisticname);
                     values[j]-=results.getStatisticStandardDeviation(statisticname);                     
                   }
                   double valmin=getMinValue(values);
                   if (valmin<min) min=valmin;
               }
            } else { // show specific statistic
               for (int i=0;i<xAxisElements.size();i++) {
                   String thisgroup=xAxisElements.get(i);
                   double[] values=new double[methodnames.size()];
                   for (int j=0;j<methodnames.size();j++) {
                       String methodname=methodnames.get(j);
                       String key=methodname;
                       if (!thisgroup.equals("")) key=key+"\t"+thisgroup;
                       Statistics results=benchmark.get(key);
                       values[j]=results.getStatisticValue(statistic);
                       values[j]-=results.getStatisticStandardDeviation(statistic);
                   }
                   double valmin=getMinValue(values);
                   if (valmin<min) min=valmin;
               }
            }
        }
        if (min>=0) return 0;
        else { // extend value slightly below the min
            double d=-0.1;
            while (min<=d) {
                d-=0.1;
            }
            return d-0.05; // the extra 0.05 is to avoid a tick on the baseline
        }
    }

    private double getMaxUsedValue(String statistic, ArrayList<String>statistics, String groupname, boolean groupbymethod, ArrayList<String> methodnames, ArrayList<String> xAxisElements, ArrayList<String> barElements) {
        double max=-Double.MAX_VALUE;
        if (groupbymethod) {  // group results by method along X-axis
            if (statistic==null) { // show all statistics
               for (int i=0;i<xAxisElements.size();i++) {
                   String methodname=xAxisElements.get(i);
                   String key=methodname;
                   if (groupname!=null && !groupname.isEmpty()) key+="\t"+groupname;
                   Statistics results=benchmark.get(key);
                   double[] values=results.getStatisticsAsArray(statistics);
                   double[] stddev=results.getStatisticsStandardDeviationsAsArray(statistics);
                   for (int j=0;j<values.length;j++) values[j]+=stddev[j];
                   double valmax=getMaxValue(values);
                   if (valmax>max) max=valmax;
               }
            } else { // show specific statistic
               for (int i=0;i<xAxisElements.size();i++) {
                   String methodname=xAxisElements.get(i);
                   double[] values=new double[barElements.size()];
                   for (int j=0;j<barElements.size();j++) {
                       String thisgroup=barElements.get(j);
                       String key=methodname;
                       if (!thisgroup.equals("")) key=key+"\t"+thisgroup;
                       Statistics results=benchmark.get(key);
                       values[j]=results.getStatisticValue(statistic);
                       values[j]+=results.getStatisticStandardDeviation(statistic);
                   }
                   double valmax=getMaxValue(values);
                   if (valmax>max) max=valmax;
               }
            }
        } else { // group results by statistic along X-axis (or by sequence-groups for single statistic)
            if (statistic==null) { // show all statistics
               for (int i=0;i<xAxisElements.size();i++) {
                   String statisticname=xAxisElements.get(i);
                   double[] values=new double[methodnames.size()];
                   for (int j=0;j<methodnames.size();j++) {
                     String key=methodnames.get(j);
                     if (groupname!=null && !groupname.isEmpty()) key+="\t"+groupname;
                     Statistics results=benchmark.get(key);
                     values[j]=results.getStatisticValue(statisticname);
                     values[j]+=results.getStatisticStandardDeviation(statisticname);                     
                   }
                   double valmax=getMaxValue(values);
                   if (valmax>max) max=valmax;
               }
            } else { // show specific statistic
               for (int i=0;i<xAxisElements.size();i++) {
                   String thisgroup=xAxisElements.get(i);
                   double[] values=new double[methodnames.size()];
                   for (int j=0;j<methodnames.size();j++) {
                       String methodname=methodnames.get(j);
                       String key=methodname;
                       if (!thisgroup.equals("")) key=key+"\t"+thisgroup;
                       Statistics results=benchmark.get(key);
                       values[j]=results.getStatisticValue(statistic);
                       values[j]+=results.getStatisticStandardDeviation(statistic);
                   }
                   double valmax=getMaxValue(values);
                   if (valmax>max) max=valmax;
               }
            }
        }
        if (max<0) return 0;
        else { // extend value slightly above the max
            double d=0.1;
            while (max>=d) {
                d+=0.1;
            }
            return d+0.05; // the extra 0.05 is to avoid a tick on the baseline
        }
    }
    
    private double getMinValue(double[] list) {
        double min=Double.MAX_VALUE;
        for (int i=0;i<list.length;i++) if (!Double.isNaN(list[i]) && list[i]<min) min=list[i];
        if (min==Double.MAX_VALUE) return 0;
        else return min;
    }
    
    private double getMaxValue(double[] list) {
        double max=-Double.MAX_VALUE;
        for (int i=0;i<list.length;i++) if (!Double.isNaN(list[i]) && list[i]>max) max=list[i];
        if (max==-Double.MAX_VALUE) return 0;
        else return max;
    }    

    private Color[] getBarBorderColors(Color[] list, Object setting) {
        if (setting==null || (setting instanceof Boolean && !(Boolean)setting) || setting.toString().equalsIgnoreCase("none")) return null;
        //if (setting instanceof Boolean) return new Color[]{Color.BLACK};
        else if (setting instanceof Color) return new Color[]{(Color)setting};
        else {
            Color[] borders=new Color[list.length];
            for (int i=0;i<list.length;i++) borders[i]=list[i].darker();
            return borders;
        }
    }
    
    @SuppressWarnings("unchecked")
    private void formatSummaryTablesWithGraphs(OutputData outputobject, MotifLabEngine engine, ArrayList<String> graphStatistics, String xaxis, double scale, boolean abbreviate, Object legend, boolean showColorBoxes) {       
        String statisticHeader="Statistics";
        if (graphStatistics.size()==1) statisticHeader=graphStatistics.get(0);
        if (groupnames.isEmpty() || groupnames.size()>1) { // output 'all' then each group in turn (if any)
           File imagefile=outputobject.createDependentFile(engine,"png");
           try {createGraphImage(imagefile,engine, graphStatistics, null, xaxis.equalsIgnoreCase("Method"), abbreviate, scale, legend);
           } catch (IOException e) {engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);}
           outputobject.append("<h2 class=\"groupheader\">"+statisticHeader+"</h2>",HTML);
           outputobject.append("<img src=\"file:///"+imagefile.getAbsolutePath()+"\" /><br><br>",HTML);
           formatSummaryTable(outputobject, engine, null, graphStatistics, showColorBoxes);
           
           for (String groupname:groupnames) { // output each group in turn
               outputobject.append("<br><hr><br>",HTML);
               outputobject.append("<h2 class=\"groupheader\">Group: "+groupname+"</h2>",HTML);
               File imagefile2=outputobject.createDependentFile(engine,"png");
               try {createGraphImage(imagefile2,engine, graphStatistics, groupname, xaxis.equalsIgnoreCase("Method"), abbreviate, scale, legend);
               } catch (IOException e) {engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);}
               outputobject.append("<h2>"+statisticHeader+"</h2>",HTML);
               outputobject.append("<img src=\"file:///"+imagefile2.getAbsolutePath()+"\" /><br><br>",HTML);
               formatSummaryTable(outputobject, engine, groupname, graphStatistics, showColorBoxes);
           }
        } else { // single group
            outputobject.append("<h2 class=\"groupheader\">Group: "+groupnames.get(0)+"</h2>",HTML);
            File imagefile=outputobject.createDependentFile(engine,"png");
            try {createGraphImage(imagefile,engine, graphStatistics, groupnames.get(0), xaxis.equalsIgnoreCase("Method"), abbreviate, scale, legend);
            } catch (IOException e) {engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);}
            outputobject.append("<h2>"+statisticHeader+"</h2>",HTML);
            outputobject.append("<img src=\"file:///"+imagefile.getAbsolutePath()+"\" /><br><br>",HTML);
            formatSummaryTable(outputobject, engine, groupnames.get(0), graphStatistics, showColorBoxes);
        }
    }

    /** Formats summary table for a single group in HTML format */
    @SuppressWarnings("unchecked")
    private void formatSummaryTable(OutputData outputobject, MotifLabEngine engine, String groupname, ArrayList<String> statistics, boolean showColorBoxes) {
        VisualizationSettings settings=engine.getClient().getVisualizationSettings();
        ArrayList<String> methodnames=settings.sortFeaturesAccordingToOrderInMasterList(predictionTrackNames, true, aggregate);
        
        DecimalFormat formatter=new DecimalFormat("#.###");

        //if (groupname!=null) outputobject.append("<h2>Group: "+groupname+"</h2>",HTML);
        outputobject.append("<table class=\"sortable\">\n<tr>",HTML);
        if (showColorBoxes) outputobject.append("<th>&nbsp;</th>",HTML);
        outputobject.append("<th>Track</th>",HTML);
        for (String metric:statistics) {
            String metricname=metric;          
            if (isLongNameForStatistic(metricname)) metricname=getShortNameForStatistic(metricname); 
            outputobject.append("<th class=\"sorttable_numeric\">"+metricname+"</th>",HTML);
         }
        outputobject.append("</tr>\n",HTML);

        for (String trackname:methodnames) {
            if (!settings.isTrackVisible(trackname)) continue; // track is hidden
            String key=(groupname==null)?trackname:(trackname+"\t"+groupname);
            Statistics stat=benchmark.get(key);
            outputobject.append("<tr>",HTML);            
            if (showColorBoxes) {
                Color color=settings.getForeGroundColor(trackname);
                String colorString=VisualizationSettings.convertColorToHTMLrepresentation(color);
                outputobject.append("<td><div style=\"width:10px;height:10px;border:1px solid #000;background-color:"+colorString+";\"></div></td>",HTML);
            }            
            outputobject.append("<td class=\"namecolumn\">"+trackname+"</td>",HTML);
            for (String metric:statistics) {
                Double value=stat.getStatisticValue(metric);                
                outputobject.append("<td class=\"num\">"+((Double.isNaN(value))?"-":formatter.format(value))+"</td>",HTML);
             }
            outputobject.append("</tr>\n",HTML);
         }
         outputobject.append("</table>\n<br>\n",HTML);
    }



    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings outputSettings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        ArrayList<String> graphStatistics=new ArrayList<String>();
        boolean abbreviate=true;
        if (outputSettings!=null) {
            try {
                Parameter[] defaults=getOutputParameters(format.getName());
                String formatString=(String)outputSettings.getResolvedParameter("Metrics",defaults,engine);
                abbreviate=(Boolean)outputSettings.getResolvedParameter("Use abbreviations",defaults,engine);
                String[] metrics=formatString.trim().split("\\s*,\\s*");
                for (String metric:metrics) {                
                   if (isLongNameForStatistic(metric)) {
                       graphStatistics.add(metric);
                   } if (isShortNameForStatistic(metric)) {
                       graphStatistics.add(getLongNameForStatistic(metric)); 
                   }
               }          
            } 
            catch (ExecutionError e) {throw e;} 
            catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }       
              
        outputobject.append("#Benchmark evaluation against '"+answerTrackName+"'\n",RAWDATA);
        if (groupnames.isEmpty() || groupnames.size()>1) { // not a single group: output combined statistics for 'all' sequences before processing each group
            outputobject.append("#Track",RAWDATA);
            for (String metric:graphStatistics) {
                String metricname=metric;
                if (abbreviate && isLongNameForStatistic(metricname)) metricname=getShortNameForStatistic(metricname);
                else if (!abbreviate && isShortNameForStatistic(metricname)) metricname=getLongNameForStatistic(metricname);
                outputobject.append("\t"+metricname,RAWDATA);
            }
            outputobject.append("\n",RAWDATA);                      
            
            for (String trackname:predictionTrackNames) {
                if (!vizSettings.isTrackVisible(trackname)) continue; // track is hidden                
                Statistics stat=benchmark.get(trackname);
                outputobject.append(trackname,RAWDATA);
                for (String metric:graphStatistics) {     
                    Double value=stat.getStatisticValue(metric);                    
                    outputobject.append((Double.isNaN(value))?"\t-":"\t"+value,RAWDATA);
                 }
                outputobject.append("\n",RAWDATA);
            }
        }
        if (!groupnames.isEmpty()) { // if individual groups have been specified, output each in turn now.
           for (String groupname:groupnames) {
             outputobject.append("\n#Group:"+groupname+"\n",RAWDATA);
             outputobject.append("#Track",RAWDATA);
             for (String metric:graphStatistics) {                
                String metricname=metric;
                if (abbreviate && isLongNameForStatistic(metricname)) metricname=getShortNameForStatistic(metricname);
                else if (!abbreviate && isShortNameForStatistic(metricname)) metricname=getLongNameForStatistic(metricname);
                outputobject.append("\t"+metricname,RAWDATA);
              }
             outputobject.append("\n",RAWDATA);               
             
             for (String trackname:predictionTrackNames) {
                if (!vizSettings.isTrackVisible(trackname)) continue; // track is hidden
                Statistics stat=benchmark.get(trackname+"\t"+groupname);
                outputobject.append(trackname,RAWDATA);
                for (String metric:graphStatistics) {     
                    Double value=stat.getStatisticValue(metric);
                    outputobject.append((Double.isNaN(value))?"\t-":"\t"+value,RAWDATA);
                }
                outputobject.append("\n",RAWDATA);
              }
           }
        }
        if (format!=null) format.setProgress(100);
        return outputobject;
    }
   
    @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings outputSettings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        XSSFWorkbook workbook=null;
        try {
            InputStream stream = DistributionAnalysis.class.getResourceAsStream("resources/AnalysisTemplate_Benchmark.xlsx");
            workbook = (XSSFWorkbook)WorkbookFactory.create(stream);
            stream.close();
            if (format!=null) format.setProgress(20);    
                               
            // setup the statistics
            ArrayList<String> methodnames=engine.getClient().getVisualizationSettings().sortFeaturesAccordingToOrderInMasterList(predictionTrackNames, true, aggregate);           
            String[] metrics=new String[]{"Sn","Sp","PPV","NPV","PC","ASP","F","Acc","CC","sSn","sPPV","sASP","sPC","sF"}; // this is the order used in the template
            CellStyle cellStyle = createExcelStyle(workbook, BorderStyle.THIN, new Color(255,255,210), HorizontalAlignment.RIGHT, false);
            cellStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("0.000"));
            CellStyle methodStyle = createExcelStyle(workbook, BorderStyle.THIN, new Color(220,220,255), HorizontalAlignment.LEFT, false);
                                          
            if (groupnames.isEmpty() || groupnames.size()>1) { // output 'all' first then each group in turn (if any)             
                int i=0;
                XSSFSheet sheet;
                
                for (String groupname:groupnames) { // Add an extra sheet for each cluster
                    if (format!=null) format.setProgress((int)(20+60*(i/groupnames.size()))); // not right...
                    sheet = workbook.cloneSheet(0);
                    workbook.setSheetName(workbook.getSheetIndex(sheet), groupname); // name the sheed after the cluster
                    outputSingleExcelSheet(sheet, groupname, methodnames, metrics, cellStyle, methodStyle);
                    i++;
                }
                // I output the first sheet last so that it is fresh when it is being cloned above and does not have anything on it that could cause problems (like error bars)              
                sheet = workbook.getSheetAt(0);
                workbook.setSheetName(0, "All"); // rename first sheet to All                 
                outputSingleExcelSheet(sheet, null, methodnames, metrics, cellStyle, methodStyle);                 
                 
            } else { // single named group (i.e. analysis limited to a selected Sequence Collection)
                if (format!=null) format.setProgress(40);                
                XSSFSheet sheet = workbook.getSheetAt(0);
                workbook.setSheetName(0, groupnames.get(0)); // name the sheed after the Sequence Collection 
                outputSingleExcelSheet(sheet, groupnames.get(0), methodnames, metrics, cellStyle, methodStyle);
            }            
                        
        } catch (Exception e) {
            // e.printStackTrace();
            throw new ExecutionError(e.getMessage(),e);
        }
        if (format!=null) format.setProgress(80);  
        // now write to the outputobject. The binary Excel file is included as a dependency in the otherwise empty OutputData object.
        File excelFile=outputobject.createDependentBinaryFile(engine,"xlsx");      
        try {
            BufferedOutputStream stream=new BufferedOutputStream(new FileOutputStream(excelFile));
            workbook.write(stream);
            stream.close();
        } catch (Exception e) {
            throw new ExecutionError("An error occurred when creating the Excel file: "+e.toString(),0, e);
        }
        outputobject.setBinary(true);        
        outputobject.setDirty(true); // this is not set automatically since I don't append to the document
        outputobject.setDataFormat(EXCEL); // this is not set automatically since I don't append to the document
        if (format!=null) format.setProgress(100);  
        return outputobject;        
    }       
    
    private void outputSingleExcelSheet(XSSFSheet sheet, String groupname, ArrayList<String> methodnames, String[] metrics, CellStyle cellStyle, CellStyle headerstyle) { 
        VisualizationSettings vizSettings = MotifLabEngine.getEngine().getClient().getVisualizationSettings();   
        String sheetname = sheet.getSheetName();
        sheet.setForceFormulaRecalculation(true);           
        String firstLine="Benchmark analysis against \""+answerTrackName+"\"";       
        Row row=sheet.getRow(0);
        if (row==null) row=sheet.createRow(0);          
        outputStringValueInCell(row, 0, firstLine, null); 

        int headerRow = 2; // Row for header (above first data line). This is dictated by the Excel template file
        XSSFDrawing drawing = ((XSSFSheet)sheet).createDrawingPatriarch();
        XSSFChart barChart = drawing.getCharts().get(0);
        XDDFChartData chartData = barChart.getChartSeries().get(0);
        
        // Add tracks to statistics table. (Note that the names can be aggregated)
        int rowIndex = headerRow;   
        int errorBarColumnsStart = metrics.length+10;
        if (aggregate) {           
            String[] metricsSTDlabels = new String[metrics.length];
            for (int i=0;i<metrics.length;i++) {
                metricsSTDlabels[i]=metrics[i]+"-StDev";
            }            
            Row stdHeaderRow = sheet.getRow(headerRow);
            outputStringValuesInCells(stdHeaderRow, metricsSTDlabels, errorBarColumnsStart, headerstyle);
        }
        
        for (String trackname:methodnames) { 
            rowIndex++;
            row=sheet.getRow(rowIndex);
            if (row==null) row=sheet.createRow(rowIndex);    
            String key=(groupname==null)?trackname:(trackname+"\t"+groupname);
            Statistics stat = benchmark.get(key);  
            double[] values = stat.getStatisticsAsArray(metrics);
            outputStringValueInCell(row, 0, trackname, headerstyle);
            outputNumericValuesInCells(row, values, 1, cellStyle);
            if (aggregate) {
                if (stat.hasStandardDeviations()) {
                    double[] std = stat.getStatisticsStandardDeviationsAsArray(metrics);                      
                    outputNumericValuesInCells(row, std, errorBarColumnsStart, cellStyle);
                } else {
                    // if a track is not aggregated, output 0.0 as stdev value for all metrics, or else Excel will complain later about missing values
                    outputNumericValuesInCells(row, new double[metrics.length], errorBarColumnsStart, null); // don't use the same cell style, as a signal that this row is just defaults
                }
            }
        }
               
        int numTracks = methodnames.size();
        XDDFCategoryDataSource categories = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(headerRow+1, headerRow+numTracks, 0, 0)); //   
             
        int seriesNumber = 0; // 
        int existingSeries = chartData.getSeriesCount();      
        for (String metric:metrics) { // Set up new series from the columns. Each metric is a series and each method is a category [!]
            XDDFNumericalDataSource values = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(headerRow+1, headerRow+numTracks, seriesNumber+1, seriesNumber+1));
            XDDFBarChartData.Series series;
            
            if (seriesNumber<existingSeries) { // first series exists already so we just replace it
                series = (XDDFBarChartData.Series)chartData.getSeries(seriesNumber);
                series.replaceData(categories, values);          
            } else {            
                series = (XDDFBarChartData.Series)chartData.addSeries(categories, values);                                
            }
            String longname = (isLongNameForStatistic(metric))?metric:getLongNameForStatistic(metric);
            if (longname==null) longname=metric;
            Color color = vizSettings.getSystemColor(longname);
            setExcelBarChartColor(series, color);
            series.setTitle(null, new CellReference(sheet.getSheetName(), headerRow,  seriesNumber+1, true, true) );
            if (aggregate) {
                addErrorBars(series, sheet, headerRow+1, headerRow+numTracks, errorBarColumnsStart+seriesNumber, errorBarColumnsStart+seriesNumber, true);
                addErrorBars(series, sheet, headerRow+1, headerRow+numTracks, errorBarColumnsStart+seriesNumber, errorBarColumnsStart+seriesNumber, false);               
            }
            seriesNumber++;
        }              
        barChart.plot(chartData);    
        
        // ==============   CHART 2  ==================
        
        // In the second plot the series are tracks/methods and the categories are metrics         
        barChart = drawing.getCharts().get(1);
        chartData = barChart.getChartSeries().get(0);        
        seriesNumber = 0; // 
        existingSeries = chartData.getSeriesCount();        
        categories = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(headerRow, headerRow, 1, metrics.length)); //        
        for (String trackname:methodnames) { // Set up new series from the rows
            int seriesRow = headerRow+1+seriesNumber;
            XDDFNumericalDataSource values = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(seriesRow, seriesRow, 1, metrics.length));
            XDDFBarChartData.Series series;
            if (seriesNumber<existingSeries) { // first series exists already so we just replace it
                series = (XDDFBarChartData.Series)chartData.getSeries(seriesNumber);
                series.replaceData(categories, values);             
            } else {
                series = (XDDFBarChartData.Series)chartData.addSeries(categories, values);
                // Now we must set the name of series as well, or Excel will complain when opening the file!  get "getF()" property should return a cell reference pointing to the name
                if (series.getCTBarSer().getTx()==null) series.getCTBarSer().addNewTx();
                if (series.getCTBarSer().getTx().getStrRef()==null) series.getCTBarSer().getTx().addNewStrRef();               
                series.getCTBarSer().getTx().getStrRef().setF("$A$"+(seriesRow+1)); // setF("'"+sheetname+"'!$A$"+(seriesRow+1));               
            }
            series.setTitle(null, new CellReference(sheet.getSheetName(), seriesRow,  0, true, true) );
            if (aggregate) {
                // addErrorBars(series, sheet, seriesRow, seriesRow, errorBarColumnsStart, errorBarColumnsStart+metrics.length-1);
                addErrorBars(series, sheet, seriesRow, seriesRow, errorBarColumnsStart, errorBarColumnsStart+metrics.length-1, true);
                addErrorBars(series, sheet, seriesRow, seriesRow, errorBarColumnsStart, errorBarColumnsStart+metrics.length-1, false);               
            }            
            Color color = vizSettings.getForeGroundColor(trackname);
            setExcelBarChartColor(series, color);
            seriesNumber++;
        }              
        barChart.plot(chartData);    

        // reposition the two charts so that they are below the statistics table
        int chartRow1 = headerRow + methodnames.size() + 2;
        int chartHeight = 14;
        int chartRow2 = chartRow1+chartHeight+1;
        setExcelAnchorRows(drawing, new int[][]{ {chartRow1,chartRow1+chartHeight},{chartRow2,chartRow2+chartHeight} }); // chartRow1, chartRow1+chartHeight);       
    }

    private void addErrorBars(XDDFBarChartData.Series barChartSeries, XSSFSheet sheet, int row1, int row2, int col1, int col2, boolean plus) {
        // Get the series at the specified index
        CTBarSer series = barChartSeries.getCTBarSer();   
        CTErrBars errBars = series.addNewErrBars();       
        
        errBars.addNewNoEndCap().setVal(false);
        errBars.addNewErrBarType().setVal((plus)?STErrBarType.PLUS:STErrBarType.MINUS);
        errBars.addNewErrValType().setVal(STErrValType.CUST);
        
        // Set error bar values from the sheet 
        String cellAddress = "$"+getExcelColumnNameForIndex(col1+1)+"$"+(row1+1)+":$"+getExcelColumnNameForIndex(col2+1)+"$"+(row2+1); // cellAddress = ""+sheet.getSheetName()+"!$"+getExcelColumnNameForIndex(col1+1)+"$"+(row1+1)+":$"+getExcelColumnNameForIndex(col2+1)+"$"+(row2+1);
        CTNumDataSource source = (plus)?errBars.addNewPlus():errBars.addNewMinus(); 
        CTNumRef numRef = source.addNewNumRef();                
        numRef.setF(cellAddress);        

        // Add numCache with actual values (or else it does not work)
        if (col1==col2) { // error values from a single column
            int numpoints = row2 - row1 + 1;
            CTNumData numCache  = numRef.addNewNumCache();    
            numCache.setFormatCode("General");            
            CTUnsignedInt ptCount = numCache.addNewPtCount();        
            ptCount.setVal(numpoints);
            for (int i=0; i<numpoints; i++) {
                int rowIndex = row1+i;
                Row row = sheet.getRow(rowIndex);
                Cell cell = row.getCell(col1);
                double value = cell.getNumericCellValue();
                CTNumVal numVal = numCache.addNewPt();
                numVal.setIdx(i); 
                numVal.setV(String.valueOf(value));          
            }            
        } else if (row1==row2){ // error values from a single row
            int numpoints = col2 - col1 + 1;
            CTNumData numCache  = numRef.addNewNumCache();       
            numCache.setFormatCode("General");            
            CTUnsignedInt ptCount = numCache.addNewPtCount();        
            ptCount.setVal(numpoints);      
            for (int i=0; i<numpoints; i++) {
                int col = col1+i;
                Row row = sheet.getRow(row1);
                Cell cell = row.getCell(col);
                double value = cell.getNumericCellValue();
                CTNumVal numVal = numCache.addNewPt();
                numVal.setIdx(i);
                numVal.setV(String.valueOf(value));           
            }                               
        }            
    }     
    
    
    private void setExcelBarChartColor(XDDFBarChartData.Series series, Color color) {
        XDDFShapeProperties shapeProperties = new XDDFShapeProperties();
        shapeProperties.setFillProperties(getExcelFillColor(color));
        series.setShapeProperties(shapeProperties);       
    }
    
    /**
     * Repositions charts in a sheet by adjusting their start and end row coordinates
     * @param drawing
     * @param newCoordinates list of pairs. Each pair should contain a startRow and an endRow for one Chart
     */
    private void setExcelAnchorRows(XSSFDrawing drawing, int[][] newCoordinates) {
       // This is a really hacky way to reposition a Chart by directly editing the "drawing1.xml" file which is part of the Excel file (which is really just a ZIP-archive)
        String xqNamespace = "declare namespace xdr='http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing';"; 
        XmlCursor cursor = drawing.getCTDrawing().newCursor();  
        cursor.selectPath(xqNamespace + "./xdr:twoCellAnchor");
        int chart = 0;
        while (cursor.toNextSelection()) {
            if (newCoordinates==null || chart>=newCoordinates.length) break;
            int[] chartCoordinates = newCoordinates[chart];
            setValueofExcelXMLelement(cursor.newCursor(),new String[]{"from","row"}, ""+chartCoordinates[0]);
            setValueofExcelXMLelement(cursor.newCursor(),new String[]{"to","row"}, ""+chartCoordinates[1]);                      
            chart++;
        }                
    }   
    
    /**
     * Given a cursor to a parent element, this method will traverse down the nested
     * structure searching for nested elements in the order listed in 'path'
     * and then replace the value of the final element with newValue
     * @param parent
     * @param path
     * @param newValue 
     */
    private void setValueofExcelXMLelement(XmlCursor cursor, String[] path, String newValue) {
        int level = 0;
        int safety = 0;
        while (level<path.length) {
            safety++; if (safety>1000) return;
            String targetChild = path[level];
            if (cursor.toFirstChild()) {
                do {
                    safety++; if (safety>1000) return;
                    String nodename = cursor.getName().getLocalPart();
                    if (nodename.equals(targetChild)) {
                        if (level==path.length-1) { // we have found the final target
                            cursor.setTextValue(""+newValue);
                            return;
                        } else { // We found the element, but we are not finished with the path. Keep traversing down
                            level++;
                            break; 
                        }
                    }
                } while (cursor.toNextSibling());
            }   
        }
    }
    
    
    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        RegionDataset answer=(RegionDataset)task.getParameter("Answer");
        answerTrackName=answer.getName();
        SequenceGroup groups=(SequenceGroup)task.getParameter("Groups");
        Double fraction=(Double)task.getParameter("Site overlap");
        if (fraction!=null) {
            siteOverlapFraction=fraction.doubleValue();
            if (siteOverlapFraction<=0) siteOverlapFraction=0.01;
            else if (siteOverlapFraction>=1.0) siteOverlapFraction=1.0;
        }
        ArrayList<Data> predictions=task.getEngine().getAllDataItemsOfType(RegionDataset.class);
        int numberofpredictions=predictions.size()-1; // subtract one because the answer set is also in the predictions list
        if (numberofpredictions==0) throw new Exception("No prediction tracks to evaluate");
        boolean groupedByPartition=groups instanceof SequencePartition;
        
        // first: count FP,TP,FN,TP for each track and sequence and make the results available in a LUT
        storage=new HashMap<String,int[]>();
        ArrayList<String> sequenceNames=null;
        if (groupedByPartition) {
            SequencePartition partition=(SequencePartition)groups;
            sequenceNames=partition.getAllNamesOfSequencesInClusters();
        } else { // single collection
            if (groups instanceof SequenceCollection) sequenceNames=((SequenceCollection)groups).getAllSequenceNames();
            else sequenceNames=task.getEngine().getDefaultSequenceCollection().getAllSequenceNames();            
        }
        int sequenceCollectionSize=sequenceNames.size();
        int totalSize=sequenceCollectionSize*numberofpredictions;
        TaskRunner taskRunner=task.getEngine().getTaskRunner();
        task.setProgress(0L,totalSize);
        long[] counters=new long[]{0,0,totalSize}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences

        ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(totalSize);
        
        for (Data track:predictions) { // now evaluate on all clusters taken together
            String trackname=track.getName();
            if (trackname.equals(answerTrackName)) continue; // do not compare answer with answer
            for (int i=0;i<sequenceCollectionSize;i++) {
                String sequenceName=sequenceNames.get(i);
                processTasks.add(new ProcessSequenceTask((RegionDataset)track, answer, sequenceName, task, counters));
            }
        }
        List<Future<String>> futures=null;
        int countOK=0;            
        try {
            futures=taskRunner.invokeAll(processTasks); // this call apparently blocks until all tasks finish (either normally or by exceptions or being cancelled)                             
            for (Future<String> future:futures) {
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
        if (countOK!=totalSize) {
            throw new ExecutionError("Some mysterious error occurred while performing analysis: "+getAnalysisName());
        }         
        
        // next: summarize the counts into track/group statistics
        if (groupedByPartition) { // evaluate separately on all clusters in partition, then on all sequences
            SequenceCollection allSequencesInPartition=null;            
            allSequencesInPartition=new SequenceCollection("all");
            SequencePartition partition=(SequencePartition)groups;
            ArrayList<String> clusterNames=partition.getClusterNames();
            int size=clusterNames.size()+1; // +1 because the 'all' collection will be evaluated at last
            int k=0;
            for (String clusterName:clusterNames) {
                groupnames.add(clusterName);
                SequenceCollection cluster=partition.getClusterAsSequenceCollection(clusterName, task.getEngine());
                for (Sequence seq:cluster.getAllSequences(task.getEngine())) allSequencesInPartition.addSequence(seq); // add to collection 'all'
                int i=0;
                for (Data track:predictions) {
                    String trackname=track.getName();
                    if (trackname.equals(answerTrackName)) continue; // do not compare answer with answer
                    i++;
                    task.setStatusMessage("Executing analysis: "+getAnalysisName()+" ("+i+"/"+numberofpredictions+") : "+clusterName);
                    task.setProgress((int)(i*100f/(float)numberofpredictions),k,size); //
                    Statistics performance=getStatisticsForTrack(trackname, cluster, task);
                    benchmark.put(trackname+"\t"+clusterName, performance);
                }
            } // end for each cluster
            int i=0;
            for (Data track:predictions) { // now evaluate on all clusters taken together
                String trackname=track.getName();
                if (trackname.equals(answerTrackName)) continue; // do not compare answer with answer
                i++;
                task.setStatusMessage("Executing analysis: "+getAnalysisName()+" ("+i+"/"+numberofpredictions+")");
                task.setProgress((int)(i*100f/(float)numberofpredictions),size-1,size); //
                predictionTrackNames.add(trackname);
                Statistics performance=getStatisticsForTrack(trackname, allSequencesInPartition, task);
                benchmark.put(trackname, performance);
            }
        } else { // just a single collection to evaluate
            SequenceCollection collection=null;
            String groupname=null;
            if (groups instanceof SequenceCollection) {
                collection=(SequenceCollection)groups;
                groupname=collection.getName();
            }
            else collection=task.getEngine().getDefaultSequenceCollection();
            if (groupname!=null) groupnames.add(groupname);
            int i=0;
            for (Data track:predictions) {
                String trackname=track.getName();
                if (trackname.equals(answerTrackName)) continue; // do not compare answer with answer
                i++;
                task.setStatusMessage("Executing analysis: "+getAnalysisName()+" ("+i+"/"+numberofpredictions+")");
                task.setProgress(i, numberofpredictions); //
                predictionTrackNames.add(trackname);
                Statistics performance=getStatisticsForTrack(trackname, collection, task);
                if (groupname!=null) benchmark.put(trackname+"\t"+groupname, performance);
                else benchmark.put(trackname, performance);
            }
        } // end evaluation of Partition or single group
        storage=null; // release temporary storage
        aggregate=(Boolean)task.getParameter("Aggregate");
        if (aggregate) { // replace tracks with same prefix ending in _x where x is some number with the average statistics for these tracks
            ArrayList<String> newTrackNames=new ArrayList<String>();
            HashMap<String, Statistics> newbenchmark=new HashMap<String, Statistics>();
            HashMap<String,ArrayList<String>> aggregates=new HashMap<String,ArrayList<String>>(); // stores the names of actual tracks that belong under which aggregated track name
            for (String trackname:predictionTrackNames) { // cluster names that should be together
                int i=trackname.lastIndexOf('_');
                if (i>0 && i<trackname.length()-1) {
                    String suffix=trackname.substring(i+1);
                    try {
                        Integer.parseInt(suffix);
                        String prefix=trackname.substring(0, i);
                        if (aggregates.containsKey(prefix)) {
                            aggregates.get(prefix).add(trackname);
                         } else {
                            ArrayList<String> tmp=new ArrayList<String>();
                            tmp.add(trackname);
                            aggregates.put(prefix,tmp);
                            newTrackNames.add(prefix);
                        }
                    } catch (Exception e) { // this is probably not an aggregation suffix, but just a track with underscores in the name
                         newTrackNames.add(trackname);
                    }
                } else { // not an aggregation name
                    newTrackNames.add(trackname);
                }                    
            }
            if (newTrackNames.size()<predictionTrackNames.size()) { // we have aggregate-clustering
                 if (groupnames.isEmpty()) { // no collection is specified. Use all sequences.
                     for (String trackname:newTrackNames) {
                         if (aggregates.containsKey(trackname)) {
                            ArrayList<Statistics> cluster=getClusters(aggregates.get(trackname), null);
                            Statistics aggregatedStats=new Statistics(cluster);
                            newbenchmark.put(trackname, aggregatedStats);  
                         } else newbenchmark.put(trackname, benchmark.get(trackname));
                     }
                 } else { // this case might be a single group or a cluster. In the latter case we should also process the "All" (null) group with sequences from all clusters
                     for (String trackname:newTrackNames) {
                         for (String groupname:groupnames) {  
                             if (aggregates.containsKey(trackname)) {
                                ArrayList<Statistics> cluster=getClusters(aggregates.get(trackname), groupname);
                                Statistics aggregatedStats=new Statistics(cluster);
                                newbenchmark.put(trackname+"\t"+groupname, aggregatedStats);  
                             } else newbenchmark.put(trackname+"\t"+groupname, benchmark.get(trackname+"\t"+groupname));
                         }
                         if (groupedByPartition) { // process the "All" group if we are dealing with clusters
                            if (aggregates.containsKey(trackname)) { 
                               ArrayList<Statistics> cluster=getClusters(aggregates.get(trackname), null);
                               Statistics aggregatedStats=new Statistics(cluster);
                               newbenchmark.put(trackname, aggregatedStats);  
                            } else newbenchmark.put(trackname, benchmark.get(trackname));   
                         }                        
                     }
                 }
                 predictionTrackNames=newTrackNames; // replace the old un-aggregated variables
                 benchmark=newbenchmark; // replace the old un-aggregated variables     
            }

        } // end aggregate=true
//         System.err.println("-------- Benchmark keys: ---------------");
//         Object[] keys=benchmark.keySet().toArray();
//         java.util.Arrays.sort(keys);
//         for (Object key:keys) System.err.println("  '"+key+"'");        
    } // end runAnalysis

    private ArrayList<Statistics> getClusters(ArrayList<String> tracknames, String groupname) {
        ArrayList<Statistics> results=new ArrayList<Statistics>(tracknames.size());
        for (String trackname:tracknames) {
            String fullname=trackname;
            if (groupname!=null) fullname=fullname+"\t"+groupname;
            results.add(benchmark.get(fullname));
        }
        return results;
    }
    
    /**
     * Calculates performance statistics for a prediction single track and returns the numbers in a Statistics object
     * @param prediction The name of the track containing the predictions
     * @param task
     * @return
     * @throws Exception
     */  
    private Statistics getStatisticsForTrack(String predictionTrack,  SequenceCollection collection, OperationTask task) throws Exception {
        if (collection==null) collection=task.getEngine().getDefaultSequenceCollection();
        int[] counts=new int[7]; // nTP,nFP,nTN,nFN,sTP,sFP,sFN
        ArrayList<String> sequences=collection.getAllSequenceNames();
        for (int i=0;i<sequences.size();i++) {
            String sequenceName=sequences.get(i);
            int[] results=storage.get(sequenceName+","+predictionTrack);
            counts[0]+=results[0]; // nTP
            counts[1]+=results[1]; // nFP
            counts[2]+=results[2]; // nTN
            counts[3]+=results[3]; // nFN
            counts[4]+=results[4]; // sTP
            counts[5]+=results[5]; // sFP
            counts[6]+=results[6]; // sFN
       }
       Statistics statistics=new Statistics(counts);
       return statistics;
    }    


    /** counts the number of TP, FP, TN, FN, sTP, sFP and sFN in a single sequence and returns them as an int[] in that order*/
    private int[] countTrueFalse(RegionSequenceData prediction, RegionSequenceData answer) {
        int[] results = new int[]{0,0,0,0,0,0,0};
        
        // calculate nucleotide-level statistics
        // This is done by first flattening consecutive segments of the tracks into int[] buffers and then comparing these buffers position-by-position
        int buffersize=10000;          
        int sequenceSize=prediction.getSize();
        if (sequenceSize<buffersize) buffersize=sequenceSize;
        int[] buffer1=new int[buffersize];     
        int[] buffer2=new int[buffersize];     
        int bufferstart=0;
        for (int pos=0;pos<sequenceSize;pos++) {
            if (pos%buffersize==0) { // flatten new segment
                int bufferend=bufferstart+buffersize-1;
                if (bufferend>sequenceSize-1) bufferend=sequenceSize-1;
                buffer1=prediction.flattenSegment(buffer1, bufferstart, bufferend);
                buffer2=answer.flattenSegment(buffer2, bufferstart, bufferend);
                bufferstart+=buffersize;
            }
            boolean hit1=buffer1[pos%buffersize]>0;
            boolean hit2=buffer2[pos%buffersize]>0;
                 if (hit1 && hit2) results[0]++; // a TP
            else if (hit1 && !hit2) results[1]++; // a FP
            else if (!hit1 && !hit2) results[2]++; // a TN
            else if (!hit1 && hit2) results[3]++; // a FN            
        }                   

        // calculate site-level statistics
        // first check the answers and see if they are overlapped by any predictions
        // Those that are not overlapped are false negatives
        for (Region answerRegion:answer.getOriginalRegions()) {
            ArrayList<Region> predictionsOverlappingAnswer=prediction.getRegionsOverlappingGenomicInterval(answerRegion.getGenomicStart(), answerRegion.getGenomicEnd());
            boolean overlappedByPrediction=false;
            for (Region pred:predictionsOverlappingAnswer) {
                if (sitesOverlap(answerRegion, pred, siteOverlapFraction)) {overlappedByPrediction=true;break;} 
            }
            if (!overlappedByPrediction) results[6]++; // a site-level FN (answer not sufficiently overlapped by any prediction)
        }
        // now check the predictions and see if they overlap with the answers. These will be either TP (if they overlap) or FP (if they don't)
        for (Region predictionRegion:prediction.getOriginalRegions()) {
            ArrayList<Region> answersOverlappingPrediction=answer.getRegionsOverlappingGenomicInterval(predictionRegion.getGenomicStart(), predictionRegion.getGenomicEnd());
            boolean overlappedByAnswer=false;
            for (Region answerRegion:answersOverlappingPrediction) {
                if (sitesOverlap(answerRegion, predictionRegion, siteOverlapFraction)) {overlappedByAnswer=true;break;} 
            }
            if (overlappedByAnswer) results[4]++; // a site-level TP
            else results[5]++; // a site-level FP
        }   
        return results;
    }    
    
    
    protected class ProcessSequenceTask implements Callable<String> {
        final String sequenceName;
        final RegionDataset prediction;
        final RegionDataset answer;
        final long[] counters; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final OperationTask task;  
        
        public ProcessSequenceTask(RegionDataset prediction, RegionDataset answer, String sequenceName, OperationTask task, long[] counters) {
           this.sequenceName=sequenceName;
           this.prediction=prediction;
           this.answer=answer;
           this.counters=counters;
           this.task=task;
        }
         
        @Override
        @SuppressWarnings("unchecked")
        public String call() throws Exception {
            synchronized(counters) {
               counters[0]++; // number of sequences started  
            }        
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            RegionSequenceData predictionSequence=(RegionSequenceData)prediction.getSequenceByName(sequenceName);
            RegionSequenceData  answerSequence=(RegionSequenceData)answer.getSequenceByName(sequenceName);
            
            int[] results=countTrueFalse(predictionSequence,answerSequence);           
            
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                String key=sequenceName+","+prediction.getName();
                storage.put(key, results);
                task.setStatusMessage("Executing analysis: "+getAnalysisName()+"  ("+counters[1]+"/"+counters[2]+")");
                task.setProgress(counters[1],counters[2]);                              
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();    
            return sequenceName;
        }   
    } 

    /** Returns true if the prediction region overlaps the answer region with at least the specified 
     *  percentage of nucleotides (given as a double in the range [0,1]) relative to the length of the answer
     *  I.e. if "minOverlap" is 0.25 at least 25% of the nucleotides in the answer region must be covered by the prediction
     */
    private boolean sitesOverlap(Region answer, Region prediction, double minOverlap) {
        int answer_start=answer.getRelativeStart();
        int answer_end=answer.getRelativeEnd();
        int prediction_start=prediction.getRelativeStart();
        int prediction_end=prediction.getRelativeEnd();
        if (answer_start>prediction_end || prediction_start>answer_end) return false; // no overlap
        if (answer_start>=prediction_start && answer_end<=prediction_end) { // answer fully within prediction
            return true; // the whole site is covered
        } else if (prediction_start>=answer_start && prediction_end<=answer_end) { // prediction within answer
             return (double)(prediction_end-prediction_start+1)/(double)(answer_end-answer_start+1)>=minOverlap; // ratio of length of prediction divided by length of answer should be more than specified
        } else if (answer_start<=prediction_start && prediction_start<=answer_end) { // partial overlap with answer first
            int overlapLength=answer_end-prediction_start+1;
            return (double)overlapLength/(double)(answer_end-answer_start+1)>=minOverlap;
        } else if (prediction_start<=answer_start && answer_start<=prediction_end) { // partial overlap with prediction first
            int overlapLength=prediction_end-answer_start+1;
            return (double)overlapLength/(double)(answer_end-answer_start+1)>=minOverlap;
        } else throw new NullPointerException("Sloppy-programming error: BenchmarkAnalysis.sitesOverlap()");
    }
    
    @Override
    protected JPanel getDisplayPanel(MotifLabGUI gui, boolean modal) {
        String group=null;
        if (groupnames!=null && groupnames.size()==1) group=groupnames.get(0); 
        Statistics temp=new Statistics();    
        JTable table=new JTable(new BenchmarkTableModel(gui.getEngine(), temp.getStatisticNames(), group));
        table.setAutoCreateRowSorter(true);
        table.setShowGrid(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setCellSelectionEnabled(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus        
        ToolTipHeader header = new ToolTipHeader(table.getColumnModel());
        table.setTableHeader(header);
        table.getTableHeader().setReorderingAllowed(false);  
        CellRenderer_TableRenderer renderer=new CellRenderer_TableRenderer();
        table.setDefaultRenderer(Double.class, renderer);
        table.setDefaultRenderer(String.class, renderer);
        TableColumn col=table.getColumn(table.getColumnName(0));
        col.setPreferredWidth(100);
        JScrollPane scrollPane=new JScrollPane(table);
        //OutputPanel tablepanel=new OutputPanel(tableDocument, gui);
        JPanel displayPanel=new JPanel(new BorderLayout());
        displayPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8,5,0,5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)
        ));
        JScrollPane graphscrollpane=new JScrollPane();
        GraphPanel graphpanel=new GraphPanel(gui,table,graphscrollpane);              
        graphscrollpane.setViewportView(graphpanel);
        //JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, graphscrollpane, tablepanel);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, graphscrollpane, scrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(420);
        displayPanel.add(splitPane);
        displayPanel.setPreferredSize(new Dimension(800,600));
        return displayPanel;
    }

     
    public ArrayList<String> getStatisticsLongNames() {
        ArrayList<String> list=new ArrayList<String>();
        list.add("Sensitivity");
        list.add("Specificity");
        list.add("Positive Predictive Value");
        list.add("Negative Predictive Value");
        list.add("Performance Coefficient");
        list.add("Average Site Performance");
        list.add("F-measure");    
        list.add("Accuracy");
        list.add("Correlation Coefficient");
        list.add("Sensitivity (site)");
        list.add("Positive Predictive Value (site)");        
        list.add("Average Site Performance (site)");
        list.add("Performance Coefficient (site)");    
        list.add("F-measure (site)");    
        return list;
    }

    public String getLongNameForStatistic(String shortname) {
         if (shortname==null) return null;
         else if (shortname.equalsIgnoreCase("Sn") || shortname.equalsIgnoreCase("nSn")) return "Sensitivity";
         else if (shortname.equalsIgnoreCase("Sp") || shortname.equalsIgnoreCase("nSp")) return "Specificity";
         else if (shortname.equalsIgnoreCase("PPV") || shortname.equalsIgnoreCase("nPPV")) return "Positive Predictive Value";
         else if (shortname.equalsIgnoreCase("NPV") || shortname.equalsIgnoreCase("nNPV")) return "Negative Predictive Value";
         else if (shortname.equalsIgnoreCase("PC") || shortname.equalsIgnoreCase("nPC")) return "Performance Coefficient";
         else if (shortname.equalsIgnoreCase("ASP") || shortname.equalsIgnoreCase("nASP") || shortname.equalsIgnoreCase("ANP")) return "Average Site Performance";
         else if (shortname.equalsIgnoreCase("F") || shortname.equalsIgnoreCase("nF")) return "F-measure";
         else if (shortname.equalsIgnoreCase("Acc") || shortname.equalsIgnoreCase("nAcc")) return "Accuracy";
         else if (shortname.equalsIgnoreCase("CC") || shortname.equalsIgnoreCase("nCC")) return "Correlation Coefficient";
         else if (shortname.equalsIgnoreCase("sSn")) return "Sensitivity (site)";
         else if (shortname.equalsIgnoreCase("sPPV")) return "Positive Predictive Value (site)";         
         else if (shortname.equalsIgnoreCase("sASP")) return "Average Site Performance (site)";         
         else if (shortname.equalsIgnoreCase("sPC")) return "Performance Coefficient (site)";         
         else if (shortname.equalsIgnoreCase("sF") || shortname.equalsIgnoreCase("sF")) return "F-measure (site)";         
         else return null;           
    }

   public String getShortNameForStatistic(String longname) {
         if (longname==null) return null;
         else if (longname.equalsIgnoreCase("Sensitivity")) return "Sn";
         else if (longname.equalsIgnoreCase("Specificity")) return "Sp";
         else if (longname.equalsIgnoreCase("Positive Predictive Value")) return "PPV";
         else if (longname.equalsIgnoreCase("Negative Predictive Value")) return "NPV";
         else if (longname.equalsIgnoreCase("Performance Coefficient")) return "PC";
         else if (longname.equalsIgnoreCase("Average Site Performance")) return "ASP";
         else if (longname.equalsIgnoreCase("F-measure")) return "F";
         else if (longname.equalsIgnoreCase("Accuracy")) return "Acc";
         else if (longname.equalsIgnoreCase("Correlation Coefficient")) return "CC";
         else if (longname.equalsIgnoreCase("Sensitivity (site)")) return "sSn";
         else if (longname.equalsIgnoreCase("Positive Predictive Value (site)")) return "sPPV";         
         else if (longname.equalsIgnoreCase("Average Site Performance (site)")) return "sASP";      
         else if (longname.equalsIgnoreCase("Performance Coefficient (site)")) return "sPC";      
         else if (longname.equalsIgnoreCase("F-measure (site)")) return "sF";         
         else return null;           
    }

   /** returns true if the name is a known long name*/
   public boolean isLongNameForStatistic(String name) {
         if (name==null) return false;
         else if (name.equalsIgnoreCase("Sensitivity")) return true;
         else if (name.equalsIgnoreCase("Specificity")) return true;
         else if (name.equalsIgnoreCase("Positive Predictive Value")) return true;
         else if (name.equalsIgnoreCase("Negative Predictive Value")) return true;
         else if (name.equalsIgnoreCase("Performance Coefficient")) return true;
         else if (name.equalsIgnoreCase("Average Site Performance")) return true;
         else if (name.equalsIgnoreCase("F-measure")) return true;
         else if (name.equalsIgnoreCase("Accuracy")) return true;
         else if (name.equalsIgnoreCase("Correlation Coefficient")) return true;
         else if (name.equalsIgnoreCase("Sensitivity (site)")) return true;
         else if (name.equalsIgnoreCase("Positive Predictive Value (site)")) return true;         
         else if (name.equalsIgnoreCase("Average Site Performance (site)")) return true;         
         else if (name.equalsIgnoreCase("Performance Coefficient (site)")) return true;         
         else if (name.equalsIgnoreCase("F-measure (site)")) return true;         
         else return false;
    }

  /** returns true if the name is a known short name*/
   public boolean isShortNameForStatistic(String name) {
         if (name==null) return false;
         else if (name.equalsIgnoreCase("Sn")) return true;
         else if (name.equalsIgnoreCase("Sp")) return true;
         else if (name.equalsIgnoreCase("PPV")) return true;
         else if (name.equalsIgnoreCase("NPV")) return true;
         else if (name.equalsIgnoreCase("PC")) return true;
         else if (name.equalsIgnoreCase("ASP")) return true;
         else if (name.equalsIgnoreCase("F")) return true;
         else if (name.equalsIgnoreCase("Acc")) return true;
         else if (name.equalsIgnoreCase("CC")) return true;
         else if (name.equalsIgnoreCase("sSn")) return true;
         else if (name.equalsIgnoreCase("sPPV")) return true;         
         else if (name.equalsIgnoreCase("sASP")) return true;         
         else if (name.equalsIgnoreCase("sPC")) return true;         
         else if (name.equalsIgnoreCase("sF")) return true;         
         else return false;
    }

   private class GraphPanel extends JPanel {
        private BufferedImage image;
        private MotifLabGUI gui;
        private JComboBox statisticCombobox;
        private JComboBox showGroupCombobox;
        private JComboBox groupByCombobox;
        private JTable table;
        private JScrollPane parent;

        public GraphPanel(MotifLabGUI gui, final JTable table, JScrollPane parent) {
            super();
            this.gui=gui;
            this.table=table; // this is just a reference to update the table. The table is not drawn in this panel
            this.parent=parent;
            this.setLayout(new FlowLayout(FlowLayout.LEFT));
            this.setBackground(Color.WHITE);
            this.setOpaque(true);
            statisticCombobox=new JComboBox(new String[]{"All","Sensitivity", "Specificity","Positive Predictive Value","Negative Predictive Value","Performance Coefficient","Average Site Performance","F-measure","Accuracy","Correlation Coefficient","Sensitivity (site)","Positive Predictive Value (site)","Average Site Performance (site)","Performance Coefficient (site)","F-measure (site)"});
            statisticCombobox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateGraphImageAndTable();                   
                }
            });
            String[] groupslist;
            if (groupnames.size()==1) {
                groupslist=new String[]{groupnames.get(0)};
            } else {
                groupslist=new String[groupnames.size()+1];
                for (int i=0;i<groupnames.size();i++) groupslist[i+1]=groupnames.get(i);
                groupslist[0]="";
            }
            showGroupCombobox=new JComboBox(groupslist);
            showGroupCombobox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateGraphImageAndTable();
                }
            });
            groupByCombobox=new JComboBox(new String[]{"Method","Statistic/Group"});
            groupByCombobox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateGraphImage();
                }
            });
            this.add(new JLabel("Statistic  "));
            this.add(statisticCombobox);
            JLabel groupLabel=new JLabel("            Group  ");
            this.add(groupLabel);
            this.add(showGroupCombobox);
            this.add(new JLabel("            X-axis  "));
            this.add(groupByCombobox);
            if (groupnames.isEmpty()) {
               groupLabel.setEnabled(false);
               showGroupCombobox.setEnabled(false);
            }
            statisticCombobox.setSelectedIndex(0);
//            gui.addRedrawListener(new RedrawListener() {
//                @Override
//                public void redrawEvent() {
//                    updateGraphImageAndTable(); // this is probably overkill!
//                }
//            });            
        }

        private void updateGraphImageAndTable() {
            updateGraphImage();
            String group=(String)showGroupCombobox.getSelectedItem();
            if (group.isEmpty()) group=null;
            String[] stats=null;
            String selected=(String)statisticCombobox.getSelectedItem();
            if (selected.equals("All")) {
                Statistics temp=new Statistics();  
                stats=temp.getStatisticNames();
            } else {
                stats=new String[]{getShortNameForStatistic(selected)};
            } 
            table.setModel(new BenchmarkTableModel(GraphPanel.this.gui.getEngine(), stats, group));
        }
        
        public void updateGraphImage() {
            String statisticname=(String)statisticCombobox.getSelectedItem();
            String showgroupname=(String)showGroupCombobox.getSelectedItem();
            if (showgroupname.isEmpty()) showgroupname=null;
            String xaxisname=(String)groupByCombobox.getSelectedItem();
            boolean groupByMethod=xaxisname.equals("Method");
            ArrayList<String> showstatistics=new ArrayList<String>();
            if (statisticname.equals("All")) showstatistics.addAll(getStatisticsLongNames());
            else showstatistics.add(statisticname);
            try {
                image=createGraphImage(null, gui.getEngine(),showstatistics,showgroupname,groupByMethod,true,1.0,"normal");
            }
            catch (Exception e) {
                gui.logMessage(e.getClass()+":"+e.getMessage());
                //e.printStackTrace(System.err);
            }
            if (image!=null) {
                int width=image.getWidth()+30;    if (width<550) width=550;
                int height=image.getHeight()+22;  if (height<300) height=300;
                this.setPreferredSize(new Dimension(width,height));
            }
            parent.revalidate();
            parent.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Dimension d=getSize();
            g.setColor(this.getBackground());
            g.fillRect(0, 0, d.width, d.height);
            if (image!=null) g.drawImage(image, 8, 20, this);
        }
    }


    private Color[] getSystemColorsForStatistics(ArrayList<String> statistics, VisualizationSettings settings) {
        Color[] colors=new Color[statistics.size()];
        for (int i=0;i<statistics.size();i++) {
            colors[i]=settings.getSystemColor(statistics.get(i));
        }
        return colors;
    }

    
    private class BenchmarkTableModel extends AbstractTableModel {
        private String[] columnNames=null;
        private Object[][] data; // just to get them in a specific order
        private Double[][][] stddata=null; // first two is row/column, the next is [0]=std,[1]=min,[2]=max,[3]=count       
    
        public BenchmarkTableModel(MotifLabEngine engine, String[] statistics, String groupname) {
            columnNames=new String[statistics.length+1];
            columnNames[0]="Track";
            for (int i=0;i<statistics.length;i++) {
                String abbr=statistics[i];
                if (isLongNameForStatistic(abbr)) abbr=getShortNameForStatistic(abbr);
                columnNames[i+1]=abbr;
            }
            VisualizationSettings settings=engine.getClient().getVisualizationSettings();
            ArrayList<String> methodnames=settings.sortFeaturesAccordingToOrderInMasterList(predictionTrackNames, true, aggregate);
            int count=0;
            for (String trackname:methodnames) {
                if (!settings.isTrackVisible(trackname)) continue; // track is hidden
                String key=(groupname==null)?trackname:(trackname+"\t"+groupname);
                if (!benchmark.containsKey(key)) continue; // this should hopefully not happen, but I will include this test to avoid more serious errors
                count++;
            }
            data=new Object[count][columnNames.length];
            int i=0; 
            for (String trackname:methodnames) {
                if (!settings.isTrackVisible(trackname)) continue; // track is hidden
                String key=(groupname==null)?trackname:(trackname+"\t"+groupname);
                data[i][0]=trackname;
                Statistics stat=benchmark.get(key);
                //System.err.println("Stat for key='"+key+"' trackname='"+trackname+"' groupname='"+groupname+"' => "+(stat!=null)+", has std="+((stat==null)?"no":stat.hasStandardDeviations()));
                if (stat.hasStandardDeviations() && stddata==null) stddata=new Double[count][columnNames.length][4];
                for (int j=0;j<statistics.length;j++) {
                    String metric=statistics[j];
                    Double value=stat.getStatisticValue(metric);                
                    data[i][j+1]=value;
                    if (stat.hasStandardDeviations()) {
                        stddata[i][j+1][0]=(Double)stat.getStatisticStandardDeviation(metric); 
                        stddata[i][j+1][1]=(Double)stat.getStatisticMinValue(metric); 
                        stddata[i][j+1][2]=(Double)stat.getStatisticMaxValue(metric); 
                        stddata[i][j+1][3]=(Double)stat.getStatisticNumberOfValues(metric); 
                    } 
                }
                i++;
            }                      
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            if (columnIndex==0) return String.class;
            else return Double.class;
        }

        public String getCellDescription(int rowIndex, int columnIndex) {
            return "<b>"+getColumnName(columnIndex)+"</b> for <b>"+data[rowIndex][0]+"</b>";
        }        
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data[rowIndex][columnIndex];
        }
        
        /** Returns the Standard deviation, min value, max value, counts*/
        public Double[] getAdditionalValuesAt(int rowIndex, int columnIndex) {
            if (stddata==null) return null;
            else {
                Double[] std=stddata[rowIndex][columnIndex];
                if (std!=null && std.length==4 && std[0]!=null && std[1]!=null && std[2]!=null && std[3]!=null) return std;
                else return null;
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
            return data.length;
        }

    }    
    
    private class CellRenderer_TableRenderer extends DefaultTableCellRenderer  {        
        DecimalFormat formatter;
        public CellRenderer_TableRenderer() {
           super();
           formatter=new DecimalFormat("0.000");
           formatter.setMaximumFractionDigits(3);
           formatter.setMinimumFractionDigits(3);
        }
        @Override
        public void setValue(Object value) {
           if (value==null) {
               setText("");
           } else if (value instanceof Double) {
              if (Double.isNaN((Double)value)) setText("NaN");
              else setText(formatter.format((Double)value));
           } else {
               setText(value.toString());             
           }         
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c=super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (column>0 && value instanceof Double) {
                BenchmarkTableModel model=(BenchmarkTableModel)table.getModel();
                String tag=model.getCellDescription(row,column);
                Double[] additional=model.getAdditionalValuesAt(row, column);
                if (additional!=null && additional.length==4 && additional[0]!=null && additional[1]!=null && additional[2]!=null && additional[3]!=null) {
                   double std=additional[0];
                   double min=additional[1];
                   double max=additional[2];
                   int count=additional[3].intValue();
                   String tooltip="<html>"+tag+"<br><br>"+value+" &plusmn; "+std+"<br><br>Min: "+min+"<br>Max: "+max+"<br>Based on "+count+" value"+((count!=1)?"s":"")+"</html>";
                   setToolTipText(tooltip);                    
                } else {
                   String tooltip="<html>"+tag+"<br><br>"+value+"</html>";
                   setToolTipText(tooltip);
                }                               
            } else setToolTipText((value==null)?null:value.toString());
            return c;
        }
        
    }// end class CellRenderer_Classification    
    
      
   /**
    * A 'struct' to hold statistics
    * Given a set of TP,FP,TN,FN counts it will calculate performance statistics which are directly accessible in this object
    */
    private class Statistics implements Serializable, Cloneable {
        int nTP=0,nFP=0,nTN=0,nFN=0;
        int sTP=0,sFP=0,sFN=0;
        HashMap<String,Double> storage=new HashMap<String, Double>();
        String[] statisticnames=new String[]{"Sn","Sp","PPV","NPV","PC","ASP","F","Acc","CC","sSn","sPPV","sASP","sPC","sF"};
        
        /** Calculates statistics based on TP, FP, TN and FN counts */
        public Statistics(int[] counts) {
            nTP=counts[0];
            nFP=counts[1];
            nTN=counts[2];
            nFN=counts[3];
            if (counts.length>=7) {
                sTP=counts[4];
                sFP=counts[5];
                sFN=counts[6];
            }
            double nSn=(nTP+nFN==0)?Double.NaN:(double)nTP/(double)(nTP+nFN);
            double nSp=(nTN+nFP==0)?Double.NaN:(double)nTN/(double)(nTN+nFP);
            double nPPV=(nTP+nFP==0)?Double.NaN:(double)nTP/(double)(nTP+nFP);
            double nNPV=(nTN+nFN==0)?Double.NaN:(double)nTN/(double)(nTN+nFN);
            double nPC=(nTP+nFN+nFP==0)?Double.NaN:(double)nTP/(double)(nTP+nFN+nFP);
            double nASP=(Double.isNaN(nSn) || Double.isNaN(nPPV))?Double.NaN:(nSn+nPPV)/2f;
            double nAcc=(double)(nTP+nTN)/(double)(nTP+nFP+nTN+nFN);
            double nCC=MotifLabEngine.calculateMatthewsCorrelationCoefficient(nTP,nFP,nTN,nFN);
            double sSn=(sTP+sFN==0)?Double.NaN:(double)sTP/(double)(sTP+sFN); 
            double sPPV=(sTP+sFP==0)?Double.NaN:(double)sTP/(double)(sTP+sFP);   
            double sASP=(Double.isNaN(sSn) || Double.isNaN(sPPV))?Double.NaN:(sSn+sPPV)/2f;  
            double sPC=(sTP+sFN+sFP==0)?Double.NaN:(double)sTP/(double)(sTP+sFN+sFP);            
            double sF=0;
            if (Double.isNaN(sSn) || Double.isNaN(sPPV)) sF=Double.NaN;
            else if (sSn>0 && sPPV>0) sF=(2.0*sSn*sPPV)/(sSn+sPPV);
            double nF=0;
            if (Double.isNaN(nSn) || Double.isNaN(nPPV)) nF=Double.NaN;
            else if (nSn>0 && nPPV>0) nF=(2.0*nSn*nPPV)/(nSn+nPPV);            
            storage.put("Sn",nSn);
            storage.put("Sp",nSp);
            storage.put("PPV",nPPV);
            storage.put("NPV",nNPV);
            storage.put("PC",nPC);
            storage.put("ASP",nASP);
            storage.put("F",nF);
            storage.put("Acc",nAcc);
            storage.put("CC",nCC);
            storage.put("sSn",sSn);
            storage.put("sPPV",sPPV);
            storage.put("sASP",sASP);
            storage.put("sPC",sPC);
            storage.put("sF",sF);            
        }
        
        /** Calculates average statistics with standard deviations based on a list of Statistics */
        public Statistics(ArrayList<Statistics> list) {
            int size=list.size();
            double[] values=new double[size];
            double[] stat=null;
            for (int i=0;i<size;i++) {                
                Statistics other=list.get(i);              
                nTP+=other.nTP;
                nFP+=other.nFP;
                nTN+=other.nTN;
                nFN+=other.nFN;   
                sTP+=other.sTP;
                sFP+=other.sFP;
                sFN+=other.sFN;                
            }               
            for (String statistic:statisticnames) {
                for (int i=0;i<size;i++) {
                   values[i]=list.get(i).getStatisticValue(statistic);
                }
                stat=getAverageAndStdDev(values);             
                storage.put(statistic,stat[0]);
                storage.put(statistic+"_std",stat[1]);               
                storage.put(statistic+"_min",stat[2]);               
                storage.put(statistic+"_max",stat[3]);  
                storage.put(statistic+"_count",stat[4]);              
            }          
        }        

        public Statistics() {} // void constructor
               
        /** Returns an array with statistics corresponding to the names in the list */        
        public double[] getStatisticsAsArray(ArrayList<String> list) {
            String[] listAsArray = new String[list.size()];
            return getStatisticsAsArray(list.toArray(listAsArray));
        }
        
        /** Returns an array with statistics corresponding to the names in the list */
        public double[] getStatisticsAsArray(String[] list) {
            double[] result=new double[list.length];
            for (int i=0;i<list.length;i++) {
                String stat=list[i];
                if (isLongNameForStatistic(stat)) stat=getShortNameForStatistic(stat);
                result[i]=getStatisticValue(stat);
            }
            return result;
        }
        
        /** Returns an array with standard deviations for the statistics corresponding to the names in the list */        
        public double[] getStatisticsStandardDeviationsAsArray(ArrayList<String> list) {
            String[] listAsArray = new String[list.size()];
            return getStatisticsStandardDeviationsAsArray(list.toArray(listAsArray));
        }        

        /** Returns an array with standard deviations for the statistics corresponding to the names in the list */
        public double[] getStatisticsStandardDeviationsAsArray(String[] list) {
            double[] result=new double[list.length];
            for (int i=0;i<list.length;i++) {
                String stat=list[i];
                if (isLongNameForStatistic(stat)) stat=getShortNameForStatistic(stat);
                result[i]=getStatisticStandardDeviation(stat);
            }
            return result;
        }

        public double getStatisticValue(String statname) {
            String name=(isLongNameForStatistic(statname))?getShortNameForStatistic(statname):statname;
            if (storage.containsKey(name)) return storage.get(name);
            else return 0;
        }
        
        /** Returns true if any of the statistics have standard deviations */
        public boolean hasStandardDeviations() {
            for (String stat:statisticnames) {
                if (storage.containsKey(stat+"_std")) return true;
            }
            return false;
        } 
        
        /** Returns true if there exists a standard deviation measurement for the specified statistic */
        public boolean hasStandardDeviation(String statname) {
            String name=(isLongNameForStatistic(statname))?getShortNameForStatistic(statname):statname;
            return (storage.containsKey(name+"_std"));
        }        
        
        public double getStatisticStandardDeviation(String statname) {
            String name=(isLongNameForStatistic(statname))?getShortNameForStatistic(statname):statname;
            if (storage.containsKey(name+"_std")) return storage.get(name+"_std");      
            else return 0;
        }
        public double getStatisticMinValue(String statname) {
            String name=(isLongNameForStatistic(statname))?getShortNameForStatistic(statname):statname;
            if (storage.containsKey(name+"_min")) return storage.get(name+"_min");      
            else return 0;
        }
        public double getStatisticMaxValue(String statname) {
            String name=(isLongNameForStatistic(statname))?getShortNameForStatistic(statname):statname;
            if (storage.containsKey(name+"_max")) return storage.get(name+"_max");      
            else return 0;
        }
        /** Returns the number of values used to calculate the given statistic if it is an average */
        public double getStatisticNumberOfValues(String statname) {
            String name=(isLongNameForStatistic(statname))?getShortNameForStatistic(statname):statname;
            if (storage.containsKey(name+"_count")) return storage.get(name+"_count");      
            else return 1;
        }         

        public String[] getStatisticNames() {
            return statisticnames;
        }

        /**
         * Calculates the mean and standard deviations of a list of values
         * @param values
         * @return an array containing the mean [0] and std [1] value, and min [2] and max [3] value
         */
        private double[] getAverageAndStdDev(double[] values) {
            int counted=0; // these are the non-NaN values
            double min=Double.NaN;
            double max=Double.NaN;
            for (int i=0;i<values.length;i++) if (!Double.isNaN(values[i])) counted++;
            if (counted==0) return new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
            double average=0;
            for (int i=0;i<values.length;i++) {
                if (!Double.isNaN(values[i])) {
                    average+=values[i];
                    if (Double.isNaN(min) || values[i]<min) min=values[i];
                    if (Double.isNaN(max) || values[i]>max) max=values[i];
                }
            }
            average=average/(double)counted;
            double std=0;
            for (int i=0;i<values.length;i++) if (!Double.isNaN(values[i])) std+=(average-values[i])*(average-values[i]);  
            std=std/(double)counted;
            std=Math.sqrt(std);
            return new double[]{average,std,min,max,counted};
        }

        @Override
        protected Statistics clone() throws CloneNotSupportedException {
            Statistics cl=new Statistics();
            cl.nTP=this.nTP;
            cl.nTN=this.nTN;
            cl.nFP=this.nFP;
            cl.nFN=this.nFN;
            
            cl.sTP=this.sTP;
            cl.sFP=this.sFP;
            cl.sFN=this.sFN;
                     
            cl.storage=(HashMap<String,Double>)this.storage.clone();              
            return cl;
        }
        // ------------ Serialization of Statistics inner class---------
        private static final long serialVersionUID = 1L;

        private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
             short currentinternalversion=2; // this is an internal version number for serialization of objects of this type
             out.writeShort(currentinternalversion);
             out.defaultWriteObject();
        }

        private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
             short currentinternalversion=in.readShort(); // the internal version number is used to determine correct format of data
             if (currentinternalversion==2) {
                 in.defaultReadObject();
             } else if (currentinternalversion>2) throw new ClassNotFoundException("Newer version");
             else if (currentinternalversion<2) throw new ClassNotFoundException("Incompatible older version");
        }        
        
    } // end of inner class: Statistics

    // ------------ Serialization of BenchmarkAnalysis---------
    private static final long serialVersionUID = 1L;

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
         short currentinternalversion=2; // this is an internal version number for serialization of objects of this type
         out.writeShort(currentinternalversion);
         out.defaultWriteObject();
         out.writeDouble(siteOverlapFraction);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
         short currentinternalversion=in.readShort(); // the internal version number is used to determine correct format of data
         if (currentinternalversion==1) {
             in.defaultReadObject();
         } else if (currentinternalversion==2) {
             in.defaultReadObject();
             siteOverlapFraction=in.readDouble();
         } else if (currentinternalversion>2) throw new ClassNotFoundException("Newer version");
    }

}
