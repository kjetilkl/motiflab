/*
 
 
 */

package motiflab.engine.data.analysis;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import motiflab.engine.data.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.Graph;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.dataformat.DataFormat;
import motiflab.gui.GenericSequenceBrowserPanel;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.VisualizationSettings;
import org.apache.commons.math3.stat.StatUtils;

/**
 *
 * @author kjetikl
 */
public final class GCcontentAnalysis extends Analysis {
    private final static String typedescription="Analysis: GC-content"; 
    private final static String analysisName="GC-content"; 
    private final static String description="Calculates the GC-content for each sequence as well as distribution statistics for sequence collections or clusters";        
    private HashMap<String,Double> results=new HashMap<String,Double>(); // stores sequence names and GC-percentage
    private SequenceGroup sequenceGroups;
    private final static int SORT_BY_NAME=0;
    private final static int SORT_BY_GC_ASCENDING=1;
    private final static int SORT_BY_GC_DESCENDING=2;
    private final static int STATISTIC_MIN=0;
    private final static int STATISTIC_MAX=1;
    private final static int STATISTIC_AVERAGE=2;
    private final static int STATISTIC_STDDEV=3;
    private final static int STATISTIC_MEDIAN=4;
    private final static int STATISTIC_1ST_QUARTILE=5;
    private final static int STATISTIC_3RD_QUARTILE=6;

    private static final String NONE="None";   
    private static final String MEDIAN_QUARTILES="Median+Quartiles"; 
    private static final String MEAN_STD="Average+StDev"; 
    private static final String BOTH="All";     
    
    private static final int PLOT_NONE=0;
    private static final int PLOT_MEDIAN_QUARTILES=1;
    private static final int PLOT_MEAN_STD=2;
    private static final int PLOT_BOTH=3;

    private DecimalFormat decimalformatter=new DecimalFormat("0.0");

    private final String[] variables = new String[]{"GC-content"};


    public GCcontentAnalysis() {
        this.name="GCcontent_temp";
        addParameter("DNA track",DNASequenceDataset.class, null,new Class[]{DNASequenceDataset.class},"The DNA track to estimate GC-content for",true,false);
        addParameter("Groups",SequenceGroup.class, null,new Class[]{SequenceCollection.class,SequencePartition.class},"If provided, the analysis will estimate average GC-content for all sequences in the collection or for individual clusters in a partition",false,false);
    }  
    
    @Override
    public String[] getSourceProxyParameters() {return new String[]{"DNA track"};} 
    
    /** Returns a list of output parameters that can be set when an Analysis is output */
    @Override
    public Parameter[] getOutputParameters() {
        return new Parameter[] {
             new Parameter("Sort by",String.class,"Sequence name",new String[]{"Sequence name","GC ascending","GC descending"},"The property to sort sequences by",false,false),
             new Parameter("Group by clusters",Boolean.class,Boolean.TRUE,new Boolean[]{Boolean.FALSE,Boolean.TRUE},"Group sequences in partition-clusters together (affects sorting order)",false,false),
             new Parameter("Show only groups summary",Boolean.class,Boolean.FALSE,new Boolean[]{Boolean.FALSE,Boolean.TRUE},"Show only the summary statistics for each group and not GC-content for individual sequences",false,false),
             new Parameter("Box plot",String.class, BOTH,new String[]{MEDIAN_QUARTILES,MEAN_STD,BOTH},"Which statistics to show using box plots",false,false),
             new Parameter("Graph scale",Integer.class,100,new Integer[]{10,2000},"Scale of graphics plot (in percent)",false,false)
        };
    }
    
