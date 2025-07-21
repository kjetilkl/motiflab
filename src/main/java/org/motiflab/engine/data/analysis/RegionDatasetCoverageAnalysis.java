/*
 
 
 */

package org.motiflab.engine.data.analysis;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.Graph;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.TaskRunner;
import static org.motiflab.engine.data.analysis.Analysis.EXCEL;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.util.Excel_BarChartMaker;
import org.motiflab.engine.util.Excel_BoxplotMaker;
import org.motiflab.gui.GenericSequenceBrowserPanel;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.VisualizationSettings;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceGroup;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.data.SequencePartition;

/**
 *
 * @author kjetikl
 */
public final class RegionDatasetCoverageAnalysis extends Analysis {
    private final static String typedescription="Analysis: region dataset coverage"; 
    private final static String analysisName="region dataset coverage"; 
    private final static String description="Calculates the fraction of each sequence which is covered by a given Region Dataset as well as distribution statistics for sequence collections or clusters";        
 
    private final static int SORT_BY_NAME=0;
    private final static int SORT_BY_COVERAGE_ASCENDING=1;
    private final static int SORT_BY_COVERAGE_DESCENDING=2;
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
    private final String[] variables = new String[]{"coverage"};

    private HashMap<String,Double> results=new HashMap<String,Double>(); // stores sequence names and coverage-percentage
    private SequenceGroup sequenceGroups;
    private String regionDatasetName=null;

    public RegionDatasetCoverageAnalysis() {
        this.name="coverage_temp";
        addParameter("Region dataset",RegionDataset.class, null,new Class[]{RegionDataset.class},"The Region Dataset to estimate coverage for",true,false);
        addParameter("Groups",SequenceGroup.class, null,new Class[]{SequenceCollection.class,SequencePartition.class},"If provided, the analysis will estimate average coverage for all sequences in the collection or for individual clusters in a partition",false,false);
    }  
    
    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Region dataset"};} 
    
    /** Returns a list of output parameters that can be set when an Analysis is output */
    @Override
    public Parameter[] getOutputParameters(String dataformat) {
        Parameter sortPar = new Parameter("Sort by",String.class,"Sequence name",new String[]{"Sequence name","Coverage ascending","Coverage descending"},"The property to sort sequences by",false,false);
        Parameter groupPar = new Parameter("Group by clusters",Boolean.class,Boolean.TRUE,new Boolean[]{Boolean.FALSE,Boolean.TRUE},"Group sequences in partition-clusters together (affects sorting order)",false,false);
        Parameter summaryPar = new Parameter("Show only groups summary",Boolean.class,Boolean.FALSE,new Boolean[]{Boolean.FALSE,Boolean.TRUE},"Show only the summary statistics for each group and not coverage for individual sequences",false,false);
        Parameter boxplotPar = new Parameter("Box plot",String.class, BOTH,new String[]{MEDIAN_QUARTILES,MEAN_STD,BOTH},"Which statistics to show using box plots",false,false);
        Parameter scalePar = new Parameter("Graph scale",Integer.class,100,new Integer[]{10,2000},"Scale of graphics plot (in percent)",false,false);
        if (dataformat.equals(HTML)) return new Parameter[]{sortPar,groupPar,summaryPar,boxplotPar,scalePar};
        else if (dataformat.equals(EXCEL) || dataformat.equals(RAWDATA)) return new Parameter[]{sortPar,groupPar,summaryPar};
        else return new Parameter[0];
    }
    
//    @Override
//    public String[] getOutputParameterFilter(String parameter) {
//        if (parameter.equals("Box plot") || parameter.equals("Graph scale")) return new String[]{"HTML"};
//        if (parameter.equals("Group by clusters") || parameter.equals("Sort by") || parameter.equals("Show only groups summary")) return new String[]{"HTML","RawData","Excel"};        
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
        if (variablename!=null && variablename.equals("coverage")) {
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
       else if (variablename.equals("coverage")) return SequenceNumericMap.class;
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
        if (column.equals("coverage")) return Double.class;
        else return null;
    }

    @Override
    public HashMap<String,Object> getColumnData(String column) throws ExecutionError {
        Class coltype=getColumnType(column);
        if (coltype==null) throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
        HashMap<String,Object> columnData=new HashMap<String, Object>();
        for (String motifname:results.keySet()) {
            Double coverage=results.get(motifname);
            if (column.equalsIgnoreCase("coverage")) columnData.put(motifname, coverage);
         }
        return columnData;
    }

