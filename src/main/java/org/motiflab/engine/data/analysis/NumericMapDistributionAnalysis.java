/*
 */

package org.motiflab.engine.data.analysis;

import de.erichseifert.vectorgraphics2d.EPSGraphics2D;
import de.erichseifert.vectorgraphics2d.PDFGraphics2D;
import de.erichseifert.vectorgraphics2d.SVGGraphics2D;
import de.erichseifert.vectorgraphics2d.VectorGraphics2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.Graph;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.Parameter;
import static org.motiflab.engine.data.analysis.Analysis.EXCEL;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.util.Excel_BoxplotMaker;
import org.motiflab.gui.OutputPanel;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.VisualizationSettings;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFChartAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.*;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.DataCollection;
import org.motiflab.engine.data.DataGroup;
import org.motiflab.engine.data.DataPartition;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.OutputData;


/**
 *
 * @author kjetikl
 */
public class NumericMapDistributionAnalysis extends Analysis {
    private static String typedescription="Analysis: numeric map distribution";
    private static String analysisName="numeric map distribution";
    private static String description="Calculates distribution statistics for the values in a numeric map";
    private static final String DEFAULT_GROUP="*DEFAULT*";
    private String numericMapName=null;
    private String dataGroupName=null;
    private Class dataGroupClass=null;
    private HashMap<String,double[]> statistics; // the key is a cluster name or collection name (or the special string "*DEFAULT*"). The value is [count,min,max,sum,avg,median,stdev,1st quartile,3rd quartile]
    private HashMap<String,int[]> bins; // the key is a cluster name or collection name (or the special string "*DEFAULT*"). The value is bin counts
    private ArrayList<String> clusterNames=null; // names of clusters if the values are grouped by a Partition. Name of Collection if grouped by Collection. else NULL
    private ArrayList<Color> clusterColors=null;
    private boolean doNormalize=true;

    private final String[] variables = new String[]{};

    private static final String NONE="None";   
    private static final String MEDIAN_QUARTILES="Median+Quartiles"; 
    private static final String MEAN_STD="Average+StDev"; 
    private static final String BOTH="All";     
    
    private static final int PLOT_NONE=0;
    private static final int PLOT_MEDIAN_QUARTILES=1;
    private static final int PLOT_MEAN_STD=2;
    private static final int PLOT_BOTH=3;
    
    public NumericMapDistributionAnalysis() {
        this.name="NumericMapDistributionAnalysis_temp";
        addParameter("Numeric Map",NumericMap.class, null,new Class[]{NumericMap.class},"The numeric map to calculate distribution statistics for",true,false);
        addParameter("Groups",DataGroup.class, null,new Class[]{DataGroup.class},"Consider only values from a Collection or do separate calculations for each clusters in a Partition",false,false);
        addParameter("Normalize",Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Normalizes the histogram of each group independently of each other (this will only affect the graph, not the statistics)",false,false);
        addParameter("Bins",Integer.class, new Integer(100),new Integer[]{1,1000},"Number of bins to partition the numeric values into",false,false);
    }

    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Numeric Map"};}     
    
    /** Returns a list of output parameters that can be set when an Analysis is output */
    @Override
    public Parameter[] getOutputParameters(String dataformat) {        
         Parameter boxplotpar=new Parameter("Box plot",String.class, BOTH,new String[]{NONE,MEDIAN_QUARTILES,MEAN_STD,BOTH},"Which statistics to show using box plots",false,false);        
         Parameter imageformat=new Parameter("Image format",String.class, "png",new String[]{"png","svg","pdf","eps"},"The image format to use for the graph",false,false);        
         Parameter scalepar=new Parameter("Graph scale",Integer.class,100,new Integer[]{10,2000},"Scale of graphics plot (in percent)",false,false);
         if (dataformat.equals(HTML)) return new Parameter[]{boxplotpar,imageformat,scalepar};
         else return new Parameter[0];
    }

//    @Override
//    public String[] getOutputParameterFilter(String parameter) {
//        if (parameter.equals("Box plot") || parameter.equals("Image format") || parameter.equals("Graph scale")) return new String[]{"HTML"};
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
        if (clusterNames==null) return new String[]{"min","max","median","first quartile","third quartile","average","standard deviation","size","sum"};
        else {
            int clusters=clusterNames.size();
            int total=clusters*9; // 9 statistics per clusters
            String[] allResults=new String[total];
            for (int j=0;j<clusters;j++) {
                String cluster=clusterNames.get(j);
                allResults[j+0]=cluster+":min";
                allResults[j+1]=cluster+":max";
                allResults[j+2]=cluster+":median";
                allResults[j+3]=cluster+":first quartile";
                allResults[j+4]=cluster+":third quartile";
                allResults[j+5]=cluster+":average";
                allResults[j+6]=cluster+":standard deviation";
                allResults[j+7]=cluster+":size";
                allResults[j+8]=cluster+":sum";                
            }
            return allResults;
        }
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        double value=Double.NaN;
        String key=DEFAULT_GROUP;  
        String statisticname=variablename;
        if (variablename.contains(":")) {
            String[] parts=variablename.split(":");
            if (parts.length==2) {key=parts[0];statisticname=parts[1];}
            else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
        } 
        if (!statistics.containsKey(key)) throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
        double[] statistic=statistics.get(key);
             if (statisticname.equalsIgnoreCase("min")) value=statistic[1];
        else if (statisticname.equalsIgnoreCase("max")) value=statistic[2];
        else if (statisticname.equalsIgnoreCase("median")) value=statistic[5];
        else if (statisticname.equalsIgnoreCase("first quartile")) value=statistic[7];
        else if (statisticname.equalsIgnoreCase("third quartile")) value=statistic[8];
        else if (statisticname.equalsIgnoreCase("average")) value=statistic[4];
        else if (statisticname.equalsIgnoreCase("standard deviation")) value=statistic[6];
        else if (statisticname.equalsIgnoreCase("size")) value=statistic[0];
        else if (statisticname.equalsIgnoreCase("sum")) value=statistic[3];
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
        return new NumericVariable("result", value);        
    }