    @Override
    public String[] getOutputParameterFilter(String parameter) {
        if (parameter.equals("Graph scale") || parameter.equals("Box plot")) return new String[]{"HTML"};
        if (parameter.equals("Group by clusters") || parameter.equals("Show only groups summary") || parameter.equals("Sort by")) return new String[]{"HTML","RawData"};        
        return null;
    }     
    
    
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
        if (variablename!=null && variablename.equals("GC-content")) {
            SequenceNumericMap map=new SequenceNumericMap("temp",0);
            for (String seqname:results.keySet()) {
                map.setValue(seqname, results.get(seqname));
            }
            return map;
        }
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }
    
    @Override
    public Class getResultType(String variablename) {
       if (variablename==null) return null;
       else if (variablename.equals("GC-content")) return SequenceNumericMap.class;
       else return null;
    }      
         
    @Override
    public Class getCollateType() {
        return Sequence.class;
    }

    @Override
    public String[] getColumnsExportedForCollation() {
        return variables;
    }

    @Override
    public Class getColumnType(String column) {
        if (column.equals("GC-content")) return Double.class;
        else return null;
    }

    @Override
    public HashMap<String,Object> getColumnData(String column) throws ExecutionError {
        Class coltype=getColumnType(column);
        if (coltype==null) throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
        HashMap<String,Object> columnData=new HashMap<String, Object>();
        for (String motifname:results.keySet()) {
            Double GCcontent=results.get(motifname);
            if (column.equalsIgnoreCase("GC-content")) columnData.put(motifname, GCcontent);
         }
        return columnData;
    }

    @Override
    @SuppressWarnings("unchecked")    
    public GCcontentAnalysis clone() {
        GCcontentAnalysis newanalysis=new GCcontentAnalysis();
        super.cloneCommonSettings(newanalysis);    
        newanalysis.results=(HashMap<String,Double>)this.results.clone();
        if (sequenceGroups instanceof SequenceCollection) newanalysis.sequenceGroups=(SequenceCollection)((SequenceCollection)this.sequenceGroups).clone();
        else if (sequenceGroups instanceof SequencePartition) newanalysis.sequenceGroups=(SequencePartition)((SequencePartition)this.sequenceGroups).clone();
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        GCcontentAnalysis datasource=((GCcontentAnalysis)source);
        this.results=(HashMap<String,Double>)datasource.results.clone();
        if (sequenceGroups instanceof SequenceCollection) this.sequenceGroups=(SequenceCollection)((SequenceCollection)datasource.sequenceGroups);     
        else if (sequenceGroups instanceof SequencePartition) this.sequenceGroups=(SequencePartition)((SequencePartition)datasource.sequenceGroups);     
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
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        boolean groupByCluster=false;
        boolean showOnlySummary=false;
        int scalepercent=100;
        int plots=PLOT_BOTH;
        String sortOrderString="Sequence name";
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             sortOrderString=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             groupByCluster=(Boolean)settings.getResolvedParameter("Group by clusters",defaults,engine);
             showOnlySummary=(Boolean)settings.getResolvedParameter("Show only groups summary",defaults,engine);
             String plotString=(String)settings.getResolvedParameter("Box plot",defaults,engine);
                  if (plotString.equals(NONE)) plots=PLOT_NONE;
             else if (plotString.equals(MEDIAN_QUARTILES)) plots=PLOT_MEDIAN_QUARTILES;
             else if (plotString.equals(MEAN_STD)) plots=PLOT_MEAN_STD;
             else if (plotString.equals(BOTH)) plots=PLOT_BOTH;
             scalepercent=(Integer)settings.getResolvedParameter("Graph scale",defaults,engine);
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        double scale=(scalepercent==100)?1.0:(((double)scalepercent)/100.0);        
        int sortBy=SORT_BY_NAME;
        if (sortOrderString.equals("GC ascending")) sortBy=SORT_BY_GC_ASCENDING;
        else if (sortOrderString.equals("GC descending")) sortBy=SORT_BY_GC_DESCENDING;

        ArrayList<String> sequenceNames=new ArrayList<String>(results.keySet());
        engine.createHTMLheader("GC-content Analysis", null, null, true, true, true, outputobject);
        if (sequenceNames.size()<=60) outputobject.append("<center>",HTML); // center header if all the data 'fits on the screen'
        outputobject.append("<h1 class=\"headline\">GC-content Analysis</h1>\n<br>\n",HTML);
        if (showOnlySummary && sequenceGroups==null) {
            outputobject.append("<br /><br />No sequence groups selected<br />", HTML);
        } else {
            formatGCgraph(outputobject, engine, vizSettings, sortBy, groupByCluster, showOnlySummary, plots, scale);
            outputobject.append("<br /><br />", HTML);
        }
        if (sequenceGroups!=null) formatGroupsTable(outputobject, engine, vizSettings);
        outputobject.append("<br /><br />", HTML);
        if (sequenceNames.size()<=60) outputobject.append("</center>",HTML); //        
        boolean ascending=true;
        boolean sortByGC=true;
        if (sortBy==SORT_BY_GC_DESCENDING) ascending=false;
        if (sortBy==SORT_BY_NAME) sortByGC=false;
        SortOrderComparator sortOrderComparator;
        if (sequenceGroups instanceof SequencePartition && groupByCluster) sortOrderComparator=new SortOrderComparator((SequencePartition)sequenceGroups, sortByGC, ascending);
        else sortOrderComparator=new SortOrderComparator(null, sortByGC, ascending);
        if (!showOnlySummary) {
            Collections.sort(sequenceNames,sortOrderComparator);
            outputobject.append("<table class=\"sortable\">\n", HTML);
            outputobject.append("<tr><th>Sequence</th><th>GC %</th></tr>\n",HTML);
            for (String sequenceName:sequenceNames) {
                Double value=results.get(sequenceName);
                outputobject.append("<tr><td class=\"namecolumn\">"+sequenceName+"</td><td class=\"num\">"+decimalformatter.format(value*100)+"%</td></tr>\n",HTML);
            }
            outputobject.append("</table>\n",HTML);
        }
        outputobject.append("</body>\n</html>\n",HTML);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }

    /** Formats GC-graph and table with min/max/average of groups */
    private void formatGCgraph(OutputData outputobject, MotifLabEngine engine, VisualizationSettings vizSettings, int sortBy, boolean grouping, boolean showOnlySummary, int plots, double scale) {
        File imagefile=outputobject.createDependentFile(engine,"png");
        try {
            createGraphImage(imagefile,engine, vizSettings, sortBy, grouping, showOnlySummary, plots, scale);
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        outputobject.append("<img src=\"file:///"+imagefile.getAbsolutePath()+"\" />",HTML);
    }


    private void formatGroupsTable(OutputData outputobject, MotifLabEngine engine, VisualizationSettings vizSettings) {
            outputobject.append("<table class=\"sortable\">\n", HTML);
            outputobject.append("<tr><th width=\"90\">Group</th><th width=\"90\">Size</th><th width=\"90\">Min</th><th width=\"90\">Max</th><th width=\"90\">Average</th><th width=\"90\">Std.&nbsp;dev.</th><th width=\"90\">Median</th><th width=\"90\">1st&nbsp;Quartile</th><th width=\"90\">3rd&nbsp;Quartile</th></tr>\n",HTML);
            if (sequenceGroups instanceof SequenceCollection) {
                double[] values=getStatistics((SequenceCollection)sequenceGroups);
                outputobject.append("<tr><td style=\"background-color:#FF8888\">"+((SequenceCollection)sequenceGroups).getName()+"</td><td class=\"num\">"+((SequenceCollection)sequenceGroups).size()+"</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_MIN]*100)+"%</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_MAX]*100)+"%</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_AVERAGE]*100)+"%</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_STDDEV]*100)+"%</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_MEDIAN]*100)+"%</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_1ST_QUARTILE]*100)+"%</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_3RD_QUARTILE]*100)+"%</td></tr>\n",HTML);
            } else if (sequenceGroups instanceof SequencePartition) {
                SequencePartition partition=(SequencePartition)sequenceGroups;
                for (String clusterName:partition.getClusterNames()) {
                    Color clusterColor=vizSettings.getClusterColor(clusterName); //.brighter();
                    String colorString=VisualizationSettings.convertColorToHTMLrepresentation(clusterColor);
                    String clusterNameString;
                    if (getBrightness(clusterColor)<130) clusterNameString="<font color=\"#FFFFFF\">"+clusterName+"</font>";
                    else clusterNameString=clusterName;
                    double[] values=getStatistics(partition.getClusterAsSequenceCollection(clusterName, engine));
                    outputobject.append("<tr><td style=\"background-color:"+colorString+"\">"+clusterNameString+"</td><td class=\"num\">"+partition.getClusterSize(clusterName)+"</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_MIN]*100)+"%</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_MAX]*100)+"%</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_AVERAGE]*100)+"%</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_STDDEV]*100)+"%</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_MEDIAN]*100)+"%</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_1ST_QUARTILE]*100)+"%</td><td class=\"num\">"+decimalformatter.format(values[STATISTIC_3RD_QUARTILE]*100)+"%</td></tr>\n",HTML);
                }
            }
            outputobject.append("</table>", HTML);
    }

    private void formatGroupsTableRaw(OutputData outputobject, MotifLabEngine engine) {
            if (sequenceGroups==null) return;
            outputobject.append("#Group\tSize\tMin\tMax\tAverage\tStd.dev.\tMedian\t1st Quartile\t3rd Quartile\n",RAWDATA);
            if (sequenceGroups instanceof SequenceCollection) {
                double[] values=getStatistics((SequenceCollection)sequenceGroups);
                outputobject.append(((SequenceCollection)sequenceGroups).getName()+"\t",RAWDATA);
                outputobject.append(((SequenceCollection)sequenceGroups).size()+"\t",RAWDATA);
                outputobject.append(values[STATISTIC_MIN]+"\t",RAWDATA);
                outputobject.append(values[STATISTIC_MAX]+"\t",RAWDATA);
                outputobject.append(values[STATISTIC_AVERAGE]+"\t",RAWDATA);
                outputobject.append(values[STATISTIC_STDDEV]+"\t",RAWDATA);
                outputobject.append(values[STATISTIC_MEDIAN]+"\t",RAWDATA);
                outputobject.append(values[STATISTIC_1ST_QUARTILE]+"\t",RAWDATA);
                outputobject.append(values[STATISTIC_3RD_QUARTILE]+"\n",RAWDATA);
             } else if (sequenceGroups instanceof SequencePartition) {
                SequencePartition partition=(SequencePartition)sequenceGroups;
                for (String clusterName:partition.getClusterNames()) {
                    double[] values=getStatistics(partition.getClusterAsSequenceCollection(clusterName, engine));
                    outputobject.append(clusterName+"\t",RAWDATA);
                    outputobject.append(partition.getClusterSize(clusterName)+"\t",RAWDATA);
                    outputobject.append(values[STATISTIC_MIN]+"\t",RAWDATA);
                    outputobject.append(values[STATISTIC_MAX]+"\t",RAWDATA);
                    outputobject.append(values[STATISTIC_AVERAGE]+"\t",RAWDATA);
                    outputobject.append(values[STATISTIC_STDDEV]+"\t",RAWDATA);
                    outputobject.append(values[STATISTIC_MEDIAN]+"\t",RAWDATA);
                    outputobject.append(values[STATISTIC_1ST_QUARTILE]+"\t",RAWDATA);
                    outputobject.append(values[STATISTIC_3RD_QUARTILE]+"\n",RAWDATA);
                }
            }
            outputobject.append("\n", HTML);
    }

    private int getBrightness(Color c) {
        return (int) Math.sqrt(
          c.getRed() * c.getRed() * .241 +
          c.getGreen() * c.getGreen() * .691 +
          c.getBlue() * c.getBlue() * .068);
    }

    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        boolean groupByCluster=false;
        boolean showOnlySummary=false;
        String sortOrderString="Sequence name";
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             sortOrderString=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             groupByCluster=(Boolean)settings.getResolvedParameter("Group by clusters",defaults,engine);
             showOnlySummary=(Boolean)settings.getResolvedParameter("Show only groups summary",defaults,engine);
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        int sortBy=SORT_BY_NAME;
        if (sortOrderString.equals("GC ascending")) sortBy=SORT_BY_GC_ASCENDING;
        else if (sortOrderString.equals("GC descending")) sortBy=SORT_BY_GC_DESCENDING;
        boolean ascending=true;
        boolean sortByGC=true;
        if (sortBy==SORT_BY_GC_DESCENDING) ascending=false;
        if (sortBy==SORT_BY_NAME) sortByGC=false;
        SortOrderComparator sortOrderComparator;
        if (sequenceGroups instanceof SequencePartition && groupByCluster) sortOrderComparator=new SortOrderComparator((SequencePartition)sequenceGroups, sortByGC, ascending);
        else sortOrderComparator=new SortOrderComparator(null, sortByGC, ascending);
        ArrayList<String> sequenceNames=new ArrayList<String>(results.keySet());
        Collections.sort(sequenceNames,sortOrderComparator);
        outputobject.append("# GC-content analysis\n",RAWDATA);   
        formatGroupsTableRaw(outputobject, engine);
        if (!showOnlySummary) {
            outputobject.append("# GC-content in individual sequences\n",RAWDATA);            
            for (String sequenceName:sequenceNames) {
                Double value=results.get(sequenceName);
                outputobject.append(sequenceName+"\t"+value+"\n",RAWDATA);
            }
        }
        if (format!=null) format.setProgress(100);
        return outputobject;
    }

    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        DNASequenceDataset dnasequenceDataset=(DNASequenceDataset)task.getParameter("DNA track");
        SequenceGroup originalSequenceGroups=(SequenceGroup)task.getParameter("Groups");    
        if (originalSequenceGroups!=null) { // clone the SequenceList because the user might change it after running the analysis but before producing output
            if (originalSequenceGroups instanceof SequenceCollection) this.sequenceGroups=(SequenceCollection)((SequenceCollection)originalSequenceGroups).clone();     
            else if (originalSequenceGroups instanceof SequencePartition) this.sequenceGroups=(SequencePartition)((SequencePartition)originalSequenceGroups).clone();   
        }      
        ArrayList<Sequence> sequences=null;
        if (originalSequenceGroups instanceof SequenceCollection) sequences=((SequenceCollection)originalSequenceGroups).getAllSequences(task.getEngine());
        else if (originalSequenceGroups instanceof SequencePartition) sequences=((SequencePartition)originalSequenceGroups).getAllSequencesInClusters(task.getEngine());
        else sequences=task.getEngine().getDefaultSequenceCollection().getAllSequences(task.getEngine());
        int sequenceSize=sequences.size();
        for (int i=0;i<sequenceSize;i++) {
            String sequenceName=sequences.get(i).getName();
            DNASequenceData sequence=(DNASequenceData)dnasequenceDataset.getSequenceByName(sequenceName);
            double GC=findGCcontent(sequence);
            results.put(sequenceName, GC);
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            task.setStatusMessage("Executing analysis: "+getAnalysisName()+" ("+(i+1)+"/"+sequenceSize+")");
            task.setProgress(i, sequenceSize); // the +20 is just to not reach 100% in this loop                                  
        }            
    }
    
 
    /** Returns the GC-content of the sequence */
    private double findGCcontent(DNASequenceData sequencedata) {
        int GCcount=0;
        int length=sequencedata.getSize();
        for (int i=0;i<length;i++) {
            char base=sequencedata.getValueAtRelativePosition(i);
            if (base=='G' || base=='C' || base=='g' || base=='c') GCcount++;
        }        
        return (double)GCcount/(double)length;
    }    
    
 

   
    
  /** Creates a graph based on the current data and saves it to file (if file is not null)*/
    private BufferedImage createGraphImage(File file, MotifLabEngine engine, VisualizationSettings settings, int sortOrder, boolean grouping, boolean showOnlySummary, int boxplot, double scale) throws IOException {
        int graphheight=200; // height of graph in pixels (just the histogram);
        int translateX=50; // the X coordinate for the top of the graph
        int translateY=30; // the Y coordinate for the top of the graph 
        int columnWidth=15;
        int columns=(showOnlySummary)?0:results.size();
        if (sequenceGroups!=null) {
            if (sequenceGroups instanceof SequenceCollection) columns++;
            else if (sequenceGroups instanceof SequencePartition) {
                columns+=((SequencePartition)sequenceGroups).getNumberOfClusters();
            }
        }
        int graphwidth=columns*columnWidth;
        int width=graphwidth+translateX+10; //
        int largestNameSize=findLargestNameSize(showOnlySummary);
        int height=translateY+graphheight+largestNameSize+30;
        BufferedImage image=new BufferedImage((int)Math.round(width*scale),(int)Math.round(height*scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.scale(scale, scale);
        Stroke defaultStroke=g.getStroke();
        BasicStroke fatStroke = new BasicStroke(3f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND);       
        BasicStroke dashed = new BasicStroke(1f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f,3f}, 0f);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);


        double maxgraphvalue=1.0f;
        double tickscaleY=0.1f;      

        Graph graph=new Graph(g, 1, columns, 0, maxgraphvalue, graphwidth, graphheight, translateX, translateY);

        ArrayList<String> sequenceNames=new ArrayList<String>(results.keySet());
        SortOrderComparator sortOrderComparator;
        boolean ascending=true;
        boolean sortByGC=true;
        if (sortOrder==SORT_BY_GC_DESCENDING) ascending=false;
        if (sortOrder==SORT_BY_NAME) sortByGC=false;
        if (sequenceGroups instanceof SequencePartition && grouping) sortOrderComparator=new SortOrderComparator((SequencePartition)sequenceGroups, sortByGC, ascending);
        else sortOrderComparator=new SortOrderComparator(null, sortByGC, ascending);
        Collections.sort(sequenceNames,sortOrderComparator);  
        
        // draw axes and ticks on Y-axis
        g.setColor(Color.BLACK);  
        g.drawLine(translateX-5, translateY, translateX-5, graphheight+translateY); // Y-axis
        g.drawLine(translateX-5, graphheight+translateY, translateX+graphwidth+10, graphheight+translateY); // X-axis
        // Y-axis ticks. Tick scale was determined above!
        int numticks=(int)Math.round(maxgraphvalue/tickscaleY);
        Color lightGray=new Color(230,230,230);
        for (int n=1;n<=numticks;n++) {
            double tickYvalue=n*tickscaleY;
            int tickY=graph.getYforValue(tickYvalue);
            g.setColor(Color.BLACK);
            g.setStroke(defaultStroke);
            g.drawLine(translateX-10, tickY, translateX-5, tickY); // tick
            int tickValue=(int)Math.round(tickscaleY*n*100);
            drawAlignedString(g, tickValue+"%", translateX-15, tickY, 1.0f, 0.3f);            
            if (tickValue==50) {g.setStroke(defaultStroke);g.setColor(Color.GREEN);} else {g.setStroke(dashed);g.setColor(lightGray);}
            g.drawLine(translateX-4, tickY, translateX+graphwidth+10, tickY); // horizontal grid line
        }        
        g.setStroke(defaultStroke);
        int offset=translateX;
        // draw GC-content bar for each sequence
        if (!showOnlySummary) {
            for (int i=0;i<sequenceNames.size();i++) {
                String sequenceName=sequenceNames.get(i);
                double value=results.get(sequenceName);
                int plotY=graph.getYforValue(value);
                int x1=i*columnWidth+translateX;
                int x2=(i+1)*columnWidth+translateX-1;
                g.setColor(lightGray);
                //g.setStroke(dashed);
                g.drawLine((int)((x1+x2)/2f), plotY, (int)((x1+x2)/2f), graphheight+translateY-1);
                g.setStroke(fatStroke);
                Color usecolor=getColorForSequence(sequenceName,settings);
                if (usecolor.equals(Color.WHITE)) usecolor=Color.BLACK; // white can not be seen
                g.setColor(usecolor);
                g.drawLine(x1+2,plotY, x2-2, plotY);
                g.setStroke(defaultStroke);
                // draw tick on X-axis with sequence label
                g.setColor(Color.BLACK);
                g.drawLine((int)((x1+x2)/2f), graphheight+translateY, (int)((x1+x2)/2f), graphheight+translateY+5);
                Color label=settings.getSequenceLabelColor(sequenceName);
                if (label!=null) g.setColor(label);
                drawVerticalString(g,sequenceName,(int)((x1+x2)/2f),graphheight+translateY+8);
                g.setColor(Color.BLACK);
            }
            offset+=columnWidth*sequenceNames.size();
        }
        
        // draw box-and-whiskers for targetSet distribution
        if (sequenceGroups!=null && sequenceGroups instanceof SequenceCollection) {
            double[] statistics=getStatistics((SequenceCollection)sequenceGroups);
            int x1=offset+1;
            int x2=x1+columnWidth;
            // tick on the X-axis (not the label)
            g.setColor(Color.BLACK);    
            g.drawLine((int)((x1+x2)/2f), graphheight+translateY, (int)((x1+x2)/2f), graphheight+translateY+5);
            // box and whiskers
            g.setColor(Color.RED);
            if (boxplot==PLOT_MEDIAN_QUARTILES || boxplot==PLOT_BOTH) graph.drawVerticalBoxAndWhiskers(statistics[STATISTIC_MIN], statistics[STATISTIC_MAX], statistics[STATISTIC_MEDIAN], statistics[STATISTIC_1ST_QUARTILE], statistics[STATISTIC_3RD_QUARTILE], x1,6);
            if (boxplot==PLOT_MEAN_STD || boxplot==PLOT_BOTH) graph.drawVerticalMeanAndStdDeviation(statistics[STATISTIC_AVERAGE], statistics[STATISTIC_STDDEV], x1,6);
            // label on the X-axis
            drawVerticalString(g,((SequenceCollection)sequenceGroups).getName(),(int)((x1+x2)/2f),graphheight+translateY+8);
            offset+=columnWidth;
        } else if (sequenceGroups!=null && sequenceGroups instanceof SequencePartition) {
            SequencePartition partition=(SequencePartition)sequenceGroups;
            for (String clusterName:partition.getClusterNames()) {
                double[] statistics=getStatistics(partition.getClusterAsSequenceCollection(clusterName, engine));
                int x1=offset+1;
                int x2=x1+columnWidth;
                // tick on the X-axis (not the label)
                g.setColor(Color.BLACK);
                g.drawLine((int)((x1+x2)/2f), graphheight+translateY, (int)((x1+x2)/2f), graphheight+translateY+5);
                // box and whiskers
                g.setColor(settings.getClusterColor(clusterName));
                if (boxplot==PLOT_MEDIAN_QUARTILES || boxplot==PLOT_BOTH) graph.drawVerticalBoxAndWhiskers(statistics[STATISTIC_MIN], statistics[STATISTIC_MAX], statistics[STATISTIC_MEDIAN], statistics[STATISTIC_1ST_QUARTILE], statistics[STATISTIC_3RD_QUARTILE], x1,6);
                if (boxplot==PLOT_MEAN_STD || boxplot==PLOT_BOTH) graph.drawVerticalMeanAndStdDeviation(statistics[STATISTIC_AVERAGE], statistics[STATISTIC_STDDEV], x1,6);
                // label on the X-axis
                drawVerticalString(g,clusterName,(int)((x1+x2)/2f),graphheight+translateY+8);
                offset+=columnWidth;
            }
        }
        // write the image to file
        if (file!=null) {
            OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
            ImageIO.write(image, "png", output);
            output.close(); 
        }
        g.dispose();
        return image;
    }
    
    
    /** Returns a color to use for the GC-bar for single sequence or boxplot for group */
    private Color getColorForSequence(String sequenceName, VisualizationSettings settings) {
        if (sequenceGroups==null) return Color.RED;
        else if (sequenceGroups instanceof SequenceCollection) {
              if (((SequenceCollection)sequenceGroups).contains(sequenceName)) return Color.RED;
              else return Color.BLACK;
        } 
        else if (sequenceGroups instanceof SequencePartition) {
            String clusterName=((SequencePartition)sequenceGroups).getClusterForSequence(sequenceName);
            if (clusterName==null) return Color.BLACK;
            else return settings.getClusterColor(clusterName);     
        } 
        else return Color.BLACK;        
    }
    
    private void drawAlignedString(Graphics2D g, String text, int x, int y, double alignX, double alignY) {
        int stringlength=g.getFontMetrics().stringWidth(text);
        int ascent=g.getFontMetrics().getAscent();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawString(text, (float)(x-stringlength*alignX), (float)(y+ascent*alignY));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }   
    
    private void drawVerticalString(Graphics2D g, String text, int x, int y) {
        //int stringlength=g.getFontMetrics().stringWidth(text);
        int ascent=g.getFontMetrics().getAscent();
        AffineTransform oldtransform=g.getTransform();
        g.rotate(Math.PI/2,x,y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawString(text,  x, y+(int)((double)ascent/2f)-2); //
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.translate(x, y);
        g.setTransform(oldtransform);
    }   
    
    private int findLargestNameSize(boolean showOnlySummary) {
        BufferedImage image=new BufferedImage(10,10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();  
        FontMetrics metrics=g.getFontMetrics();
        int largest=0;
        if (!showOnlySummary) {
            for (String seqName:results.keySet()) {
                int width=metrics.stringWidth(seqName);
                if (width>largest) largest=width;
            }
        }
        if (sequenceGroups!=null && sequenceGroups instanceof SequenceCollection) {          
            int width=metrics.stringWidth(((SequenceCollection)sequenceGroups).getName());
            if (width>largest) largest=width;            
        }
        else if (sequenceGroups!=null && sequenceGroups instanceof SequencePartition) {  
            SequencePartition partition=(SequencePartition)sequenceGroups;
            for (String clusterName:partition.getClusterNames()) {
                int width=metrics.stringWidth(clusterName);
                if (width>largest) largest=width;            
            }
        }
        return largest;
    }
    
    /** Returns an array containing summary statistics of the GC-content in the collection */
    private double[] getStatistics(SequenceCollection collection) {
        double[] values=new double[collection.size()];
        for (int i=0;i<collection.size();i++) {
            String seqName=collection.getSequenceNameByIndex(i);
            values[i]=results.get(seqName);
        }
        double average=StatUtils.mean(values);
        double min=StatUtils.min(values);
        double max=StatUtils.max(values);
        double sumDev=0;
        for (int i=0;i<collection.size();i++) {
            String seqName=collection.getSequenceNameByIndex(i);
            double gc=results.get(seqName);
            sumDev+=(average-gc)*(average-gc);
        }        
        double stddev=(double)Math.sqrt(sumDev/(double)collection.size());
        double[] result=new double[7];
        result[STATISTIC_MIN]=min;
        result[STATISTIC_MAX]=max;
        result[STATISTIC_AVERAGE]=average;
        result[STATISTIC_STDDEV]=stddev;
        result[STATISTIC_MEDIAN]=StatUtils.percentile(values, 50);
        result[STATISTIC_1ST_QUARTILE]=StatUtils.percentile(values, 25);
        result[STATISTIC_3RD_QUARTILE]=StatUtils.percentile(values, 75);
        return result;
    }

    /**
     *
     */
    private class SortOrderComparator implements Comparator<String> {
            private SequencePartition partition=null;
            private boolean sortByGC=false;
            private int direction=1;

            public SortOrderComparator(SequencePartition partition, boolean sortByGC, boolean ascending) {
                this.partition=partition;
                this.sortByGC=sortByGC;
                if (!ascending) direction=-1;
            }

            @Override
            public int compare(String seq1, String seq2) { // these are two sequence names
                if (sortByGC) {
                    double gc1=results.get(seq1);
                    double gc2=results.get(seq2);
                    if (partition==null) return Double.compare(gc1, gc2)*direction;
                    else {
                        String cluster1=partition.getClusterForSequence(seq1);
                        String cluster2=partition.getClusterForSequence(seq2);
                        if (cluster1==null && cluster2==null) return 0;
                        else if (cluster1==null && cluster2!=null) return 1;
                        else if (cluster1!=null && cluster2==null) return -1;
                        else {
                            int res=cluster1.compareTo(cluster2);
                            if (res!=0) return res;
                            else return Double.compare(gc1, gc2)*direction;
                        }
                    }
                } else { // sort by sequence name
                    if (partition==null) return seq1.compareTo(seq2);
                    else {
                        String cluster1=partition.getClusterForSequence(seq1);
                        String cluster2=partition.getClusterForSequence(seq2);
                        if (cluster1==null && cluster2==null) return 0;
                        else if (cluster1==null && cluster2!=null) return 1;
                        else if (cluster1!=null && cluster2==null) return -1;
                        else {
                            int res=cluster1.compareTo(cluster2);
                            if (res!=0) return res;
                            else return seq1.compareTo(seq2);
                        }
                    }
                }
            }
    }


    @Override
    protected JPanel getDisplayPanel(MotifLabGUI gui, boolean modal) {
        String[] columnNames=new String[]{"Group","Size","Min","Max","Average","Std. dev.","Median","1st Quartile","3rd Quartile"};
        Object[][] summarydata=null;      
        if (sequenceGroups instanceof SequenceCollection) {
            double[] values=getStatistics((SequenceCollection)sequenceGroups);
            summarydata=new Object[][]{
                new Object[]{
                    ((SequenceCollection)sequenceGroups).getName(),
                    ((SequenceCollection)sequenceGroups).size(),
                     values[STATISTIC_MIN],
                     values[STATISTIC_MAX],
                     values[STATISTIC_AVERAGE],
                     values[STATISTIC_STDDEV],
                     values[STATISTIC_MEDIAN],
                     values[STATISTIC_1ST_QUARTILE],
                     values[STATISTIC_3RD_QUARTILE]
                }
            };
        } else if (sequenceGroups instanceof SequencePartition) {
            SequencePartition partition=(SequencePartition)sequenceGroups;
            summarydata=new Object[partition.getNumberOfClusters()][];
            int i=0;
            for (String clusterName:partition.getClusterNames()) {
                double[] values=getStatistics(partition.getClusterAsSequenceCollection(clusterName, gui.getEngine()));
                summarydata[i]= new Object[]{
                     clusterName,
                     partition.getClusterSize(clusterName),
                     values[STATISTIC_MIN],
                     values[STATISTIC_MAX],
                     values[STATISTIC_AVERAGE],
                     values[STATISTIC_STDDEV],
                     values[STATISTIC_MEDIAN],
                     values[STATISTIC_1ST_QUARTILE],
                     values[STATISTIC_3RD_QUARTILE]
                };
                i++;
            }
        }   
              
        DefaultTableModel summarymodel=new DefaultTableModel(summarydata, columnNames);          
        JTable summaryTable=new JTable(summarymodel) {
            @Override
            public Class<?> getColumnClass(int column) {
               switch (column) {
                   case 0: return String.class;
                   case 1: return Integer.class;
                   default: return Double.class;
               }
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }          
        };
        summaryTable.setAutoCreateRowSorter(true);
        summaryTable.getTableHeader().setReorderingAllowed(false);
        summaryTable.setCellSelectionEnabled(true);
        CellRenderer_Percentage percentage_renderer=new CellRenderer_Percentage();
        summaryTable.setDefaultRenderer(Double.class, percentage_renderer);
        summaryTable.getColumn("Group").setCellRenderer(new CellRenderer_GroupLabel(gui.getVisualizationSettings()));
        summaryTable.getColumn("Size").setPreferredWidth(40);
        summaryTable.getColumn("Min").setPreferredWidth(50);
        summaryTable.getColumn("Max").setPreferredWidth(50);
        summaryTable.getColumn("Average").setPreferredWidth(60);
        summaryTable.getColumn("Std. dev.").setPreferredWidth(60);
        summaryTable.getColumn("Median").setPreferredWidth(60);        
        summaryTable.getColumn("1st Quartile").setPreferredWidth(80);
        summaryTable.getColumn("3rd Quartile").setPreferredWidth(80);        
        JScrollPane summaryTablescrollPane=new JScrollPane();
        if (summarydata!=null) summaryTablescrollPane.setViewportView(summaryTable);
        
        ArrayList<String> sequenceNames=new ArrayList<String>(results.keySet());
        boolean sequencesOK=true;
        MotifLabEngine engine=gui.getEngine();
        for (int i=0;i<sequenceNames.size();i++) {
            Data item=engine.getDataItem(sequenceNames.get(i));
            if (!(item instanceof Sequence)) {sequencesOK=false;break;}
        }
        final boolean allOK=sequencesOK;        
        Object[][] data=new Object[sequenceNames.size()][2];
        for (int i=0;i<sequenceNames.size();i++) {
            if (allOK) {
                Data item=engine.getDataItem(sequenceNames.get(i));
                if (item instanceof Sequence) data[i][0]=item;
            } else data[i][0]=sequenceNames.get(i);         
            data[i][1]=results.get(sequenceNames.get(i));
        }
        DefaultTableModel model=new DefaultTableModel(data, new String[]{"Sequence","GC-content"}) {
            @Override
            public Class getColumnClass(int column) {
                switch(column) {
                    case 0: return (allOK)?Sequence.class:String.class;
                    case 1: return Double.class;
                    default: return Object.class;
                }
            }  
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }              
        };
        JPanel sequencepanel=null;
        JTable table=null;
        if (allOK) {
            sequencepanel=new GenericSequenceBrowserPanel(gui, model, false, modal);
            table=((GenericSequenceBrowserPanel)sequencepanel).getTable();
        } else {
            table=new JTable(model); 
            JScrollPane tablescrollpanel=new JScrollPane(table); 
            sequencepanel=new JPanel(new BorderLayout());
            sequencepanel.add(tablescrollpanel);
        }       
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);        
        table.getColumn("GC-content").setPreferredWidth(90);
        table.getColumn("GC-content").setMaxWidth(90);
        table.getColumn("GC-content").setCellRenderer(percentage_renderer);
        table.getRowSorter().toggleSortOrder(0);
        JPanel topPanel=new JPanel(new BorderLayout());
        GraphPanel graphpanel=new GraphPanel(gui);        
        topPanel.add(new HeaderPanel(graphpanel),BorderLayout.NORTH);
        JScrollPane graphscrollpane=new JScrollPane(graphpanel);
        topPanel.add(graphscrollpane,BorderLayout.CENTER);
        JSplitPane splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,summaryTablescrollPane, sequencepanel);
        splitPane2.setDividerLocation(530);
        splitPane2.setOneTouchExpandable(true);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, splitPane2);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(450);
        JPanel displayPanel=new JPanel(new BorderLayout());        
        displayPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8,5,0,5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)
        ));        
        displayPanel.add(splitPane);
        displayPanel.setPreferredSize(new Dimension(800,600));
        return displayPanel;
    }

    private class HeaderPanel extends JPanel {
        private Font headerFont;
        private JCheckBox groupCheckBox;
        private JComboBox orderCombobox;
        private JComboBox boxandwhiskersCombobox;
        private GraphPanel graphpanel;
        
        public HeaderPanel(GraphPanel graphpanel) {
            this.graphpanel=graphpanel;
            initComponents();
        }
        
        private void initComponents() {
            this.setLayout(new BorderLayout());
            headerFont=new Font(Font.SANS_SERIF, Font.BOLD, 18);
            this.setMinimumSize(null);
            JLabel headerlabel=new JLabel("  GC-content");
            headerlabel.setFont(headerFont);
            this.add(headerlabel,BorderLayout.WEST);
            JPanel controlsPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
            groupCheckBox=new JCheckBox("    Group by cluster");
            groupCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
            orderCombobox=new JComboBox(new String[]{"Sequence name","GC ascending", "GC descending"});
            boxandwhiskersCombobox=new JComboBox(new String[]{MEDIAN_QUARTILES,MEAN_STD,BOTH});
            boxandwhiskersCombobox.setSelectedItem(MEAN_STD);
            controlsPanel.add(new javax.swing.JLabel("Order by  "));
            controlsPanel.add(orderCombobox);
            if (sequenceGroups!=null) {
                controlsPanel.add(new javax.swing.JLabel("  Box plot "));
                controlsPanel.add(boxandwhiskersCombobox);
            }
            if (sequenceGroups instanceof SequencePartition) {
                controlsPanel.add(groupCheckBox);
                groupCheckBox.setSelected(true);
            }
            this.add(controlsPanel,BorderLayout.EAST);
            updateGraphImage();
            ActionListener listener=new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateGraphImage();
                }
            };
            groupCheckBox.addActionListener(listener);
            orderCombobox.addActionListener(listener);
            boxandwhiskersCombobox.addActionListener(listener);                            
        }
        private void updateGraphImage() {
            String order=(String)orderCombobox.getSelectedItem();
            String boxAndWhiskers=(String)boxandwhiskersCombobox.getSelectedItem();
            boolean grouping=groupCheckBox.isSelected();  
            graphpanel.updateGraphImage(order, boxAndWhiskers, grouping);
        }
    }
    
    private class GraphPanel extends JPanel {
        private BufferedImage image;
        private MotifLabGUI gui;

        public GraphPanel(MotifLabGUI gui) {
            super();
            this.gui=gui;      
            this.setBackground(Color.WHITE);           
            this.setOpaque(true);
        }

        public final void updateGraphImage(String order, String boxAndWhiskers, boolean grouping) {
            int sortOrder=SORT_BY_NAME;
            int boxplot=PLOT_MEAN_STD;
            // showOnlySummary below could be based on a checkbox!
            boolean showOnlySummary=(sequenceGroups!=null && results.size()>70); // if there are more than 70 sequences and groups have been specified. Show only the summary
                 if (order.equals("GC ascending")) sortOrder=SORT_BY_GC_ASCENDING;
            else if (order.equals("GC descending")) sortOrder=SORT_BY_GC_DESCENDING;
                 if (boxAndWhiskers.equals(MEAN_STD)) boxplot=PLOT_MEAN_STD;
            else if (boxAndWhiskers.equals(MEDIAN_QUARTILES)) boxplot=PLOT_MEDIAN_QUARTILES;
            else if (boxAndWhiskers.equals(BOTH)) boxplot=PLOT_BOTH;
            try {image=createGraphImage(null, gui.getEngine(), gui.getVisualizationSettings(),sortOrder,grouping, showOnlySummary, boxplot, 1.0);} catch (Exception e) {}
            if (image!=null) {
                int width=image.getWidth()+30;    if (width<550) width=550;
                int height=image.getHeight()+22;  if (height<300) height=300;
                this.setPreferredSize(new Dimension(width,height));
            }
            repaint();
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

private class CellRenderer_Percentage extends DefaultTableCellRenderer {
    public CellRenderer_Percentage() {
           super();
           this.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
    }
    @Override
    public void setValue(Object value) {
           if (value!=null && value instanceof Double) {
               double percentage=((Double)value).doubleValue()*100.0;
               setText(decimalformatter.format(percentage)+"%");
           } else if (value!=null) {
               setText(""+value.toString());
           }
       }
}

private class CellRenderer_GroupLabel extends DefaultTableCellRenderer {
    VisualizationSettings settings;
    public CellRenderer_GroupLabel(VisualizationSettings settings) {
           super();
           this.settings=settings;
           this.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
    }
    @Override
    public void setValue(Object value) {
           super.setValue(value);
           if (value!=null && value instanceof String) {
             Color clusterColor=settings.getClusterColor((String)value);// .brighter(); 
             this.setBackground(clusterColor);
             if (VisualizationSettings.isDark(clusterColor)) this.setForeground(Color.WHITE);
             else this.setForeground(Color.BLACK);
           }
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