    @Override
    @SuppressWarnings("unchecked")    
    public RegionDatasetCoverageAnalysis clone() {
        RegionDatasetCoverageAnalysis newanalysis=new RegionDatasetCoverageAnalysis();
        super.cloneCommonSettings(newanalysis);    
        newanalysis.regionDatasetName=this.regionDatasetName;
        newanalysis.results=(HashMap<String,Double>)this.results.clone();
        if (sequenceGroups instanceof SequenceCollection) newanalysis.sequenceGroups=(SequenceCollection)((SequenceCollection)this.sequenceGroups).clone();
        else if (sequenceGroups instanceof SequencePartition) newanalysis.sequenceGroups=(SequencePartition)((SequencePartition)this.sequenceGroups).clone();
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        RegionDatasetCoverageAnalysis datasource=((RegionDatasetCoverageAnalysis)source);
        this.regionDatasetName=datasource.regionDatasetName;
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
             Parameter[] defaults=getOutputParameters(format);
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
        if (sortOrderString.equals("Coverage ascending")) sortBy=SORT_BY_COVERAGE_ASCENDING;
        else if (sortOrderString.equals("Coverage descending")) sortBy=SORT_BY_COVERAGE_DESCENDING;

        ArrayList<String> sequenceNames=new ArrayList<String>(results.keySet());
        engine.createHTMLheader(regionDatasetName+" Coverage Analysis", null, null, true, true, true, outputobject);
        if (sequenceNames.size()<=60) outputobject.append("<center>",HTML); // center header if all the data 'fits on the screen'
        outputobject.append("<h1 class=\"headline\">"+regionDatasetName+" Coverage Analysis</h1>\n<br>\n",HTML);
        if (showOnlySummary && sequenceGroups==null) {
            outputobject.append("<br /><br />No sequence groups selected<br />", HTML);
        } else {
            formatCoverageGraph(outputobject, engine, vizSettings, sortBy, groupByCluster, showOnlySummary, plots, scale);
            outputobject.append("<br /><br />", HTML);
        }
        if (sequenceGroups!=null) formatGroupsTable(outputobject, engine, vizSettings);
        outputobject.append("<br /><br />", HTML);
        if (sequenceNames.size()<=60) outputobject.append("</center>",HTML); //        
        boolean ascending=true;
        boolean sortByCoverage=true;
        if (sortBy==SORT_BY_COVERAGE_DESCENDING) ascending=false;
        if (sortBy==SORT_BY_NAME) sortByCoverage=false;
        SortOrderComparator sortOrderComparator;
        if (sequenceGroups instanceof SequencePartition && groupByCluster) sortOrderComparator=new SortOrderComparator((SequencePartition)sequenceGroups, sortByCoverage, ascending);
        else sortOrderComparator=new SortOrderComparator(null, sortByCoverage, ascending);
        if (!showOnlySummary) {
            Collections.sort(sequenceNames,sortOrderComparator);
            outputobject.append("<table class=\"sortable\">\n", HTML);
            outputobject.append("<tr><th>Sequence</th><th>Coverage %</th></tr>\n",HTML);
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

    /** Formats graph and table with min/max/average of groups */
    private void formatCoverageGraph(OutputData outputobject, MotifLabEngine engine, VisualizationSettings vizSettings, int sortBy, boolean grouping, boolean showOnlySummary, int plots, double scale) {
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
                    Color clusterColor=getClusterColor(clusterName, vizSettings).brighter();
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
             Parameter[] defaults=getOutputParameters(format);
             sortOrderString=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             groupByCluster=(Boolean)settings.getResolvedParameter("Group by clusters",defaults,engine);
             showOnlySummary=(Boolean)settings.getResolvedParameter("Show only groups summary",defaults,engine);
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        int sortBy=SORT_BY_NAME;
        if (sortOrderString.equals("Coverage ascending")) sortBy=SORT_BY_COVERAGE_ASCENDING;
        else if (sortOrderString.equals("Coverage descending")) sortBy=SORT_BY_COVERAGE_DESCENDING;
        boolean ascending=true;
        boolean sortByCoverage=true;
        if (sortBy==SORT_BY_COVERAGE_DESCENDING) ascending=false;
        if (sortBy==SORT_BY_NAME) sortByCoverage=false;
        SortOrderComparator sortOrderComparator;
        if (sequenceGroups instanceof SequencePartition && groupByCluster) sortOrderComparator=new SortOrderComparator((SequencePartition)sequenceGroups, sortByCoverage, ascending);
        else sortOrderComparator=new SortOrderComparator(null, sortByCoverage, ascending);
        ArrayList<String> sequenceNames=new ArrayList<String>(results.keySet());
        Collections.sort(sequenceNames,sortOrderComparator);
        outputobject.append("# "+regionDatasetName+" Coverage Analysis\n#\n",RAWDATA);        
        formatGroupsTableRaw(outputobject, engine);
        if (!showOnlySummary) {
            outputobject.append("# Coverage in individual sequences\n",RAWDATA); 
            for (String sequenceName:sequenceNames) {
                Double value=results.get(sequenceName);
                outputobject.append(sequenceName+"\t"+value+"\n",RAWDATA);
            }
        }
        if (format!=null) format.setProgress(100);
        return outputobject;
    }

    
    @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        boolean groupByCluster=false;
        boolean showOnlySummary=false;
        String sortOrderString="Sequence name";
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             sortOrderString=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             groupByCluster=(Boolean)settings.getResolvedParameter("Group by clusters",defaults,engine);
             showOnlySummary=(Boolean)settings.getResolvedParameter("Show only groups summary",defaults,engine);
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }    
        boolean sortByCoverage=false;
        boolean sortAscending=true;
        if (sortOrderString.equals("Coverage ascending")) { sortByCoverage=true; sortAscending=true;}
        else if (sortOrderString.equals("Coverage descending")) { sortByCoverage=true; sortAscending=false;}

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet summarySheet = workbook.createSheet("Summary");
        CellStyle tableheaderStyle=getExcelTableHeaderStyle(workbook);
        CellStyle percentageStyle=getExcelPercentageStyle(workbook);
        
        int groupCount=(sequenceGroups instanceof SequencePartition)?((SequencePartition)sequenceGroups).getNumberOfClusters():1;
        if (!showOnlySummary) {
            Color color = vizSettings.getForeGroundColor(regionDatasetName);
            SequencePartition clusters = (sequenceGroups instanceof SequencePartition)?((SequencePartition)sequenceGroups):null;
            SequencePartition grouping = (clusters!=null && groupByCluster)?((SequencePartition)sequenceGroups):null;
            SortOrderComparator sortOrderComparator = new SortOrderComparator(clusters, sortByCoverage, sortAscending);
            ArrayList<String> sequenceNames;        
            XSSFSheet[] coverageSheet;
            if (sequenceGroups instanceof SequenceCollection) {
                coverageSheet=new XSSFSheet[1];
                coverageSheet[0]=workbook.createSheet(((SequenceCollection)sequenceGroups).getName());
                sequenceNames=((SequenceCollection)sequenceGroups).getAllSequenceNames();
                Collections.sort(sequenceNames,sortOrderComparator);
                outputCoverageColumns(coverageSheet[0],sequenceNames,tableheaderStyle,percentageStyle);
                drawCoveragePlot(coverageSheet[0],0,1,sequenceNames.size(), color);
            } else if (grouping instanceof SequencePartition) { // Do not group by clusters if the user has unchecked this option            
                ArrayList<String> groupNames=((SequencePartition)sequenceGroups).getClusterNames();
                coverageSheet=new XSSFSheet[groupCount];
                for (int i=0;i<groupCount;i++) {
                    String groupName=groupNames.get(i);
                    coverageSheet[i]=workbook.createSheet(groupName);
                    sequenceNames=((SequencePartition)sequenceGroups).getAllSequenceNamesInCluster(groupName);
                    Collections.sort(sequenceNames,sortOrderComparator);                    
                    outputCoverageColumns(coverageSheet[i],sequenceNames,tableheaderStyle,percentageStyle);
                    drawCoveragePlot(coverageSheet[i],0,1,sequenceNames.size(), color);
                }
            } else { // use all sequences as one group
                coverageSheet=new XSSFSheet[1];
                coverageSheet[0]=workbook.createSheet(engine.getDefaultSequenceCollectionName());
                sequenceNames=new ArrayList<>(results.keySet());
                Collections.sort(sequenceNames,sortOrderComparator);
                outputCoverageColumns(coverageSheet[0],sequenceNames,tableheaderStyle,percentageStyle);
                drawCoveragePlot(coverageSheet[0],0,1,sequenceNames.size(), color);
            }
        }
        // Output summary statistics on first sheet
        int rowIndex=0;
        Row row = summarySheet.createRow(rowIndex++);
        Cell cell = row.createCell(0);
        cell.setCellValue(" "); // the title will be added later after columns have been sized
        cell.setCellStyle(getExcelTitleStyle(workbook));
        
        row = summarySheet.createRow(rowIndex++);
        int cellIndex=0;
        for (String header:new String[]{"Group","Size","Min","Max","Average","Std.dev.","Median","1st Quartile","3rd Quartile"}) {
            cell = row.createCell(cellIndex++);
            cell.setCellValue(header);
            cell.setCellStyle(tableheaderStyle);
        }
             
        if (sequenceGroups==null || sequenceGroups instanceof SequenceCollection) {
            SequenceCollection collection=(sequenceGroups instanceof SequenceCollection)?(SequenceCollection)sequenceGroups:null;
            if (collection==null) { // create new collection with all the sequences used in the analysis if no collection was specified
                collection=new SequenceCollection(engine.getDefaultSequenceCollectionName());
                collection.addSequenceNames(new ArrayList<String>(results.keySet()));
            }
            double[] values=getStatistics(collection);
            row = summarySheet.createRow(rowIndex++);
            cell = row.createCell(0); cell.setCellValue(collection.getName());
            cell = row.createCell(1); cell.setCellValue(collection.size());
            cell = row.createCell(2); cell.setCellValue(values[STATISTIC_MIN]); cell.setCellStyle(percentageStyle);
            cell = row.createCell(3); cell.setCellValue(values[STATISTIC_MAX]); cell.setCellStyle(percentageStyle);           
            cell = row.createCell(4); cell.setCellValue(values[STATISTIC_AVERAGE]); cell.setCellStyle(percentageStyle);          
            cell = row.createCell(5); cell.setCellValue(values[STATISTIC_STDDEV]); cell.setCellStyle(percentageStyle);           
            cell = row.createCell(6); cell.setCellValue(values[STATISTIC_MEDIAN]); cell.setCellStyle(percentageStyle);
            cell = row.createCell(7); cell.setCellValue(values[STATISTIC_1ST_QUARTILE]); cell.setCellStyle(percentageStyle);
            cell = row.createCell(8); cell.setCellValue(values[STATISTIC_3RD_QUARTILE]); cell.setCellStyle(percentageStyle);            
         } else if (sequenceGroups instanceof SequencePartition) {
            SequencePartition partition=(SequencePartition)sequenceGroups;
            for (String clusterName:partition.getClusterNames()) {
                double[] values=getStatistics(partition.getClusterAsSequenceCollection(clusterName, engine));
                row = summarySheet.createRow(rowIndex++);
                cell = row.createCell(0); cell.setCellValue(clusterName);
                cell = row.createCell(1); cell.setCellValue(partition.getClusterSize(clusterName));
                cell = row.createCell(2); cell.setCellValue(values[STATISTIC_MIN]); cell.setCellStyle(percentageStyle);
                cell = row.createCell(3); cell.setCellValue(values[STATISTIC_MAX]);  cell.setCellStyle(percentageStyle);          
                cell = row.createCell(4); cell.setCellValue(values[STATISTIC_AVERAGE]); cell.setCellStyle(percentageStyle);          
                cell = row.createCell(5); cell.setCellValue(values[STATISTIC_STDDEV]); cell.setCellStyle(percentageStyle);          
                cell = row.createCell(6); cell.setCellValue(values[STATISTIC_MEDIAN]); cell.setCellStyle(percentageStyle);
                cell = row.createCell(7); cell.setCellValue(values[STATISTIC_1ST_QUARTILE]); cell.setCellStyle(percentageStyle);
                cell = row.createCell(8); cell.setCellValue(values[STATISTIC_3RD_QUARTILE]); cell.setCellStyle(percentageStyle);                
            }
        } 
        for (int c=0;c<=8;c++) {
          summarySheet.autoSizeColumn(c);
          summarySheet.setColumnWidth(c, summarySheet.getColumnWidth(c)+1000); // one unit of width is rather small
        }
        summarySheet.getRow(0).getCell(0).setCellValue("Region dataset coverage analysis"); // add title back after cell resizing
        int firstRow=2; // 2 lines of headers (title + column headers)
        rowIndex--; // this is now the last row
        int restartRow=rowIndex+3;
        row = summarySheet.createRow(restartRow);
        row.createCell(0).setCellValue("In the boxplots below, the blue boxes show the 1st, 2nd (median) and 3rd quartiles, the whiskers show the minimum and maximum values, the yellow diamond shows the average (mean) value and the red lines the standard deviations from the mean");
        // Create boxplots
        XSSFSheet doodleSheet = workbook.createSheet(" ");      
        int chartRow=restartRow+2;
        int chartColumn=0;
        int chartWidth=(int)Math.floor(groupCount*1.0)+1; //
        int chartHeight=(36-chartRow); // there is room for about 36 rows without scrolling, so this will size the chart according to free space
        if (chartHeight<10) chartHeight=20;
        if (chartWidth<5) chartWidth=5;
        Excel_BoxplotMaker boxplot = new Excel_BoxplotMaker(summarySheet, doodleSheet, chartColumn, chartRow, chartWidth, chartHeight, true);
        int offset=2; // two columns before the statistical values
        boxplot.setupDataFromColumns(firstRow,rowIndex, 0,STATISTIC_MIN+offset,STATISTIC_MAX+offset,STATISTIC_MEDIAN+offset,STATISTIC_1ST_QUARTILE+offset,STATISTIC_3RD_QUARTILE+offset);
        boxplot.addLabels("Coverage", "Groups", "Coverage %");
        boxplot.setYrange(0,1.0);       
        boxplot.addExtraPointsFromColumnSum(STATISTIC_AVERAGE+offset, STATISTIC_STDDEV+offset, false, firstRow, rowIndex, "Std+", Excel_BoxplotMaker.PointStyle.DASH, (short)12, Color.RED);  
        boxplot.addExtraPointsFromColumnSum(STATISTIC_AVERAGE+offset, STATISTIC_STDDEV+offset, true, firstRow, rowIndex, "Std-", Excel_BoxplotMaker.PointStyle.DASH, (short)12, Color.RED); 
        boxplot.addExtraPointsFromColumn(STATISTIC_AVERAGE+offset, firstRow, rowIndex, "Mean", Excel_BoxplotMaker.PointStyle.DIAMOND, (short)12, Color.YELLOW);        
        boxplot.addExtraPointsFromColumn(STATISTIC_MIN+offset, firstRow, rowIndex, "Min", Excel_BoxplotMaker.PointStyle.DASH, (short)20, Color.BLACK);
        boxplot.addExtraPointsFromColumn(STATISTIC_MAX+offset, firstRow, rowIndex, "Max", Excel_BoxplotMaker.PointStyle.DASH, (short)20, Color.BLACK);  
        boxplot.drawBoxplot();
        
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
        if (format!=null) format.setProgress(100);
        return outputobject; 
    }