    @Override
    public Class getResultType(String variablename) {
       if (hasResult(variablename)) return NumericVariable.class; // all exported values in this analysis are numerical
       else return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NumericMapDistributionAnalysis clone() {
        NumericMapDistributionAnalysis newanalysis=new NumericMapDistributionAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.statistics=cloneMap(this.statistics);
        newanalysis.bins=cloneIntMap(this.bins);
        newanalysis.dataGroupClass=this.dataGroupClass;
        newanalysis.dataGroupName=this.dataGroupName;
        newanalysis.doNormalize=this.doNormalize;
        if (this.clusterNames==null) newanalysis.clusterNames=null;
        else newanalysis.clusterNames=(ArrayList<String>)this.clusterNames.clone();
        if (this.clusterColors==null) newanalysis.clusterColors=null;
        else newanalysis.clusterColors=(ArrayList<Color>)this.clusterColors.clone();
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        NumericMapDistributionAnalysis other=(NumericMapDistributionAnalysis)source;
        this.numericMapName=other.numericMapName;
        this.dataGroupName=other.dataGroupName;
        this.dataGroupClass=other.dataGroupClass;
        this.doNormalize=other.doNormalize;
        this.statistics=cloneMap(other.statistics);
        this.bins=cloneIntMap(other.bins);
        this.clusterNames=(other.clusterNames==null)?null:(ArrayList<String>)other.clusterNames.clone();
        if (other.clusterColors==null) this.clusterColors=null;
        else this.clusterColors=(ArrayList<Color>)other.clusterColors.clone();
    }

    private HashMap<String,double[]> cloneMap(HashMap<String,double[]> map) {
        HashMap<String,double[]> result=new HashMap<String,double[]>(map.size());
        for (String key:map.keySet()) {
            double[] array=map.get(key);
            result.put(key,Arrays.copyOf(array, array.length));
        }
        return result;
    }
    private HashMap<String,int[]> cloneIntMap(HashMap<String,int[]> map) {
        HashMap<String,int[]> result=new HashMap<String,int[]>(map.size());
        for (String key:map.keySet()) {
            int[] array=map.get(key);
            result.put(key,Arrays.copyOf(array, array.length));
        }
        return result;
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
        engine.createHTMLheader("Numeric Map Distribution Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<div align=\"center\">\n",HTML);
             if (dataGroupClass==null) outputobject.append("<h2 class=\"headline\">Distribution of values for '"+numericMapName+"'</h2>\n",HTML);
        else if (DataCollection.class.isAssignableFrom(dataGroupClass)) outputobject.append("<h2 class=\"headline\">Distribution of values for '"+numericMapName+"' in collection '"+dataGroupName+"'</h2>\n",HTML);
        else if (DataPartition.class.isAssignableFrom(dataGroupClass)) outputobject.append("<h2 class=\"headline\">Distribution of values for '"+numericMapName+"' for clusters in '"+dataGroupName+"'</h2>\n",HTML);
        else outputobject.append("<h2 class=\"headline\">A problem of type #204 has occurred<h2>\n"+dataGroupClass.toString(),HTML);
        if (dataGroupClass!=null && DataPartition.class.isAssignableFrom(dataGroupClass) && clusterNames.isEmpty()) {
            outputobject.append("<br /><br />... "+dataGroupName+" contains no clusters ...<br /><br />",HTML);
        } else {
            int plots=PLOT_BOTH;
            int scalepercent=100;
            String imageFormat="png";
            if (settings!=null) {
                try {
                    Parameter[] defaults=getOutputParameters(format);
                    String plotString=(String)settings.getResolvedParameter("Box plot",defaults,engine);
                        if (plotString.equals(NONE)) plots=PLOT_NONE;
                    else if (plotString.equals(MEDIAN_QUARTILES)) plots=PLOT_MEDIAN_QUARTILES;
                    else if (plotString.equals(MEAN_STD)) plots=PLOT_MEAN_STD;
                    else if (plotString.equals(BOTH)) plots=PLOT_BOTH;
                    scalepercent=(Integer)settings.getResolvedParameter("Graph scale",defaults,engine);
                    imageFormat=(String)settings.getResolvedParameter("Image format",defaults,engine);
                } 
                catch (ExecutionError e) {throw e;} 
                catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
            } 
            double scale=(scalepercent==100)?1.0:(((double)scalepercent)/100.0);
            if (clusterColors==null) clusterColors=assignClusterColors(clusterNames, engine.getClient().getVisualizationSettings()); // assign new colors
            File imagefile=outputobject.createDependentFile(engine,imageFormat);            
            Dimension dim=null;
            try {
                dim=saveGraphAsImage(imagefile,plots,scale, engine.getClient().getVisualizationSettings());
            } catch (IOException e) {
                engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
            }
            if (format!=null) format.setProgress(50);
            outputobject.append("<br /><br />\n<table class=\"sortable\">\n",HTML);
            if (clusterNames!=null && !clusterNames.isEmpty()) {
                outputobject.append("<tr><th width=\"90\"> </th><th width=\"90\">Size</th><th width=\"90\">Min</th><th width=\"90\">Max</th><th width=\"90\">Median</th><th width=\"90\">Average</th><th width=\"90\"><nobr>Std.dev.</nobr></th><th width=\"90\">1st&nbsp;Quartile</th><th width=\"90\">3rd&nbsp;Quartile</th></tr>\n",HTML);
                for (int i=0;i<clusterNames.size();i++) {
                   String clusterName=clusterNames.get(i);
                   Color clusterColor=clusterColors.get(i);
                   double[] clusterResults=statistics.get(clusterName);
                   String colorString=VisualizationSettings.convertColorToHTMLrepresentation(clusterColor.brighter());
                   String clusterNameString;
                   if (getBrightness(clusterColor)<130) clusterNameString="<span style=\"color:#FFFFFF\">"+clusterName+"</span>";
                   else clusterNameString=clusterName;
                   outputobject.append("<tr><td style=\"background-color:"+colorString+";text-align:left;padding-left:10px;padding-right:10px\">"+clusterNameString+"</td><td class=\"num\">"+((int)clusterResults[0])+"</td><td class=\"num\">"+formatNumber(clusterResults[1])+"</td><td class=\"num\">"+formatNumber(clusterResults[2])+"</td><td class=\"num\">"+formatNumber(clusterResults[5])+"</td><td class=\"num\">"+formatNumber(clusterResults[4])+"</td><td class=\"num\">"+formatNumber(clusterResults[6])+"</td><td class=\"num\">"+formatNumber(clusterResults[7])+"</td><td class=\"num\">"+formatNumber(clusterResults[8])+"</td></tr>\n",HTML);
                }
            } else { // single group
                 double[] clusterResults=statistics.get(DEFAULT_GROUP);  
                 outputobject.append("<tr><th width=\"90\">Size</th><th width=\"90\">Min</th><th width=\"90\">Max</th><th width=\"90\">Median</th><th width=\"90\">Average</th><th width=\"90\"><nobr>Std.dev.</nobr></th><th width=\"90\">1st&nbsp;Quartile</th><th width=\"90\">3rd&nbsp;Quartile</th></tr>\n",HTML);
                 outputobject.append("<tr><td class=\"num\">"+((int)clusterResults[0])+"</td><td class=\"num\">"+formatNumber(clusterResults[1])+"</td><td class=\"num\">"+formatNumber(clusterResults[2])+"</td><td class=\"num\">"+formatNumber(clusterResults[5])+"</td><td class=\"num\">"+formatNumber(clusterResults[4])+"</td><td class=\"num\">"+formatNumber(clusterResults[6])+"</td><td class=\"num\">"+formatNumber(clusterResults[7])+"</td><td class=\"num\">"+formatNumber(clusterResults[8])+"</td></tr>\n",HTML);
            }
            outputobject.append("</table>\n",HTML);
            outputobject.append("<br /><br /><br /><br />",HTML);

            if (imageFormat.equals("pdf")) outputobject.append("<object type=\"application/pdf\" data=\"file:///"+imagefile.getAbsolutePath()+"\"></object>",HTML);
            else {
                outputobject.append("<img src=\"file:///"+imagefile.getAbsolutePath()+"\"",HTML);
                if (dim!=null) outputobject.append(" width="+(int)Math.ceil(dim.width*scale)+" height="+(int)Math.ceil(dim.height*scale),HTML);                          
                outputobject.append(" />\n",HTML);
            }
        }
        outputobject.append("</div>\n",HTML);
        outputobject.append("</body>\n</html>\n",HTML);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }

    private int getBrightness(Color c) {
        return (int) Math.sqrt(
          c.getRed() * c.getRed() * .241 +
          c.getGreen() * c.getGreen() * .691 +
          c.getBlue() * c.getBlue() * .068);
    }

    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        if (clusterNames!=null && !clusterNames.isEmpty()) {
            for (String clusterName:clusterNames) {
                double[] clusterResults=statistics.get(clusterName);
                outputobject.append(clusterName+":size="+((int)clusterResults[0])+"\n",RAWDATA);
                outputobject.append(clusterName+":min="+clusterResults[1]+"\n",RAWDATA);
                outputobject.append(clusterName+":max="+clusterResults[2]+"\n",RAWDATA);
                outputobject.append(clusterName+":median="+clusterResults[5]+"\n",RAWDATA);
                outputobject.append(clusterName+":average="+clusterResults[4]+"\n",RAWDATA);
                outputobject.append(clusterName+":standard deviation="+clusterResults[6]+"\n",RAWDATA);
                outputobject.append(clusterName+":1st quartile="+clusterResults[7]+"\n",RAWDATA);
                outputobject.append(clusterName+":3rd quartile="+clusterResults[8]+"\n",RAWDATA);            
                outputobject.append("\n",RAWDATA);
            }
        } else {
            double[] results=statistics.get(DEFAULT_GROUP);
            outputobject.append("size="+((int)results[0])+"\n",RAWDATA);
            outputobject.append("min="+results[1]+"\n",RAWDATA);
            outputobject.append("max="+results[2]+"\n",RAWDATA);
            outputobject.append("median="+results[5]+"\n",RAWDATA);
            outputobject.append("average="+results[4]+"\n",RAWDATA);
            outputobject.append("standard deviation="+results[6]+"\n",RAWDATA);
            outputobject.append("1st quartile="+results[7]+"\n",RAWDATA);
            outputobject.append("3rd quartile="+results[8]+"\n",RAWDATA);            
            outputobject.append("\n",RAWDATA);            
        }
        if (format!=null) format.setProgress(100);
        return outputobject;
    }