    private void outputCoverageColumns(XSSFSheet sheet, ArrayList<String> sortedSequenceNames, CellStyle tableheaderStyle, CellStyle percentageStyle) {
        Row row = sheet.createRow(0); // header row
        Cell sCell=row.createCell(0);
        sCell.setCellValue("Sequence");
        sCell.setCellStyle(tableheaderStyle);
        Cell vCell=row.createCell(1);
        vCell.setCellValue("Coverage");
        vCell.setCellStyle(tableheaderStyle);
        for (int i = 0; i < sortedSequenceNames.size(); i++) {
            row = sheet.createRow(i+1);
            String sequenceName=sortedSequenceNames.get(i);
            Double value=results.get(sequenceName);          
            row.createCell(0).setCellValue(sequenceName);           
            Cell valueCell=row.createCell(1);
            valueCell.setCellValue(value);
            valueCell.setCellStyle(percentageStyle);                   
        }
        sheet.autoSizeColumn(0);               
        sheet.autoSizeColumn(1);
        sheet.setColumnWidth(0, sheet.getColumnWidth(0)+500);
        sheet.setColumnWidth(1, sheet.getColumnWidth(1)+500);        
    }

    private void drawCoveragePlot(XSSFSheet sheet, int sequenceNamesColumn, int valuesColumn, int rows, Color color) {
        Excel_BarChartMaker barchart = new Excel_BarChartMaker(sheet, valuesColumn+2, 3, (int)(rows/4.0), 20);
        barchart.setupDataFromColumns(sheet, 1, rows+1, sequenceNamesColumn, valuesColumn); // starting at second row ("1") since first row is header
        barchart.addLabels("Coverage","Groups","Coverage");
        barchart.setYrange(0, 1.0);
        barchart.setShowInPercentage(true);        
        barchart.setColors(color, Color.BLACK);
        barchart.drawBarChart();
    }
    
    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        RegionDataset regionDataset=(RegionDataset)task.getParameter("Region dataset");
        this.regionDatasetName=regionDataset.getName();
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
  