    @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        if (clusterNames==null || clusterNames.size()==1) return formatExcelSingleGroup(outputobject, engine, settings, task, format);
        else return formatExcelMultipleGroups(outputobject, engine, settings, task, format);
    }
    
    private OutputData formatExcelSingleGroup(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        XSSFWorkbook workbook=null;
        try {
            InputStream stream = DistributionAnalysis.class.getResourceAsStream("resources/AnalysisTemplate_NumericMapDistribution.xlsx");
            workbook = (XSSFWorkbook)WorkbookFactory.create(stream);
            stream.close();
            XSSFSheet sheet = workbook.getSheetAt(0);           
            String title = "Distribution of values for numeric map \""+numericMapName+"\"";   
            String group = (clusterNames==null)?DEFAULT_GROUP:clusterNames.get(0); //            
            if (dataGroupName!=null) {
                if (DataCollection.class.isAssignableFrom(dataGroupClass)) title+=" in collection \""+dataGroupName+"\"";
                else if (DataPartition.class.isAssignableFrom(dataGroupClass)) title+=" for cluster \""+group+"\" from partition \""+dataGroupName+"\"";
                
            } 
            formatStatisticExcel(group, workbook.getSheetAt(0), title);   
            sheet.getRow(0).getCell(0).setCellValue(title);
            if (format!=null) format.setProgress(90);          
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new ExecutionError(e.getMessage());
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
    
    private void formatStatisticExcel(String name, XSSFSheet sheet, String title) {
        double[] clusterResults=statistics.get(name);        
        Row row = sheet.getRow(4); // the row index is determined by the Excel template file
        int column=1;
        row.getCell(column++).setCellValue((int)clusterResults[0]); // size         
        row.getCell(column++).setCellValue(clusterResults[1]); // min 
        row.getCell(column++).setCellValue(clusterResults[2]); // max
        row.getCell(column++).setCellValue(clusterResults[4]); // average
        row.getCell(column++).setCellValue(clusterResults[6]); // std dev
        row.getCell(column++).setCellValue(clusterResults[5]); // median
        row.getCell(column++).setCellValue(clusterResults[7]); // 1st quartile
        row.getCell(column++).setCellValue(clusterResults[8]); // 3rd quartile
          
        int datarow = 99;
        int size = (int)clusterResults[0];
        int[] databins = bins.get(name);
        int bincount = databins.length;        
        double minValue = clusterResults[1];
        double maxValue = clusterResults[2];
        double binrange = (maxValue-minValue)/(double)bincount;
        double binstart = minValue;
        
        Cell cell;
        for (int i=0;i<bincount;i++) {
            int rowIndex = datarow+i;
            row = sheet.getRow(rowIndex);
            if (row==null) row=sheet.createRow(rowIndex);
            cell = row.getCell(0);
            if (cell==null) cell=row.createCell(0);
            cell.setCellValue(binstart);
            cell = row.getCell(1);
            if (cell==null) cell=row.createCell(1);
            cell.setCellValue(databins[i]/(double)size);                        
            binstart+=binrange;
        }
        // clear remaining example data from analysis template sheet
        for (int i=datarow+bincount;i<210;i++) {
           row = sheet.getRow(i);
           if (row==null) break;
           cell = row.getCell(0);
           if (cell!=null) cell.setBlank();  
           cell = row.getCell(1);
           if (cell!=null) cell.setBlank();                  
        } 
        forceExcelFormulaRecalculation(sheet.getWorkbook(),sheet);        
        
        XSSFDrawing drawing = ((XSSFSheet)sheet).createDrawingPatriarch();
        XSSFChart histogramChart = drawing.getCharts().get(0);
        List<XDDFChartData> data = histogramChart.getChartSeries();        

        XDDFCategoryDataSource categories = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(datarow, datarow+bincount, 0, 0));
        XDDFNumericalDataSource values = XDDFDataSourcesFactory.fromNumericCellRange(sheet,new CellRangeAddress(datarow, datarow+bincount, 1, 1));       
        XDDFBarChartData.Series series1 = (XDDFBarChartData.Series)data.get(0).getSeries(0); 
        series1.replaceData(categories, values);
        histogramChart.plot(histogramChart.getChartSeries().get(0));
     
        // Define new data ranges
        int datarowPlusOne=datarow+1;
        String sheetname = sheet.getSheetName();
        String newXRange = "'"+sheetname+"'!$A$"+datarowPlusOne+":$A$"+(datarowPlusOne+bincount-1);
        String newValueRange = "'"+sheetname+"'!$B$"+datarowPlusOne+":$B$"+(datarowPlusOne+bincount-1);        
        // Check if the category axis is null
        if (series1.getCTBarSer().getCat() == null) {series1.getCTBarSer().addNewCat();}
        if (series1.getCTBarSer().getCat().getNumRef() != null) {
            series1.getCTBarSer().getCat().getNumRef().setF(newXRange);
        }
        else if (series1.getCTBarSer().getCat().getStrRef()!= null) {
            series1.getCTBarSer().getCat().getStrRef().setF(newXRange);
        }
        else {
            series1.getCTBarSer().getCat().addNewNumRef();
            series1.getCTBarSer().getCat().getNumRef().setF(newXRange);
        }
        series1.getCTBarSer().getVal().getNumRef().setF(newValueRange);       
        
        // Set the minimum and maximum X-values for box-and-whiskers plot
        for (int i=1;i<drawing.getCharts().size();i++) {
            XSSFChart boxchart = drawing.getCharts().get(i);
            XDDFValueAxis xAxis = null;
            for (XDDFChartAxis axis : boxchart.getAxes()) {
                if (axis.getPosition() == AxisPosition.BOTTOM) {
                    xAxis = (XDDFValueAxis) axis;
                    break;
                }
            }
            if (xAxis != null) {                               
                xAxis.setMinimum(minValue); // 
                xAxis.setMaximum(maxValue); //
            }
        }
        // update title
        sheet.getRow(0).getCell(0).setCellValue(title);      
    }    
    
    private OutputData formatExcelMultipleGroups(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet summarySheet = workbook.createSheet("Numeric Map Distribution");
        CellStyle tableheaderStyle=getExcelTableHeaderStyle(workbook);
        CellStyle tableCellStyleInteger=createExcelStyle(workbook, BorderStyle.THIN, null, HorizontalAlignment.CENTER, false);
        CellStyle tableCellStyle=createExcelStyle(workbook, BorderStyle.THIN, null, HorizontalAlignment.CENTER, false);     
        tableCellStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00000"));
        
        // Output summary statistics on first sheet
        int rowIndex=0;
        Row row = summarySheet.createRow(rowIndex++);
        Cell cell = row.createCell(0);
        cell.setCellValue(" "); // the title will be added later after columns have been sized
        cell.setCellStyle(getExcelTitleStyle(workbook));
        rowIndex++;
        row = summarySheet.createRow(rowIndex++);
        int cellIndex=0;
        for (String header:new String[]{"Group","Size","Min","Max","Average","Std.dev.","Median","1st Quartile","3rd Quartile"}) {
            cell = row.createCell(cellIndex++);
            cell.setCellValue(header);
            cell.setCellStyle(tableheaderStyle);
        }
        int firstRow = rowIndex;
        int groupCount = clusterNames.size();
        double globalMin = Double.MAX_VALUE;
        double globalMax = -Double.MAX_VALUE;       
        for (int i=0;i<groupCount;i++) {
            String clusterName = clusterNames.get(i);
            Color clusterColor = clusterColors.get(i);
            double[] values = statistics.get(clusterName);
            int size = (int)values[0];
            double min = values[1];
            double max = values[2]; 
            double sum = values[3]; // not used
            double average = values[4]; 
            double median = values[5]; 
            double stddev = values[6]; 
            double quart1 = values[7]; 
            double quart3 = values[8]; 
            if (min<globalMin) globalMin = min;
            if (max>globalMax) globalMax = max;
            
            CellStyle groupStyle = createExcelStyle(workbook, BorderStyle.THIN, clusterColor, HorizontalAlignment.LEFT, false); 
            
            row = summarySheet.createRow(rowIndex++);
            outputStringValueInCell(row, 0, clusterName, groupStyle);
            outputNumericValueInCell(row, 1, size, tableCellStyleInteger);  
            outputNumericValuesInCells(row, new double[]{min,max,average,stddev,median,quart1,quart3}, 2, tableCellStyle);  
            
        }
        autoSizeExcelColumns(summarySheet, 0, 8, 1000, 3500);
        String title = "Distribution of values for numeric map \""+numericMapName+"\" for clusters in \""+dataGroupName+"\"";
        summarySheet.getRow(0).getCell(0).setCellValue(title);
        
        rowIndex+=2;
        row = summarySheet.createRow(rowIndex);
        row.createCell(0).setCellValue("In the boxplots below, the blue boxes show the 1st, 2nd (median) and 3rd quartiles, the whiskers show the minimum and maximum values, the yellow diamond shows the average (mean) value and the red lines the standard deviations from the mean");
        
        // Create boxplots
        XSSFSheet doodleSheet = workbook.createSheet(" ");      
        int chartRow=rowIndex+2;
        int chartColumn=0;
        int chartWidth=(int)Math.floor(groupCount*1.0)+1; //
        int chartHeight=(36-chartRow); // there is room for about 36 rows without scrolling, so this will size the chart according to free space
        if (chartHeight<10) chartHeight=20;
        if (chartWidth<5) chartWidth=5;
        int lastRow = firstRow + groupCount - 1;
        Excel_BoxplotMaker boxplot = new Excel_BoxplotMaker(summarySheet, doodleSheet, chartColumn, chartRow, chartWidth, chartHeight, false);
        boxplot.setupDataFromColumns(firstRow, lastRow, 0, 2, 3, 6, 7, 8);
        boxplot.setYrange(globalMin, globalMax);
        boxplot.addLabels(" ", "Groups", "Values");     
        boxplot.addExtraPointsFromColumnSum(4, 5, false, firstRow, lastRow, "Std+", Excel_BoxplotMaker.PointStyle.DASH, (short)12, Color.RED);  
        boxplot.addExtraPointsFromColumnSum(4, 5, true, firstRow, lastRow, "Std-", Excel_BoxplotMaker.PointStyle.DASH, (short)12, Color.RED); 
        boxplot.addExtraPointsFromColumn(4, firstRow, lastRow, "Mean", Excel_BoxplotMaker.PointStyle.DIAMOND, (short)12, Color.YELLOW);        
        boxplot.addExtraPointsFromColumn(2, firstRow, lastRow, "Min", Excel_BoxplotMaker.PointStyle.DASH, (short)20, Color.BLACK);
        boxplot.addExtraPointsFromColumn(3, firstRow, lastRow, "Max", Excel_BoxplotMaker.PointStyle.DASH, (short)20, Color.BLACK);  
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

    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        int numbins=(Integer)task.getParameter("Bins");
        NumericMap numericMap=(NumericMap)task.getParameter("Numeric Map");
        DataGroup datagroup=(DataGroup)task.getParameter("Groups");
        doNormalize=(Boolean)task.getParameter("Normalize");
        numericMapName=numericMap.getName();
        if (datagroup instanceof Data) dataGroupName=((Data)datagroup).getName();
        else dataGroupName=null;

        Class numericMapClass=numericMap.getMembersClass();
        Class groupMembersClass=(datagroup==null)?null:datagroup.getMembersClass();
        if (groupMembersClass!=null && !numericMapClass.equals(groupMembersClass)) throw new ExecutionError("The type of the numeric map ("+numericMap.getTypeDescription()+") is not compatible with the type of the group ("+((Data)datagroup).getTypeDescription()+")");
        dataGroupClass=(datagroup==null)?null:datagroup.getClass();
        ArrayList<String> assignedMembers=null;
        if (datagroup instanceof DataCollection) {
            assignedMembers=new ArrayList<String>(((DataCollection)datagroup).size());
            assignedMembers.addAll( ((DataCollection)datagroup).getValues());
        } else if (datagroup instanceof DataPartition) {
            assignedMembers=((DataPartition)datagroup).getAllAssignedMembers();
        } else {
            assignedMembers=task.getEngine().getNamesForAllDataItemsOfType(numericMapClass);
        }
        double min=numericMap.getMinValue(assignedMembers);
        double max=numericMap.getMaxValue(assignedMembers);
        double binrange=(max-min)/(double)numbins;
        int numgroups=(datagroup instanceof DataPartition)?((DataPartition)datagroup).getNumberOfClusters():1;
        clusterNames=new ArrayList<String>();
             if (datagroup instanceof DataPartition) clusterNames.addAll(((DataPartition)datagroup).getClusterNames());
        else if (datagroup instanceof DataCollection) clusterNames.add(dataGroupName);
        else clusterNames=null;
        statistics = new HashMap<String, double[]>(numgroups);
        bins = new HashMap<String, int[]>(numgroups);
        if (datagroup instanceof DataPartition && clusterNames!=null) {
            for (String clusterName:clusterNames) {
                int j=1;
                ArrayList<String> members=((DataPartition)datagroup).getAllMembersInCluster(clusterName);
                Object[] results=getResultsForGroup(numericMap, members, numbins, binrange, min);
                double[] clusterResults=(double[])results[0];
                int[] clusterbins=(int[])results[1];           
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                task.setStatusMessage("Executing analysis: "+getAnalysisName()+" ("+j+"/"+clusterNames.size()+")");
                task.setProgress(j, clusterNames.size()+1); // +1 is just to not reach 100%
                statistics.put(clusterName, clusterResults);
                bins.put(clusterName, clusterbins);
                j++;
            } // end for each cluster
        } else { // just a single group (no clusters)
            Object[] results=getResultsForGroup(numericMap, assignedMembers, numbins, binrange, min);
            double[] clusterResults=(double[])results[0];
            int[] clusterbins=(int[])results[1];           
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            task.setStatusMessage("Executing analysis: "+getAnalysisName());
            task.setProgress(50, 100); // set to 50%
            String groupname=(dataGroupName!=null)?dataGroupName:DEFAULT_GROUP;
            statistics.put(groupname, clusterResults);
            bins.put(groupname, clusterbins);           
        }
        
    }
  
    /** Calculates statistics for the given subgroup of the MumericMap
     *  @param members A list of the entries to include in the statistics
     *  @param an Object[] with two elements: [0]=a double[] with statistics, [1]=an int[] with bincounts 
     */
    private Object[] getResultsForGroup(NumericMap numericMap, ArrayList<String> members, int numbins, double binrange, double min) {
        double[] values=numericMap.getValues(members);
        int clustersize=values.length;
        int[] clusterbins=new int[numbins];
        double clusterSum=0;
        double clusterMin=Double.MAX_VALUE;
        double clusterMax=-Double.MAX_VALUE;
        for (int i=0;i<values.length;i++) {
            double value=values[i];
            int bin=(int)((value-min)/binrange);
            if (bin>=numbins) bin=numbins-1;
            clusterbins[bin]++;
            clusterSum+=value;
            if (value>clusterMax) clusterMax=value;
            if (value<clusterMin) clusterMin=value;

        }
        double clusterAverage=clusterSum/(double)clustersize;
        Arrays.sort(values);
        double clusterMedian=MotifLabEngine.getMedianValue(values);
        double firstQuartile=MotifLabEngine.getFirstQuartileValue(values);
        double thirdQuartile=MotifLabEngine.getThirdQuartileValue(values);            
        double clusterDeviationSum=0;
        for (int i=0;i<values.length;i++) {
            clusterDeviationSum+=(values[i]-clusterAverage)*(values[i]-clusterAverage);
        }
        double clusterStdDev=(double)Math.sqrt(clusterDeviationSum/(double)clustersize);
        double[] clusterResults=new double[]{values.length,clusterMin,clusterMax,clusterSum,clusterAverage,clusterMedian,clusterStdDev,firstQuartile,thirdQuartile};
        return new Object[]{clusterResults,clusterbins};
    }

    
   /** Creates a histogram chart based on the current data and saves it to file
    * @param file A file to save the image to
    * @param doNormalize If TRUE values will be normalized within each group 'inside' or 'outside'
    */
    private Dimension saveGraphAsImage(File file, int plots, double scale, VisualizationSettings settings) throws IOException {
        String[] cNames=null;
        int legendWidth=0;
        int legendHeight=0;
        int entries=1;
        Font legendFont=settings.getSystemFont("graph.legendFont");
        Font tickFont=settings.getSystemFont("graph.tickFont");
        
        if (clusterNames!=null && !clusterNames.isEmpty()) {
            cNames=new String[clusterNames.size()];
            cNames=clusterNames.toArray(cNames);
            Dimension dim=Graph.getLegendDimension(cNames, legendFont);
            legendWidth=dim.width;
            legendHeight=dim.height;
            entries=clusterNames.size();
        }    
        Dimension tickDimension=Graph.getDimension("100.0%", tickFont, null);
        int translateX=tickDimension.width+5+10;
        int graphwidth=500;
        int marginLegendLeft=30;
        int marginLegendRight=5;
        int width=translateX+graphwidth+marginLegendLeft+legendWidth+marginLegendRight;
        int graphheight=300; // height of graph in pixels (just the histogram);
        int bottomSpace=10+10+tickDimension.height; // Space beneath the X-axis (should have room for tickmarks)
        
        int height=graphheight+bottomSpace+((plots==PLOT_NONE)?0:(entries*25)); // image height
        if (height<legendHeight) height=legendHeight;
        
        // write the image to file
        if (file!=null) {
            if (!file.getName().endsWith(".png")) {
                    VectorGraphics2D g=null;
                    String filename=file.getName();
                         if (filename.endsWith(".svg")) g = new SVGGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
                    else if (filename.endsWith(".pdf")) g = new PDFGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
                    else if (filename.endsWith(".eps")) g = new EPSGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
                    g.setClip(0, 0, (int)Math.ceil(width*scale),(int)Math.ceil(height*scale));
                    paintImage(g, width, height, scale, plots, entries, graphwidth, graphheight, legendWidth, cNames, legendFont, tickFont, translateX, marginLegendLeft);                             
                    FileOutputStream fileStream = new FileOutputStream(file);
                    try {
                        fileStream.write(g.getBytes());
                    } finally {
                        fileStream.close();
                    }                                 
            } else {
                BufferedImage image=new BufferedImage((int)Math.ceil(width*scale),(int)Math.ceil(height*scale), BufferedImage.TYPE_INT_RGB);
                Graphics2D g=image.createGraphics();  
                paintImage(g, width, height, scale, plots, entries, graphwidth, graphheight, legendWidth, cNames, legendFont, tickFont, translateX, marginLegendLeft);
                OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
                ImageIO.write(image, "png", output);
                output.close(); 
                g.dispose();
            }          
        }
        return new Dimension(width,height);
    }
    
    
    /** Draws the graph into a Graphics object */
    private void paintImage(Graphics2D g, int width, int height, double scale, int plots, int entries, int graphwidth, int graphheight, int legendWidth, String[] cNames, Font legendFont, Font tickFont, int translateX, int marginLegendLeft) {
        String firstClusterName=(clusterNames==null || clusterNames.isEmpty())?DEFAULT_GROUP:clusterNames.get(0);
        int numbins=bins.get(firstClusterName).length; // the number of bins is the same for all clusters     
        g.scale(scale,scale);
        Stroke defaultStroke=g.getStroke();
        // BasicStroke dashed = new BasicStroke(1f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3f}, 0f);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width+10, height+10);
        int totalcount=0;
        double globalmin=Double.MAX_VALUE;
        double globalmax=-Double.MAX_VALUE;
        for (double[] clusterResults:statistics.values()) {
            totalcount+=(int)clusterResults[0]; // this is the size
            if (clusterResults[1]<globalmin) globalmin=clusterResults[1];
            if (clusterResults[2]>globalmax) globalmax=clusterResults[2];            
        }
        // normalize bin counts
        HashMap<String,double[]> normalized=new HashMap<String,double[]>(entries);
        double maxgraphvalue=0;  
        if (clusterNames!=null) {
            for (String clusterName:clusterNames) {
                double[] clusterNormalized=new double[numbins];
                int[] clusterBins=bins.get(clusterName);
                double[] clusterResults=statistics.get(clusterName);
                for (int i=0;i<numbins;i++) {
                    clusterNormalized[i]=(double)clusterBins[i]/((doNormalize)?(double)clusterResults[0]:(double)(totalcount));
                    if (clusterNormalized[i]>maxgraphvalue) maxgraphvalue=clusterNormalized[i];
                } 
                normalized.put(clusterName, clusterNormalized);
            }
        } else {
            double[] clusterNormalized=new double[numbins];
            int[] clusterBins=bins.get(DEFAULT_GROUP);
            double[] clusterResults=statistics.get(DEFAULT_GROUP);
            for (int i=0;i<numbins;i++) {
                clusterNormalized[i]=(double)clusterBins[i]/((doNormalize)?(double)clusterResults[0]:(double)(totalcount));
                if (clusterNormalized[i]>maxgraphvalue) maxgraphvalue=clusterNormalized[i];
            } 
            normalized.put(DEFAULT_GROUP, clusterNormalized);           
        }
        //int translateX=50; // the X coordinate for the left edge of the graph
        int translateY=(plots==PLOT_NONE)?10:(25*entries+10); // the Y coordinate for the top of the graph
        double tickscaleY=0;
        if (maxgraphvalue<=0.01) {tickscaleY=0.001f;}
        else if (maxgraphvalue<=0.1) {tickscaleY=0.01f;}
        else if (maxgraphvalue<=0.5) {tickscaleY=0.05f;}
        else {tickscaleY=0.1f;}
        maxgraphvalue=(((int)(maxgraphvalue/tickscaleY))+1)*tickscaleY; // ceil to nearest X
        if (maxgraphvalue>1.0) maxgraphvalue=1.0f;
        Graph graph=new Graph(g, globalmin, globalmax, 0f, maxgraphvalue, graphwidth, graphheight, translateX, translateY);

//        g.setStroke(new BasicStroke(1f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f}, 0f));
//        g.setColor(Graph.lightGray);
//        graph.drawHorizontalGridLines();
//        graph.drawVerticalGridLines();
//        g.setStroke(defaultStroke);

        for (int index=0;index<entries;index++) {
            String clusterName=(clusterNames==null)?DEFAULT_GROUP:clusterNames.get(index);
            Color clusterColor=clusterColors.get(index);
            double[] clusterResults=statistics.get(clusterName);
            double[] clusterNormalized=normalized.get(clusterName);
            g.setColor(clusterColor);
            graph.drawHistogram(clusterNormalized);
            if (plots==PLOT_MEDIAN_QUARTILES || plots==PLOT_BOTH) graph.drawHorizontalBoxAndWhiskers(clusterResults[1], clusterResults[2], clusterResults[5], clusterResults[7], clusterResults[8], 25*index,7);
            if (plots==PLOT_MEAN_STD || plots==PLOT_BOTH) graph.drawHorizontalMeanAndStdDeviation(clusterResults[4], clusterResults[6], 25*index,7);
            int averageXcoordinate=graph.getXforValue(clusterResults[4]);
            int medianXcoordinate=graph.getXforValue(clusterResults[5]); 
            if (plots==PLOT_BOTH || plots==PLOT_MEAN_STD) {
                g.setStroke(Graph.DASHED_LONG_STROKE);
                g.drawLine(averageXcoordinate, 25*index+15, averageXcoordinate, graphheight+translateY+6);
                g.setStroke(defaultStroke);
            }
            if (plots==PLOT_BOTH || plots==PLOT_MEDIAN_QUARTILES) g.drawLine(medianXcoordinate, 25*index, medianXcoordinate, graphheight+translateY+6);
            
        }
        g.setColor(Color.BLACK);
        //g.drawRect(translateX, translateY, graphwidth, graphheight); // draw bounding box
        Font save=g.getFont();
        g.setFont(tickFont);
        graph.drawXaxisWithTicks(graphheight+translateY, false, false);
        graph.drawYaxisWithTicks(translateX, false, true);
        Color[] cColors=new Color[clusterColors.size()];
        cColors=clusterColors.toArray(cColors);
        if (cNames!=null) {
            int legendX=translateX+graphwidth+marginLegendLeft;
            g.setFont(legendFont);
            graph.drawLegendBox(cNames, cColors, legendX, 0, true);
        }               
        g.setFont(save);        
    }

    /** Returns an Nx(m+1) matrix containing [X,Y1,Y2,...,Ym] data for the histogram */
    private HashMap<String,double[]> getHistogramValues() {
        int entries=(clusterNames!=null && !clusterNames.isEmpty())?clusterNames.size():1;
        String firstClusterName=(clusterNames==null || clusterNames.isEmpty())?DEFAULT_GROUP:clusterNames.get(0);
        int numbins=bins.get(firstClusterName).length; // the number of bins is the same for all clusters
        int totalcount=0;
        double globalmin=Double.MAX_VALUE;
        double globalmax=-Double.MAX_VALUE;
        for (double[] clusterResults:statistics.values()) {
            totalcount+=(int)clusterResults[0]; // this is the size
            if (clusterResults[1]<globalmin) globalmin=clusterResults[1];
            if (clusterResults[2]>globalmax) globalmax=clusterResults[2];            
        }
        // normalize bin counts
        HashMap<String,double[]> normalized=new HashMap<String,double[]>(entries);
        double maxgraphvalue=0;  
        if (clusterNames!=null) {
            for (String clusterName:clusterNames) {
                double[] clusterNormalized=new double[numbins];
                int[] clusterBins=bins.get(clusterName);
                double[] clusterResults=statistics.get(clusterName);
                for (int i=0;i<numbins;i++) {
                    clusterNormalized[i]=(double)clusterBins[i]/((doNormalize)?(double)clusterResults[0]:(double)(totalcount));
                    if (clusterNormalized[i]>maxgraphvalue) maxgraphvalue=clusterNormalized[i];
                } 
                normalized.put(clusterName, clusterNormalized);
            }
        } else {
            double[] clusterNormalized=new double[numbins];
            int[] clusterBins=bins.get(DEFAULT_GROUP);
            double[] clusterResults=statistics.get(DEFAULT_GROUP);
            for (int i=0;i<numbins;i++) {
                clusterNormalized[i]=(double)clusterBins[i]/((doNormalize)?(double)clusterResults[0]:(double)(totalcount));
                if (clusterNormalized[i]>maxgraphvalue) maxgraphvalue=clusterNormalized[i];
            } 
            normalized.put(DEFAULT_GROUP, clusterNormalized);           
        }
        double xstep=(globalmax-globalmin)/numbins;
        double[] xRange=new double[numbins];
        for (int i=0;i<numbins;i++) {
            xRange[i]=globalmin+i*xstep;
        }
        normalized.put("*xRange*",xRange);
        return normalized;        
        
    }

    private String formatNumber(double number) {
        return Graph.formatNumber(number, false);

    }

    private ArrayList<Color> assignClusterColors(ArrayList<String> clusternames, VisualizationSettings vs) {
        int size=(clusternames==null)?1:clusternames.size();
        ArrayList<Color> colors = new ArrayList<Color>(size);
        for (int i=0;i<size;i++) {
            if (clusternames!=null) {
                colors.add(vs.getClusterColor(clusternames.get(i)));
            }
            else {
                if (i<palette.length) colors.add(palette[i]);
                else {
                    //choose a random color which is not too dark and not too unsaturated
                    colors.add(Color.getHSBColor((float)Math.random(),(float)(Math.random()/2.0+0.5),(float)(Math.random()/2.0+0.5)));
                }
            }
        }       
        return colors;
    }

    private static Color[] palette=new Color[]{Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW,Color.CYAN,Color.MAGENTA,Color.GRAY,Color.PINK,Color.ORANGE};

    @Override
    protected JPanel getDisplayPanel(MotifLabGUI gui, boolean modal) {
        OutputData document=new OutputData("temp");
        try {document=formatHTML(document, gui.getEngine(), null, null, null);}
        catch (Exception e) {
            document.append("ERROR:"+e.getMessage(), HTML);
            //e.printStackTrace(System.err);
        }
        document.setShowAsHTML(true);
        OutputPanel panel=new OutputPanel(document, gui);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8,5,0,5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)
        ));
        panel.setPreferredSize(new Dimension(850,600));
        return panel;
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

    // -----------------------------


}