        TaskRunner taskRunner=task.getEngine().getTaskRunner();
        task.setProgress(0L,sequenceSize);
        long[] counters=new long[]{0,0,sequenceSize}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences

        ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(sequenceSize);
        for (int i=0;i<sequenceSize;i++) {
            String sequenceName=sequences.get(i).getName();
            RegionSequenceData sequence=(RegionSequenceData)regionDataset.getSequenceByName(sequenceName);
            processTasks.add(new ProcessSequenceTask(sequence, task, counters));
        }
        List<Future<Double>> futures=null;
        int countOK=0;            
        try {
            futures=taskRunner.invokeAll(processTasks); // this call apparently blocks until all tasks finish (either normally or by exceptions or being cancelled)                             
            for (Future<Double> future:futures) {
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
        if (countOK!=sequenceSize) {
            throw new ExecutionError("Some mysterious error occurred while performing analysis: "+getAnalysisName());
        }         
        
    }
       
 
    /** Returns the coverage of the sequence */
//    private double findCoverage_old(RegionSequenceData sequencedata) {
//        int coveredbases=0;
//        int length=sequencedata.getSize();
//        for (int i=0;i<length;i++) {
//            if (sequencedata.getNumberOfRegionsAtRelativePosition(i)>0) coveredbases++;
//        }        
//        return (double)coveredbases/(double)length;
//    } 
    
    /** Returns the coverage of the sequence */
    private double findCoverage(RegionSequenceData sequencedata) { 
        // this implementation is much faster than the one above since the repeated use of the slow method "sequencedata.getNumberOfRegionsAtRelativePosition(i)" does not scale well for sequences with many regions
        // this new method works by flattening consecutive segments of the of sequence into a buffer which is then examined
        int coveredbases=0;
        int buffersize=10000;          
        int sequenceSize=sequencedata.getSize();
        if (sequenceSize<buffersize) buffersize=sequenceSize;
        int[] buffer=new int[buffersize];     
        int bufferstart=0;
        for (int pos=0;pos<sequenceSize;pos++) {
            if (pos%buffersize==0 && buffer!=null) { // flatten new segment
                int bufferend=bufferstart+buffersize-1;
                if (bufferend>sequenceSize-1) bufferend=sequenceSize-1;
                buffer=sequencedata.flattenSegment(buffer, bufferstart, bufferend);
                bufferstart+=buffersize;
            }
            if (buffer[pos%buffersize]>0) coveredbases++;
        }               
        return (double)coveredbases/(double)sequenceSize;
    }     
    
    
 
   protected class ProcessSequenceTask implements Callable<Double> {
        final RegionSequenceData sequence;
        final long[] counters; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final ExecutableTask task;  
        
        public ProcessSequenceTask( RegionSequenceData sequence, ExecutableTask task, long[] counters) {
           this.sequence=sequence;
           this.counters=counters;
           this.task=task;
        }
         
        @Override
        @SuppressWarnings("unchecked")
        public Double call() throws Exception {
            synchronized(counters) {
               counters[0]++; // number of sequences started  
            }        
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            
            double coverage=findCoverage(sequence);      
            
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed            
                results.put(sequence.getSequenceName(), coverage); 
                task.setStatusMessage("Executing analysis: "+getAnalysisName()+"  ("+counters[1]+"/"+counters[2]+")");
                task.setProgress(counters[1],counters[2]);                              
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();    
            return coverage;
        }   
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
        boolean sortByCoverage=true;
        if (sortOrder==SORT_BY_COVERAGE_DESCENDING) ascending=false;
        if (sortOrder==SORT_BY_NAME) sortByCoverage=false;
        if (sequenceGroups instanceof SequencePartition && grouping) sortOrderComparator=new SortOrderComparator((SequencePartition)sequenceGroups, sortByCoverage, ascending);
        else sortOrderComparator=new SortOrderComparator(null, sortByCoverage, ascending);
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
        // draw coverage bar for each sequence
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
                drawVerticalString(g,sequenceName,(int)((x1+x2)/2f),graphheight+translateY+8);
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
                g.setColor(getClusterColor(clusterName,settings));
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

    private HashMap<String,Color> assignedClusterColors;
    private Color getClusterColor(String clusterName, VisualizationSettings settings) {
         if (settings!=null) return settings.getClusterColor(clusterName);
         if (assignedClusterColors==null) assignedClusterColors=new HashMap<String,Color>();
         if (assignedClusterColors.containsKey(clusterName)) return assignedClusterColors.get(clusterName);
         else {
             Color[] colorList=new Color[]{Color.red, Color.green, Color.blue, Color.magenta, Color.cyan, Color.orange, new Color(128,0,0), new Color(0,128,0), new Color(0,0,128), new Color(128,0,128), new Color(0,128,128), new Color(128,128,0), new Color(128,64,0), Color.BLACK};
             int size=assignedClusterColors.size();
             Color newcolor=colorList[size%colorList.length];
             assignedClusterColors.put(clusterName, newcolor);
             return newcolor;
         }
         
    }
    
    private Color getColorForSequence(String sequenceName, VisualizationSettings settings) {
        if (sequenceGroups==null) return Color.RED;
        else if (sequenceGroups instanceof SequenceCollection) {
              if (((SequenceCollection)sequenceGroups).contains(sequenceName)) return Color.RED;
              else return Color.BLACK;
        } 
        else if (sequenceGroups instanceof SequencePartition) {
            String clusterName=((SequencePartition)sequenceGroups).getClusterForSequence(sequenceName);
            if (clusterName==null) return Color.BLACK;
            else return getClusterColor(clusterName,settings);     
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
    
    /** Returns an array containing summary statistics of the coverage in the collection */
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
            double coverage=results.get(seqName);
            sumDev+=(average-coverage)*(average-coverage);
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
            private boolean sortByCoverage=false;
            private int direction=1;

            public SortOrderComparator(SequencePartition partition, boolean sortByCoverage, boolean ascending) {
                this.partition=partition;
                this.sortByCoverage=sortByCoverage;
                if (!ascending) direction=-1;
            }

            @Override
            public int compare(String seq1, String seq2) { // these are two sequence names
                if (sortByCoverage) {
                    Double value1=results.get(seq1);
                    Double value2=results.get(seq2);
                    if (value1==null || Double.isNaN(value1)) value1=Double.MAX_VALUE;
                    if (value2==null || Double.isNaN(value2)) value2=Double.MAX_VALUE;
                    if (partition==null) return Double.compare(value1, value2)*direction;
                    else {
                        String cluster1=partition.getClusterForSequence(seq1);
                        String cluster2=partition.getClusterForSequence(seq2);
                        if (cluster1==null && cluster2==null) return 0;
                        else if (cluster1==null && cluster2!=null) return 1;
                        else if (cluster1!=null && cluster2==null) return -1;
                        else {
                            int res=cluster1.compareTo(cluster2);
                            if (res!=0) return res;
                            else return Double.compare(value1, value2)*direction;
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
        DefaultTableModel model=new DefaultTableModel(data, new String[]{"Sequence","Coverage"}) {
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
        table.getColumn("Coverage").setPreferredWidth(90);
        table.getColumn("Coverage").setMaxWidth(90);
        table.getColumn("Coverage").setCellRenderer(percentage_renderer);
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
            javax.swing.JLabel headerlabel=new javax.swing.JLabel("  "+regionDatasetName+" coverage");
            headerlabel.setFont(headerFont);
            this.add(headerlabel,BorderLayout.WEST);
            JPanel controlsPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
            groupCheckBox=new JCheckBox("    Group by cluster");
            groupCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
            orderCombobox=new JComboBox(new String[]{"Sequence name","Coverage ascending", "Coverage descending"});
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
                 if (order.equals("Coverage ascending")) sortOrder=SORT_BY_COVERAGE_ASCENDING;
            else if (order.equals("Coverage descending")) sortOrder=SORT_BY_COVERAGE_DESCENDING;
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
             Color clusterColor=settings.getClusterColor((String)value).brighter(); 
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