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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
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
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.gui.OutputPanel;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.VisualizationSettings;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;

/**
 *
 * @author kjetikl
 */
public class DistributionAnalysis extends Analysis {
    private static final String typedescription="Analysis: numeric dataset distribution"; 
    private static final String analysisName="numeric dataset distribution";
    private final static String description="Computes distribution statistics for a Numeric Dataset or compares the distribution of values for a Numeric Dataset inside versus outside regions of a Region Dataset";
    private String regionDatasetName=null; // if this is NULL we calculate one distribution for the entire dataset ('inside'). If this is set we split into 2 distributions 'inside' and 'outside' regions
    private String numericDatasetName=null;
    private String sequenceCollectionName=null;
    private int sequenceCollectionSize=0;
    protected int[] insideBins=null; // 
    private int[] outsideBins=null; // 
    protected double insideMax=-Double.MAX_VALUE;
    protected double insideMin=Double.MAX_VALUE;
    private double insideSum=0;
    private double insideAverage=0;
    private double insideMedian=0;
    private double insideFirstQuartile=0;
    private double insideThirdQuartile=0;    
    private double insideStdDev=0;
    private double outsideMax=-Double.MAX_VALUE;
    private double outsideMin=Double.MAX_VALUE;
    private double outsideSum=0;
    private double outsideAverage=0;
    private double outsideMedian=0;
    private double outsideFirstQuartile=0;
    private double outsideThirdQuartile=0;    
    private double outsideStdDev=0;
    protected int  insideRegionsBaseCount=0;
    protected int  outsideRegionsBaseCount=0; 
    private boolean  doNormalize=true;

   
    private final String[] variablesSplit = new String[]{"inside count","inside min","inside max","inside median","inside average","inside stdev","inside first quartile","inside third quartile","outside count","outside min","outside max","outside median","outside average","outside stdev","outside first quartile","outside third quartile"};
    private final String[] variables = new String[]{"count","min","max","median","average","stdev","first quartile","third quartile"};

    private static final String NONE="None";   
    private static final String MEDIAN_QUARTILES="Median+Quartiles"; 
    private static final String MEAN_STD="Average+StDev"; 
    private static final String BOTH="All";     
    
    private static final int PLOT_NONE=0;
    private static final int PLOT_MEDIAN_QUARTILES=1;
    private static final int PLOT_MEAN_STD=2;
    private static final int PLOT_BOTH=3;
    
    
    public DistributionAnalysis() {
        this.name="DistributionAnalysis_temp";
        addParameter("Numeric dataset",NumericDataset.class, null,new Class[]{NumericDataset.class},"A numeric dataset to estimate distribution statistics for",true,false);
        addParameter("Region dataset",RegionDataset.class, null,new Class[]{RegionDataset.class},"If specified, distribution statistics will be calculated separately for bases inside versus outside regions in this dataset",false,false);
        addParameter("Sequences",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"If specified, the analysis will be limited to the sequences in this collection",false,false);
        addParameter("Normalize",Boolean.class, Boolean.TRUE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Normalizes the histogram of each group independently of each other (this will only affect the appearance of the histogram, not the statistics)",false,false);
        addParameter("Bins",Integer.class, new Integer(100),new Integer[]{1,1000},"Number of bins to partition the numeric values into for the histogram",false,false);
        addParameter("Cumulative histogram",Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Show the histogram as cumulative distribution",false,false);
        // addParameter("Bootstrap samples",Integer.class, new Integer(0),new Integer[]{0,10000000},"Number of samples to use when calculating significance of Mann-Whitney-U test",false,false);
    }
    
    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Numeric dataset","Region dataset"};}     

    /** Returns a list of output parameters that can be set when an Analysis is output */
    @Override
    public Parameter[] getOutputParameters(String dataformat) {        
         Parameter imageformat=new Parameter("Image format",String.class, "png",new String[]{"png","svg","pdf","eps"},"The image format to use for the graph",false,false);                                 
         Parameter boxplotpar=new Parameter("Box plot",String.class, BOTH,new String[]{NONE,MEDIAN_QUARTILES,MEAN_STD,BOTH},"Which statistics to show using box plots",false,false);        
         Parameter scalepar=new Parameter("Graph scale",Integer.class,100,new Integer[]{10,2000},"Scale of graphics plot (in percent)",false,false);
         if (dataformat.equals(HTML)) return new Parameter[]{boxplotpar,imageformat,scalepar};
         else return new Parameter[0];
    }    
    
//    @Override
//    public String[] getOutputParameterFilter(String parameter) {
//        if (parameter.equals("Graph scale") || parameter.equals("Image format") || parameter.equals("Box plot")) return new String[]{HTML};
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
        if (regionDatasetName==null) return variables;
        else return variablesSplit;
    }

    @Override    
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
             if (variablename.equals("inside count") || variablename.equals("count")) return new NumericVariable("result", insideRegionsBaseCount);
        else if (variablename.equals("inside min") || variablename.equals("min")) return new NumericVariable("result", insideMin);
        else if (variablename.equals("inside max") || variablename.equals("max")) return new NumericVariable("result", insideMax);
        else if (variablename.equals("inside median") || variablename.equals("median")) return new NumericVariable("result", insideMedian);
        else if (variablename.equals("inside average") || variablename.equals("inside mean") || variablename.equals("average")  || variablename.equals("mean")) return new NumericVariable("result", insideAverage);
        else if (variablename.equals("inside stdev") || variablename.equals("stdev")) return new NumericVariable("result", insideStdDev);
        else if (variablename.equals("inside first quartile") || variablename.equals("first quartile")) return new NumericVariable("result", insideFirstQuartile);
        else if (variablename.equals("inside third quartile") || variablename.equals("third quartile")) return new NumericVariable("result", insideThirdQuartile); 
        else if (variablename.equals("outside count") && regionDatasetName!=null) return new NumericVariable("result", outsideRegionsBaseCount);
        else if (variablename.equals("outside min") && regionDatasetName!=null) return new NumericVariable("result", outsideMin);
        else if (variablename.equals("outside max") && regionDatasetName!=null) return new NumericVariable("result", outsideMax);
        else if (variablename.equals("outside median") && regionDatasetName!=null) return new NumericVariable("result", outsideMedian);
        else if ((variablename.equals("outside average") || variablename.equals("outside mean")) && regionDatasetName!=null) return new NumericVariable("result", outsideAverage);
        else if (variablename.equals("outside stdev") && regionDatasetName!=null) return new NumericVariable("result", outsideStdDev);
        else if (variablename.equals("outside first quartile") && regionDatasetName!=null) return new NumericVariable("result", outsideFirstQuartile);
        else if (variablename.equals("outside third quartile") && regionDatasetName!=null) return new NumericVariable("result", outsideThirdQuartile);                
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }
    
    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else return NumericVariable.class; // all exported values in this analysis are numerical
    } 
    
    @Override
    @SuppressWarnings("unchecked")    
    public DistributionAnalysis clone() {
        DistributionAnalysis newanalysis=new DistributionAnalysis();
        super.cloneCommonSettings(newanalysis);         
        newanalysis.regionDatasetName=this.regionDatasetName;
        newanalysis.numericDatasetName=this.numericDatasetName;
        newanalysis.sequenceCollectionName=this.sequenceCollectionName;
        newanalysis.sequenceCollectionSize=this.sequenceCollectionSize;
        newanalysis.insideMin=this.insideMin;
        newanalysis.insideMax=this.insideMax;
        newanalysis.insideSum=this.insideSum;
        newanalysis.insideAverage=this.insideAverage;  
        newanalysis.insideStdDev=this.insideStdDev;  
        newanalysis.insideMedian=this.insideMedian;  
        newanalysis.insideFirstQuartile=this.insideFirstQuartile;
        newanalysis.insideThirdQuartile=this.insideThirdQuartile;      
        newanalysis.insideRegionsBaseCount=this.insideRegionsBaseCount;           
        newanalysis.outsideMin=this.outsideMin;
        newanalysis.outsideMax=this.outsideMax;
        newanalysis.outsideSum=this.outsideSum;
        newanalysis.outsideAverage=this.outsideAverage;
        newanalysis.outsideStdDev=this.outsideStdDev;        
        newanalysis.outsideMedian=this.outsideMedian;  
        newanalysis.outsideFirstQuartile=this.outsideFirstQuartile;
        newanalysis.outsideThirdQuartile=this.outsideThirdQuartile;       
        newanalysis.outsideRegionsBaseCount=this.outsideRegionsBaseCount;
        newanalysis.insideBins=(this.insideBins==null)?null:this.insideBins.clone();   
        newanalysis.outsideBins=(this.outsideBins==null)?null:this.outsideBins.clone();
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        DistributionAnalysis other=(DistributionAnalysis)source;
        this.regionDatasetName=other.regionDatasetName;
        this.numericDatasetName=other.numericDatasetName;
        this.sequenceCollectionName=other.sequenceCollectionName;
        this.sequenceCollectionSize=other.sequenceCollectionSize;       
        this.insideSum=other.insideSum;
        this.insideMin=other.insideMin;
        this.insideMax=other.insideMax;
        this.insideAverage=other.insideAverage;
        this.insideStdDev=other.insideStdDev;
        this.insideMedian=other.insideMedian;  
        this.insideFirstQuartile=other.insideFirstQuartile;  
        this.insideThirdQuartile=other.insideThirdQuartile;  
        this.outsideSum=other.outsideSum;
        this.outsideMin=other.outsideMin;
        this.outsideMax=other.outsideMax;
        this.outsideAverage=other.outsideAverage;
        this.outsideStdDev=other.outsideStdDev;
        this.outsideMedian=other.outsideMedian;  
        this.outsideFirstQuartile=other.outsideFirstQuartile;
        this.outsideThirdQuartile=other.outsideThirdQuartile;              
        this.insideRegionsBaseCount=other.insideRegionsBaseCount;
        this.outsideRegionsBaseCount=other.outsideRegionsBaseCount;
        this.insideBins=other.insideBins;
        this.outsideBins=other.outsideBins;    
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
        VisualizationSettings vz=engine.getClient().getVisualizationSettings();
        File imagefile=outputobject.createDependentFile(engine,imageFormat);
        Color color1=vz.getSystemColor("color1");
        Color color2=vz.getSystemColor("color2");
        String textColor1=VisualizationSettings.isDark(color1)?"#FFFFFF":"#000000";
        String textColor2=VisualizationSettings.isDark(color2)?"#FFFFFF":"#000000";
        Dimension dim=null;
        try {
            dim=saveGraphAsImage(imagefile, plots, scale, color1, color2, vz);
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        engine.createHTMLheader("Numeric Dataset Distribution Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<div align=\"center\">\n",HTML);
        if (regionDatasetName!=null) outputobject.append("<h2 class=\"headline\">Distribution of values for '"+numericDatasetName+"' inside versus outside '"+regionDatasetName+"' regions</h2>\n",HTML);
        else outputobject.append("<h2 class=\"headline\">Distribution of values for '"+numericDatasetName+"'</h2>\n",HTML);
        outputobject.append("<br>Analysis based on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),HTML);
        if (sequenceCollectionName!=null) outputobject.append(" from collection <span class=\"dataitem\">"+sequenceCollectionName+"</span>\n",HTML);
        outputobject.append("<br />\n<br />\n<table class=\"sortable\">\n",HTML);
        if (regionDatasetName==null) {
            outputobject.append("<tr><th width=\"90\"><nobr>Base count</nobr></th><th width=\"90\">Min</th><th width=\"90\">Max</th><th width=\"90\">Average</th><th width=\"90\">Std. dev.</th><th width=\"90\">Median</th><th width=\"90\"><nobr>1st Quartile</nobr></th><th width=\"90\"><nobr>3rd Quartile</nobr></th></tr>\n",HTML);
            outputobject.append("<tr><td style=\"text-align:center\">"+insideRegionsBaseCount+"</td><td style=\"text-align:center\">"+formatNumber(insideMin)+"</td><td style=\"text-align:center\">"+formatNumber(insideMax)+"</td><td style=\"text-align:center\">"+formatNumber(insideAverage)+"</td><td style=\"text-align:center\">"+formatNumber(insideStdDev)+"</td><td style=\"text-align:center\">"+formatNumber(insideMedian)+"</td><td style=\"text-align:center\">"+formatNumber(insideFirstQuartile)+"</td><td style=\"text-align:center\">"+formatNumber(insideThirdQuartile)+"</td></tr>",HTML);
        } else {
            outputobject.append("<tr><th width=\"90\"> </th><th width=\"90\"><nobr>Base count</nobr></th><th width=\"90\">Min</th><th width=\"90\">Max</th><th width=\"90\">Average</th><th width=\"90\">Std. dev.</th><th width=\"90\">Median</th><th width=\"90\"><nobr>1st Quartile</nobr></th><th width=\"90\"><nobr>3rd Quartile</nobr></th></tr>\n",HTML);
            outputobject.append("<tr><td style=\"color:"+textColor1+";background-color:"+VisualizationSettings.convertColorToHTMLrepresentation(color1)+";text-align:left;padding-left:10px;padding-right:10px;font-weight:bold;\">Inside</td><td style=\"text-align:center\">"+insideRegionsBaseCount+"</td><td style=\"text-align:center\">"+formatNumber(insideMin)+"</td><td style=\"text-align:center\">"+formatNumber(insideMax)+"</td><td style=\"text-align:center\">"+formatNumber(insideAverage)+"</td><td style=\"text-align:center\">"+formatNumber(insideStdDev)+"</td><td style=\"text-align:center\">"+formatNumber(insideMedian)+"</td><td style=\"text-align:center\">"+formatNumber(insideFirstQuartile)+"</td><td style=\"text-align:center\">"+formatNumber(insideThirdQuartile)+"</td></tr>\n",HTML);
            outputobject.append("<tr><td style=\"color:"+textColor2+";background-color:"+VisualizationSettings.convertColorToHTMLrepresentation(color2)+";text-align:left;padding-left:10px;padding-right:10px;font-weight:bold;\">Outside</td><td style=\"text-align:center\">"+outsideRegionsBaseCount+"</td><td style=\"text-align:center\">"+formatNumber(outsideMin)+"</td><td style=\"text-align:center\">"+formatNumber(outsideMax)+"</td><td style=\"text-align:center\">"+formatNumber(outsideAverage)+"</td><td style=\"text-align:center\">"+formatNumber(outsideStdDev)+"</td><td style=\"text-align:center\">"+formatNumber(outsideMedian)+"</td><td style=\"text-align:center\">"+formatNumber(outsideFirstQuartile)+"</td><td style=\"text-align:center\">"+formatNumber(outsideThirdQuartile)+"</td></tr>\n",HTML);
        }
        outputobject.append("</table>\n",HTML);
        outputobject.append("<br /><br /><br /><br />\n",HTML);
        if (imageFormat.equals("pdf")) outputobject.append("<object type=\"application/pdf\" data=\"file:///"+imagefile.getAbsolutePath()+"\"></object>",HTML);
        else {
            outputobject.append("<img src=\"file:///"+imagefile.getAbsolutePath()+"\"",HTML);
            if (dim!=null) outputobject.append(" width="+(int)Math.ceil(dim.width*scale)+" height="+(int)Math.ceil(dim.height*scale),HTML);                          
            outputobject.append(" />\n",HTML);
        }        
        outputobject.append("</div>\n",HTML);
        outputobject.append("</body>\n</html>\n",HTML);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }

    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        if (regionDatasetName==null) {
            outputobject.append("#Distribution of values for '"+numericDatasetName+"'\n",RAWDATA);
            outputobject.append("#Analysis based on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),RAWDATA);
            if (sequenceCollectionName!=null) outputobject.append(" from collection '"+sequenceCollectionName+"'",RAWDATA);
            outputobject.append("\n",RAWDATA);
            outputobject.append("basecount="+insideRegionsBaseCount+"\n",RAWDATA);
            outputobject.append("min="+insideMin+"\n",RAWDATA);
            outputobject.append("max="+insideMax+"\n",RAWDATA);
            outputobject.append("average="+insideAverage+"\n",RAWDATA);
            outputobject.append("standard deviation="+insideStdDev+"\n",RAWDATA);
            outputobject.append("median="+insideMedian+"\n",RAWDATA);        
            outputobject.append("first quartile="+insideFirstQuartile+"\n",RAWDATA);
            outputobject.append("third quartile="+insideThirdQuartile+"\n",RAWDATA);              
        } else {
            outputobject.append("#Distribution of values for '"+numericDatasetName+"' inside versus outside '"+regionDatasetName+"' regions\n",RAWDATA);
            outputobject.append("#Analysis based on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),RAWDATA);
            if (sequenceCollectionName!=null) outputobject.append(" from collection '"+sequenceCollectionName+"'",RAWDATA);
            outputobject.append("\n",RAWDATA);
            outputobject.append("inside basecount="+insideRegionsBaseCount+"\n",RAWDATA);
            outputobject.append("inside min="+insideMin+"\n",RAWDATA);
            outputobject.append("inside max="+insideMax+"\n",RAWDATA);
            outputobject.append("inside average="+insideAverage+"\n",RAWDATA);
            outputobject.append("inside standard deviation="+insideStdDev+"\n",RAWDATA);
            outputobject.append("inside median="+insideMedian+"\n",RAWDATA);        
            outputobject.append("inside first quartile="+insideFirstQuartile+"\n",RAWDATA);
            outputobject.append("inside third quartile="+insideThirdQuartile+"\n",RAWDATA);        
            outputobject.append("outside basecount="+outsideRegionsBaseCount+"\n",RAWDATA);
            outputobject.append("outside min="+outsideMin+"\n",RAWDATA);
            outputobject.append("outside max="+outsideMax+"\n",RAWDATA);
            outputobject.append("outside average="+outsideAverage+"\n",RAWDATA);          
            outputobject.append("outside standard deviation="+outsideStdDev+"\n",RAWDATA);
            outputobject.append("outside median="+outsideMedian+"\n",RAWDATA);        
            outputobject.append("outside first quartile="+outsideFirstQuartile+"\n",RAWDATA);
            outputobject.append("outside third quartile="+outsideThirdQuartile+"\n",RAWDATA);        
        }
//        outputobject.append(drawHistogram(),RAWDATA);
//        outputobject.append("\n\n",RAWDATA);   
//        double min=Math.min(insideMin, insideMax);
//        double max=Math.max(insideMin, insideMax);
//        double binrange=(max-min)/(double)insideBins.length;
//        for (int i=0;i<insideBins.length;i++) {
//            outputobject.append("inside["+i+":"+(binrange*i)+"-"+(binrange*(i+1))+"]=>"+insideBins[i]+"\n",RAWDATA);
//        }
//        outputobject.append("\n\n",RAWDATA);
//        for (int i=0;i<outsideBins.length;i++) {
//            outputobject.append("outsideBins["+i+":"+(binrange*i)+"-"+(binrange*(i+1))+"]=>"+outsideBins[i]+"\n",RAWDATA);
//        }
        if (format!=null) format.setProgress(100);
        return outputobject;
    }
    
    @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        XSSFWorkbook workbook=null;
        try {
            String template=(regionDatasetName==null)?"AnalysisTemplate_Distribution_justInside":"AnalysisTemplate_Distribution";
            InputStream stream = DistributionAnalysis.class.getResourceAsStream("resources/"+template+".xlsx");
            workbook = (XSSFWorkbook)WorkbookFactory.create(stream);
            stream.close();
            
            XSSFSheet sheet = workbook.getSheetAt(0);
            // Update the statistics matrix
            int offset=(regionDatasetName==null)?1:2;
            sheet.getRow(6).getCell(offset+0).setCellValue(insideRegionsBaseCount);
            sheet.getRow(6).getCell(offset+1).setCellValue(insideMin);
            sheet.getRow(6).getCell(offset+2).setCellValue(insideMax);
            sheet.getRow(6).getCell(offset+3).setCellValue(insideAverage);
            sheet.getRow(6).getCell(offset+4).setCellValue(insideStdDev);
            sheet.getRow(6).getCell(offset+5).setCellValue(insideMedian);
            sheet.getRow(6).getCell(offset+6).setCellValue(insideFirstQuartile);
            sheet.getRow(6).getCell(offset+7).setCellValue(insideThirdQuartile);
                       
            if (regionDatasetName!=null) {            
                sheet.getRow(7).getCell(offset+0).setCellValue(outsideRegionsBaseCount);
                sheet.getRow(7).getCell(offset+1).setCellValue(outsideMin);
                sheet.getRow(7).getCell(offset+2).setCellValue(outsideMax);
                sheet.getRow(7).getCell(offset+3).setCellValue(outsideAverage);
                sheet.getRow(7).getCell(offset+4).setCellValue(outsideStdDev);
                sheet.getRow(7).getCell(offset+5).setCellValue(outsideMedian);
                sheet.getRow(7).getCell(offset+6).setCellValue(outsideFirstQuartile);
                sheet.getRow(7).getCell(offset+7).setCellValue(outsideThirdQuartile);
            }            
            forceExcelFormulaRecalculation(workbook,null);             
            
            // setup the data for the histogram                                      
            double[][] histogram=getHistogramValues();
            int bins=insideBins.length; 
            XSSFSheet histogramsheet = workbook.getSheet(" ");

            for (int i=0;i<bins;i++) { // insert histogram values into Sheet2. If only one set of values is defined, these are considered to be "inside"
                Row row=histogramsheet.getRow(i);
                if (row==null) row=histogramsheet.createRow(i);
                Cell cell=row.getCell(0);
                if (cell==null) cell=row.createCell(0);
                cell.setCellValue(histogram[i][0]);
                cell=row.getCell(1);
                if (cell==null) cell=row.createCell(1);                
                if (regionDatasetName!=null) { // if we should include "outside" then this is placed before "inside" (although the order is opposite in histogram[][])
                    cell.setCellValue(histogram[i][2]); // [sic] !!
                    cell=row.getCell(2);
                    if (cell==null) cell=row.createCell(2);
                }
                cell.setCellValue(histogram[i][1]); // [sic] !!                   
            }
            
            // update the data range of the histogram
            XSSFDrawing drawing = ((XSSFSheet)sheet).createDrawingPatriarch();           
            XSSFChart histogramChart = drawing.getCharts().get(0);
            List<XDDFChartData> data = histogramChart.getChartSeries();

            // Define new data ranges
           if (data.get(0).getSeriesCount()>1) {
                String newRange = "' '!$C$1:$C$"+bins;
                XDDFBarChartData.Series series1 = (XDDFBarChartData.Series)data.get(0).getSeries(0);                
                series1.getCTBarSer().getVal().getNumRef().setF(newRange);                
               
                XDDFBarChartData.Series series2 = (XDDFBarChartData.Series)data.get(0).getSeries(1);
                String newRange2 = "' '!$B$1:$B$"+bins;
                series2.getCTBarSer().getVal().getNumRef().setF(newRange2);                
           } else {
                String newRange = "' '!$B$1:$B$"+bins;
                XDDFBarChartData.Series series1 = (XDDFBarChartData.Series)data.get(0).getSeries(0);                
                series1.getCTBarSer().getVal().getNumRef().setF(newRange);               
           }     
            
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
                    double minValue = (regionDatasetName==null)?insideMin:(Math.min(insideMin, outsideMin));
                    double maxValue = (regionDatasetName==null)?insideMax:(Math.max(insideMax, outsideMax));                               
                    xAxis.setMinimum(minValue); // 
                    xAxis.setMaximum(maxValue); //
                }
            }
            
            // Now update the tables and the description in the header
            sheet.setForceFormulaRecalculation(true);
            String descriptionString="Distribution of values for \""+numericDatasetName+"\"";
            if (regionDatasetName!=null) descriptionString+=" inside versus outside \""+regionDatasetName+"\" regions";
            sheet.getRow(0).getCell(0).setCellValue(descriptionString);           
            String numberOfSequencesString="Analysis based on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":"");
            if (sequenceCollectionName!=null) numberOfSequencesString+=" from collection \""+sequenceCollectionName+"\"";
            sheet.getRow(2).getCell(1).setCellValue(numberOfSequencesString);
                        
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
        
    
    //@Override
    public void runAnalysis_old(OperationTask task) throws Exception {
        int bins=(Integer)task.getParameter("Bins");
        insideBins=new int[bins];
        outsideBins=new int[bins];  
        int bootstrapsamples=0;
        Integer bs=(Integer)task.getParameter("Bootstrap samples");   
        if (bs!=null) bootstrapsamples=bs.intValue();
        RegionDataset regionTrack=(RegionDataset)task.getParameter("Region dataset");        
        NumericDataset valueTrack=(NumericDataset)task.getParameter("Numeric dataset");
        doNormalize=(Boolean)task.getParameter("Normalize");
        boolean showCumulative=(Boolean)task.getParameter("Cumulative histogram");
        regionDatasetName=(regionTrack==null)?null:regionTrack.getName();
        numericDatasetName=valueTrack.getName();
        SequenceCollection sequenceCollection=(SequenceCollection)task.getParameter("Sequences");
        if (sequenceCollection==null) sequenceCollection=task.getEngine().getDefaultSequenceCollection();
        if (sequenceCollection.getName().equals(task.getEngine().getDefaultSequenceCollectionName())) sequenceCollectionName=null;
        else sequenceCollectionName=sequenceCollection.getName();
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(task.getEngine());
        sequenceCollectionSize=sequences.size();
        double[] minmaxRange=valueTrack.getMinMaxValuesFromData();
        double min=minmaxRange[0];
        double max=minmaxRange[1];
        double binrange=(max-min)/(double)bins;  
        ArrayList<Double> insideValues=new ArrayList<Double>();
        ArrayList<Double> outsideValues=(regionTrack==null)?null:new ArrayList<Double>();
        //ArrayList<Double[]> samples=new ArrayList<Double[]>(); // samples used for significance testing
        for (int i=0;i<sequenceCollectionSize;i++) {
            String sequenceName=sequences.get(i).getName();
            NumericSequenceData valueTrackSequence=(NumericSequenceData)valueTrack.getSequenceByName(sequenceName);
            RegionSequenceData regionTrackSequence=(regionTrack==null)?null:(RegionSequenceData)regionTrack.getSequenceByName(sequenceName);
            for (int pos=0;pos<valueTrackSequence.getSize();pos++) {
                double value=valueTrackSequence.getValueAtRelativePosition(pos);
                boolean inside=(regionTrack==null)?true:regionTrackSequence.isWithinRegion(pos);
                int bin=(int)((value-min)/binrange);
                if (bin>=bins) bin=bins-1;
                if (inside) {
                    insideRegionsBaseCount++;
                    insideBins[bin]++;
                    insideSum+=value;
                    insideValues.add(value);
                    if (value>insideMax) insideMax=value;
                    if (value<insideMin) insideMin=value;
                } else {
                    outsideRegionsBaseCount++;
                    outsideBins[bin]++;
                    outsideSum+=value;
                    outsideValues.add(value);
                    if (value>outsideMax) outsideMax=value;
                    if (value<outsideMin) outsideMin=value;                     
                }    
                //if (bs!=null && bs>0) samples.add(new Double[]{value,(inside)?1.0:2.0,0.0});
            }
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            task.setStatusMessage("Executing analysis: "+getAnalysisName()+" ("+(i+1)+"/"+sequenceCollectionSize+")");
            task.setProgress(i+1, sequences.size()+1); // +1 is just to not reach 100%            
        }
        if (insideRegionsBaseCount==0) {
             insideMin=Double.NaN;
             insideMax=Double.NaN;
             insideSum=Double.NaN;
             insideAverage=Double.NaN;
             insideStdDev=Double.NaN;
             insideMedian=Double.NaN;
             insideFirstQuartile=Double.NaN;
             insideThirdQuartile=Double.NaN;
        } else {
             insideAverage=insideSum/(double)insideRegionsBaseCount;
             Collections.sort(insideValues);
             insideMedian=MotifLabEngine.getMedianValue(insideValues);    
             insideFirstQuartile=MotifLabEngine.getFirstQuartileValue(insideValues);  
             insideThirdQuartile=MotifLabEngine.getThirdQuartileValue(insideValues);  
             double insideDeviationSum=0;
             for (int i=0;i<insideValues.size();i++) {
                insideDeviationSum+=(insideValues.get(i)-insideAverage)*(insideValues.get(i)-insideAverage);
             }
             insideStdDev=(double)Math.sqrt(insideDeviationSum/(double)insideRegionsBaseCount);              
        }
        if (outsideRegionsBaseCount==0) {
             outsideMin=Double.NaN;
             outsideMax=Double.NaN;
             outsideSum=Double.NaN;
             outsideAverage=Double.NaN;
             outsideStdDev=Double.NaN;
             outsideMedian=Double.NaN;
             outsideFirstQuartile=Double.NaN;
             outsideThirdQuartile=Double.NaN;             
        } else {
             outsideAverage=outsideSum/(double)outsideRegionsBaseCount;
             Collections.sort(outsideValues);
             outsideMedian=MotifLabEngine.getMedianValue(outsideValues);    
             outsideFirstQuartile=MotifLabEngine.getFirstQuartileValue(outsideValues);  
             outsideThirdQuartile=MotifLabEngine.getThirdQuartileValue(outsideValues);               
             double outsideDeviationSum=0;
             for (int i=0;i<outsideValues.size();i++) {
                outsideDeviationSum+=(outsideValues.get(i)-outsideAverage)*(outsideValues.get(i)-outsideAverage);
             }
             outsideStdDev=(double)Math.sqrt(outsideDeviationSum/(double)outsideRegionsBaseCount);            
        }
        if (showCumulative) { // use cumulative histogram
            for (int i=1;i<insideBins.length;i++) {
                insideBins[i]=insideBins[i]+insideBins[i-1];
                outsideBins[i]=outsideBins[i]+outsideBins[i-1];
            }
        }
//        if (bootstrapsamples>0) {
//            SampleComparator sampleComparator=new SampleComparator();
//            Collections.sort(samples,sampleComparator);
//            double[] U=MannWhitneyU(samples);
//            bootstrapValues(samples, bootstrapsamples, insideRegionsBaseCount, outsideRegionsBaseCount, U[0], U[1], task);
//        }
        
    }    
    
    
    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        int bins=(Integer)task.getParameter("Bins");
        insideBins=new int[bins];
        outsideBins=new int[bins];  
        int bootstrapsamples=0;
        Integer bs=(Integer)task.getParameter("Bootstrap samples");   
        if (bs!=null) bootstrapsamples=bs.intValue();
        RegionDataset regionTrack=(RegionDataset)task.getParameter("Region dataset");        
        NumericDataset valueTrack=(NumericDataset)task.getParameter("Numeric dataset");
        doNormalize=(Boolean)task.getParameter("Normalize");
        boolean showCumulative=(Boolean)task.getParameter("Cumulative histogram");
        regionDatasetName=(regionTrack==null)?null:regionTrack.getName();
        numericDatasetName=valueTrack.getName();
        SequenceCollection sequenceCollection=(SequenceCollection)task.getParameter("Sequences");
        if (sequenceCollection==null) sequenceCollection=task.getEngine().getDefaultSequenceCollection();
        if (sequenceCollection.getName().equals(task.getEngine().getDefaultSequenceCollectionName())) sequenceCollectionName=null;
        else sequenceCollectionName=sequenceCollection.getName();
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(task.getEngine());
        sequenceCollectionSize=sequences.size();
        double[] minmaxRange=valueTrack.getMinMaxValuesFromData();
        double min=minmaxRange[0];
        double max=minmaxRange[1];
        double binrange=(max-min)/(double)bins;  
        ArrayList<Double> insideValues=new ArrayList<Double>();
        ArrayList<Double> outsideValues=(regionTrack==null)?null:new ArrayList<Double>();
        //ArrayList<Double[]> samples=new ArrayList<Double[]>(); // samples used for significance testing
              
        TaskRunner taskRunner=task.getEngine().getTaskRunner();
        task.setProgress(0L,sequenceCollectionSize);
        long[] counters=new long[]{0,0,sequenceCollectionSize}; // counters[0]=motifs started, [1]=motifs completed, [2]=total number of motifs

        ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(sequenceCollectionSize);
        for (int i=0;i<sequenceCollectionSize;i++) {
            String sequenceName=sequences.get(i).getName();
            processTasks.add(new ProcessSequenceTask(valueTrack, regionTrack, sequenceName, min, binrange, bins, insideValues, outsideValues, task, counters));
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
           if (e instanceof java.util.concurrent.ExecutionException) {
               Throwable cause=e.getCause();
               if (cause instanceof Exception) throw (Exception)cause;
               else throw new ExecutionError(cause.getMessage(), cause);
           } 
           else throw e; 
        }       
        if (countOK!=sequenceCollectionSize) {
            throw new ExecutionError("Some mysterious error occurred while performing analysis: "+getAnalysisName());
        }   
        
        // calculate statistics
        if (insideRegionsBaseCount==0) {
             insideMin=Double.NaN;
             insideMax=Double.NaN;
             insideSum=Double.NaN;
             insideAverage=Double.NaN;
             insideStdDev=Double.NaN;
             insideMedian=Double.NaN;
             insideFirstQuartile=Double.NaN;
             insideThirdQuartile=Double.NaN;
        } else {
             insideAverage=insideSum/(double)insideRegionsBaseCount;
             Collections.sort(insideValues);
             insideMedian=MotifLabEngine.getMedianValue(insideValues);    
             insideFirstQuartile=MotifLabEngine.getFirstQuartileValue(insideValues);  
             insideThirdQuartile=MotifLabEngine.getThirdQuartileValue(insideValues);  
             double insideDeviationSum=0;
             for (int i=0;i<insideValues.size();i++) {
                insideDeviationSum+=(insideValues.get(i)-insideAverage)*(insideValues.get(i)-insideAverage);
             }
             insideStdDev=(double)Math.sqrt(insideDeviationSum/(double)insideRegionsBaseCount);              
        }
        if (outsideRegionsBaseCount==0) {
             outsideMin=Double.NaN;
             outsideMax=Double.NaN;
             outsideSum=Double.NaN;
             outsideAverage=Double.NaN;
             outsideStdDev=Double.NaN;
             outsideMedian=Double.NaN;
             outsideFirstQuartile=Double.NaN;
             outsideThirdQuartile=Double.NaN;             
        } else {
             outsideAverage=outsideSum/(double)outsideRegionsBaseCount;
             Collections.sort(outsideValues);
             outsideMedian=MotifLabEngine.getMedianValue(outsideValues);    
             outsideFirstQuartile=MotifLabEngine.getFirstQuartileValue(outsideValues);  
             outsideThirdQuartile=MotifLabEngine.getThirdQuartileValue(outsideValues);               
             double outsideDeviationSum=0;
             for (int i=0;i<outsideValues.size();i++) {
                outsideDeviationSum+=(outsideValues.get(i)-outsideAverage)*(outsideValues.get(i)-outsideAverage);
             }
             outsideStdDev=(double)Math.sqrt(outsideDeviationSum/(double)outsideRegionsBaseCount);            
        }
        if (showCumulative) { // use cumulative histogram
            for (int i=1;i<insideBins.length;i++) {
                insideBins[i]=insideBins[i]+insideBins[i-1];
                outsideBins[i]=outsideBins[i]+outsideBins[i-1];
            }
        }
//        if (bootstrapsamples>0) {
//            SampleComparator sampleComparator=new SampleComparator();
//            Collections.sort(samples,sampleComparator);
//            double[] U=MannWhitneyU(samples);
//            bootstrapValues(samples, bootstrapsamples, insideRegionsBaseCount, outsideRegionsBaseCount, U[0], U[1], task);
//        }
        
    }       
    
      
    protected class ProcessSequenceTask implements Callable<String> {
        final String sequenceName;
        final NumericDataset numericdata;
        final RegionDataset regiondata;
        final long[] counters; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final OperationTask task;  
        final double min;
        final double binrange;
        final int bins;         
        final ArrayList<Double> insideValues;
        final ArrayList<Double> outsideValues;
        
        public ProcessSequenceTask(NumericDataset numericdata, RegionDataset regiondata, String sequenceName, double min, double binrange, int bins, ArrayList<Double> insideValues, ArrayList<Double> outsideValues, OperationTask task, long[] counters) {
           this.sequenceName=sequenceName;
           this.numericdata=numericdata;
           this.regiondata=regiondata;
           this.counters=counters;
           this.task=task;
           this.min=min;
           this.binrange=binrange;
           this.bins=bins;
           this.insideValues=insideValues;
           this.outsideValues=outsideValues;
        }
         
        @Override
        @SuppressWarnings("unchecked")
        public String call() throws Exception {
            synchronized(counters) {
               counters[0]++; // number of sequences started  
            }        
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            NumericSequenceData numericSequence=(NumericSequenceData)numericdata.getSequenceByName(sequenceName);
            RegionSequenceData regionSequence=(regiondata==null)?null:(RegionSequenceData)regiondata.getSequenceByName(sequenceName);
            
            // process single sequence here...
            ArrayList<Double> this_insideValues=new ArrayList<Double>();
            ArrayList<Double> this_outsideValues=(regiondata==null)?null:new ArrayList<Double>();  
            double this_insideMax=-Double.MAX_VALUE;
            double this_insideMin=Double.MAX_VALUE;
            double this_outsideMax=-Double.MAX_VALUE;        
            double this_outsideMin=Double.MAX_VALUE;   
            double this_insideSum=0;
            double this_outsideSum=0;
            int this_insideRegionsBaseCount=0;
            int this_outsideRegionsBaseCount=0;
            int[] this_insideBins=new int[bins];
            int[] this_outsideBins=new int[bins];   
            
            int buffersize=10000;          
            int sequenceSize=numericSequence.getSize();
            if (sequenceSize<buffersize) buffersize=sequenceSize;
            int[] buffer=(regiondata==null)?null:new int[buffersize];     
            int bufferstart=0;
            for (int pos=0;pos<sequenceSize;pos++) {
                if (pos%buffersize==0 && buffer!=null) { // flatten new segment
                    int bufferend=bufferstart+buffersize-1;
                    if (bufferend>sequenceSize-1) bufferend=sequenceSize-1;
                    buffer=regionSequence.flattenSegment(buffer, bufferstart, bufferend);
                    bufferstart+=buffersize;
                }
                double value=numericSequence.getValueAtRelativePosition(pos);
                boolean inside=(regiondata==null)?true:(buffer[pos%buffersize]>0);
                int bin=(int)((value-min)/binrange);
                if (bin>=bins) bin=bins-1;
                if (inside) {
                    this_insideRegionsBaseCount++;
                    this_insideBins[bin]++;
                    this_insideSum+=value;
                    this_insideValues.add(value);
                    if (value>this_insideMax) this_insideMax=value;
                    if (value<this_insideMin) this_insideMin=value;
                } else {
                    this_outsideRegionsBaseCount++;
                    this_outsideBins[bin]++;
                    this_outsideSum+=value;
                    this_outsideValues.add(value);
                    if (value>this_outsideMax) this_outsideMax=value;
                    if (value<this_outsideMin) this_outsideMin=value;                     
                }    
                //if (bs!=null && bs>0) samples.add(new Double[]{value,(inside)?1.0:2.0,0.0});
            }                             
            
            synchronized(counters) { // finished one of the sequences. Add the results to the global counters
                counters[1]++; // number of sequences completed               
                insideRegionsBaseCount+=this_insideRegionsBaseCount;
                outsideRegionsBaseCount+=this_outsideRegionsBaseCount;
                insideSum+=this_insideSum;
                outsideSum+=this_outsideSum;                
                if (this_outsideMax>outsideMax) outsideMax=this_outsideMax;
                if (this_insideMax>insideMax) insideMax=this_insideMax;
                if (this_outsideMin<outsideMin) outsideMin=this_outsideMin;
                if (this_insideMin<insideMin) insideMin=this_insideMin;               
                insideValues.addAll(this_insideValues);
                if (outsideValues!=null && this_outsideValues!=null) outsideValues.addAll(this_outsideValues);              
                for (int b=0;b<bins;b++) {
                    insideBins[b]+=this_insideBins[b];
                    if (outsideBins!=null) outsideBins[b]+=this_outsideBins[b];                            
                }
                task.setStatusMessage("Executing analysis: "+getAnalysisName()+"  ("+counters[1]+"/"+counters[2]+")");
                task.setProgress(counters[1],counters[2]);                              
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();    
            return sequenceName;
        }   
    }     
    
    
    /** Draws a histogram in a text document using different characters */
    private String drawHistogram() {
        StringBuilder string=new StringBuilder();
        int[][] histogram=getHistogram();
        //System.err.println("Histogramsize="+histogram.length+"x"+histogram[0].length);
        for (int i=0;i<histogram.length;i++) {
            string.append("|");
            for (int j=0;j<histogram[i].length;j++) {
                int val=histogram[i][j];
                     if (val==0) string.append(" ");
                else if (val==1) string.append("#");
                else if (val==2) string.append(".");
                else if (val==3) string.append("*");
            }     
            string.append("\n");
        }
        string.append("-----------------------------------------------------------------------------------");
        return string.toString();
    }
    
    /** Returns an Nx2 or Nx3 matrix containing [X,Y] pairs or [X,Yinside,Youtside] triplets for the histogram */
    private double[][] getHistogramValues() {
        double maxgraphvalue=0;        
        int totalbasecount=outsideRegionsBaseCount+insideRegionsBaseCount;
        double[] outsideNormalized=null;
        if (regionDatasetName!=null) {
            outsideNormalized=new double[outsideBins.length];
            for (int i=0;i<outsideBins.length;i++) {
                outsideNormalized[i]=(double)outsideBins[i]/((doNormalize)?(double)outsideRegionsBaseCount:(double)(totalbasecount));
                if (outsideNormalized[i]>maxgraphvalue) maxgraphvalue=outsideNormalized[i];
            }            
        }
        double[] insideNormalized=new double[insideBins.length];
        for (int i=0;i<insideBins.length;i++) {
            insideNormalized[i]=(double)insideBins[i]/((doNormalize)?(double)insideRegionsBaseCount:(double)(totalbasecount));
            if (insideNormalized[i]>maxgraphvalue) maxgraphvalue=insideNormalized[i];
        }     
        double globalmin=(regionDatasetName==null)?insideMin:Math.min(outsideMin,insideMin); // the smallest value in the numericdataset
        double globalmax=(regionDatasetName==null)?insideMax:Math.max(outsideMax,insideMax); // the largest value in the numericdataset
        double xstep=(globalmax-globalmin)/insideBins.length;
        double[][] result=new double[insideBins.length][(outsideNormalized!=null)?3:2];
        for (int i=0;i<insideBins.length;i++) {
            result[i][0]=globalmin+i*xstep;
            result[i][1]=insideNormalized[i];
            if (outsideNormalized!=null) result[i][2]=outsideNormalized[i];
        }
        return result;
    }
    
    
    private int[][] getHistogram() {
         int[][] histogram=new int[30][outsideBins.length];
         double maxrelativeInside=0;
         double maxrelativeOutside=0;
         for (int i=0;i<outsideBins.length;i++) {
             int bincount1=outsideBins[i];
             int bincount2=insideBins[i];
             double relative1=bincount1/(double)(insideRegionsBaseCount+outsideRegionsBaseCount);
             double relative2=bincount2/(double)(insideRegionsBaseCount+outsideRegionsBaseCount);
             if (relative1>maxrelativeOutside) maxrelativeOutside=relative1;
             if (relative2>maxrelativeInside) maxrelativeInside=relative2;
         }
         for (int i=0;i<outsideBins.length;i++) {
             int bincount=outsideBins[i];
             double relative=bincount/(double)(insideRegionsBaseCount+outsideRegionsBaseCount);
             relative=relative/maxrelativeOutside;
             drawColumn(histogram, i, relative, 1);
         }
         for (int i=0;i<insideBins.length;i++) {
             int bincount=insideBins[i];
             double relative=bincount/(double)(insideRegionsBaseCount+outsideRegionsBaseCount);
             relative=relative/maxrelativeInside;
             drawColumn(histogram, i, relative, 2);
         }
        return histogram;
    }
    
    private void drawColumn(int[][] histogram, int pos, double percentage, int val) {
        int height=histogram.length;
        int fill=(val==0)?0:(int)(height*percentage)+1;
        if (fill>height) fill=height;
        for (int i=0;i<fill;i++) histogram[height-(i+1)][pos]+=val;
    }
    
    private Dimension saveGraphAsImage(File file, int plots, double scale, Color color1, Color color2, VisualizationSettings settings) throws IOException {
        Font tickFont=settings.getSystemFont("graph.tickFont");  
        Dimension tickDimension=Graph.getDimension("100.0%", tickFont, null);
        
        int graphheight=250; // height of graph in pixels (just the histogram);
        int graphwidth=500; // height of graph in pixels (just the histogram);
        int marginX=5;
        int translateX=tickDimension.width+marginX+10;  // 10 is ~size of tickmark (without label)        
        int width=translateX+graphwidth+marginX;
        int bottomSpace=10+10+tickDimension.height; // Space beneath the X-axis (should have room for tickmarks)        
        int height=graphheight+((plots==PLOT_NONE)?0:50)+bottomSpace; // image height          
        int translateY=(plots==PLOT_NONE)?10:60; // the Y coordinate for the top of the graph        
        
        
        // write the image to file
        if (file!=null) {
            if (!file.getName().endsWith(".png")) {
                    VectorGraphics2D g=null;
                    String filename=file.getName();
                         if (filename.endsWith(".svg")) g = new SVGGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
                    else if (filename.endsWith(".pdf")) g = new PDFGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
                    else if (filename.endsWith(".eps")) g = new EPSGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
                    g.setClip(0, 0, (int)Math.ceil(width*scale),(int)Math.ceil(height*scale));
                    paintGraphImage(g, width, height, graphwidth, graphheight, translateX, translateY, plots, scale, color1, color2, tickFont);       
                    FileOutputStream fileStream = new FileOutputStream(file);
                    try {
                        fileStream.write(g.getBytes());
                    } finally {
                        fileStream.close();
                    }
                    g.dispose();
            } else {
                BufferedImage image=new BufferedImage((int)Math.ceil(width*scale),(int)Math.ceil(height*scale), BufferedImage.TYPE_INT_RGB);
                Graphics2D g=image.createGraphics();  
                paintGraphImage(g, width, height, graphwidth, graphheight, translateX, translateY, plots, scale, color1, color2, tickFont);
                OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
                ImageIO.write(image, "png", output);
                output.close(); 
                g.dispose();
            }          
        }
        return new Dimension(width,height);        
    }    
    
    
   /** Creates a histogram chart based on the current data and saves it to file
    * @param file A file to save the image to
    * @param doNormalize If TRUE values will be normalized within each group 'inside' or 'outside'
    */
    private void paintGraphImage(Graphics2D g, int width, int height, int graphwidth, int graphheight, int translateX, int translateY, int plots, double scale, Color color1, Color color2, Font tickFont) throws IOException {      
        int numbins=insideBins.length;
        double maxgraphvalue=0;

        g.scale(scale, scale);
        Stroke defaultStroke=g.getStroke();
        //BasicStroke dashed = new BasicStroke(1f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3f}, 0f);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        // normalize bin counts
        int totalbasecount=outsideRegionsBaseCount+insideRegionsBaseCount;
        double[] outsideNormalized=null;
        if (regionDatasetName!=null) {
            outsideNormalized=new double[outsideBins.length];
            for (int i=0;i<outsideBins.length;i++) {
                outsideNormalized[i]=(double)outsideBins[i]/((doNormalize)?(double)outsideRegionsBaseCount:(double)(totalbasecount));
                if (outsideNormalized[i]>maxgraphvalue) maxgraphvalue=outsideNormalized[i];
            }            
        }
        double[] insideNormalized=new double[insideBins.length];
        for (int i=0;i<insideBins.length;i++) {
            insideNormalized[i]=(double)insideBins[i]/((doNormalize)?(double)insideRegionsBaseCount:(double)(totalbasecount));
            if (insideNormalized[i]>maxgraphvalue) maxgraphvalue=insideNormalized[i];
        }
        double globalmin=(regionDatasetName==null)?insideMin:Math.min(outsideMin,insideMin); // the smallest value in the numericdataset
        double globalmax=(regionDatasetName==null)?insideMax:Math.max(outsideMax,insideMax); // the largest value in the numericdataset
        double tickscaleY=0;
        if (maxgraphvalue<=0.01) {tickscaleY=0.001f;}
        else if (maxgraphvalue<=0.1) {tickscaleY=0.01f;}
        else if (maxgraphvalue<=0.5) {tickscaleY=0.05f;}
        else {tickscaleY=0.1f;}
        maxgraphvalue=(((int)(maxgraphvalue/tickscaleY))+1)*tickscaleY; // ceil to nearest X
        if (maxgraphvalue>1.0) maxgraphvalue=1.0f;
        Graph graph=new Graph(g, globalmin, globalmax, 0f, maxgraphvalue, graphwidth, graphheight, translateX, translateY);
//        g.setStroke(new BasicStroke(1f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new double[]{2f}, 0f));
//        g.setColor(Graph.lightGray);
//        graph.drawHorizontalGridLines();
//        graph.drawVerticalGridLines();
//        g.setStroke(defaultStroke);
        if (regionDatasetName!=null) {
            g.setColor(color2);
            graph.drawHistogram(outsideNormalized);
        }
        g.setColor(color1);
        graph.drawHistogram(insideNormalized);
        g.setColor(Color.BLACK);
        //g.drawRect(translateX, translateY, graphwidth, graphheight); // draw bounding box
        Font save=g.getFont();
        g.setFont(tickFont);
        graph.drawXaxisWithTicks(graphheight+translateY, false, false);
        graph.drawYaxisWithTicks(translateX, false, true);
        g.setFont(save);
        
        // box and whiskers   
        if (regionDatasetName!=null) {
            g.setColor(color2);
            if (plots==PLOT_MEDIAN_QUARTILES || plots==PLOT_BOTH) graph.drawHorizontalBoxAndWhiskers(outsideMin, outsideMax, outsideMedian, outsideFirstQuartile, outsideThirdQuartile, 25,7);
            if (plots==PLOT_MEAN_STD || plots==PLOT_BOTH) graph.drawHorizontalMeanAndStdDeviation(outsideAverage,outsideStdDev, 25,7);
            int averageoutsideXcoordinate=graph.getXforValue(outsideAverage);
            int medianoutsideXcoordinate=graph.getXforValue(outsideMedian);
            if (plots==PLOT_BOTH || plots==PLOT_MEAN_STD) {
                g.setStroke(Graph.DASHED_LONG_STROKE);
                g.drawLine(averageoutsideXcoordinate, 25+15, averageoutsideXcoordinate, graphheight+translateY+6);
                g.setStroke(defaultStroke);
            }
            if (plots==PLOT_BOTH || plots==PLOT_MEDIAN_QUARTILES) g.drawLine(medianoutsideXcoordinate, 25, medianoutsideXcoordinate, graphheight+translateY+6);        
        }
        g.setColor(color1);
        int averageinsideXcoordinate=graph.getXforValue(insideAverage);            
        int medianinsideXcoordinate=graph.getXforValue(insideMedian);        
        if (plots==PLOT_MEDIAN_QUARTILES || plots==PLOT_BOTH) graph.drawHorizontalBoxAndWhiskers(insideMin, insideMax, insideMedian, insideFirstQuartile, insideThirdQuartile, 0,7);
        if (plots==PLOT_MEAN_STD || plots==PLOT_BOTH) graph.drawHorizontalMeanAndStdDeviation(insideAverage, insideStdDev, 0,7);
        if (plots==PLOT_BOTH || plots==PLOT_MEAN_STD) {
            g.setStroke(Graph.DASHED_LONG_STROKE);
            g.drawLine(averageinsideXcoordinate, 15, averageinsideXcoordinate, graphheight+translateY+6);
            g.setStroke(defaultStroke);
        }
        if (plots==PLOT_BOTH || plots==PLOT_MEDIAN_QUARTILES) g.drawLine(medianinsideXcoordinate, 0, medianinsideXcoordinate, graphheight+translateY+6);
    }



    private String formatNumber(double number) {
        return Graph.formatNumber(number, false);
    }

     
  
    /** Calculates Mann-Whitney U statistics (u1 and u2) for a list of samples in sorted order (ascending)
     * Description of Double[] samples
     * [0] => the sample value (numeric data track value for a point)
     * [1] => the sample number (1f == inside regions, 2f == outside regions)
     * [2] => holds the ranks that are assigned in this method
     */
    private double[] MannWhitneyU(ArrayList<Double[]> samples) {
        //Collections.sort(samples,comparator);
        double lastValue=samples.get(0)[0];
        int lastpos=0;
        for (int i=1;i<samples.size();i++) {
            double newValue=samples.get(i)[0];
            if (newValue!=lastValue) {
                if (i==lastpos+1) {samples.get(lastpos)[2]=(lastpos+1.0);}
                else {
                    double rank=0;
                    for (int j=lastpos;j<i;j++) rank+=(j+1f);
                    rank=rank/(i-lastpos);
                    for (int j=lastpos;j<i;j++) samples.get(j)[2]=rank;
                }  
                lastpos=i;lastValue=newValue;
            }
        }
        int i=samples.size();
        if (i==lastpos+1) {samples.get(lastpos)[2]=(lastpos+1.0);}
        else {
            double rank=0;
            for (int j=lastpos;j<i;j++) rank+=(j+1f);
            rank=rank/(i-lastpos);
            for (int j=lastpos;j<i;j++) samples.get(j)[2]=rank;
        }           
        double w1=0;
        double w2=0;
        int n1=0;
        int n2=0;
        for (i=0;i<samples.size();i++) {
            Double[] sample=samples.get(i);
            if (sample[1]==1f) {// sample[1] is the sample number (n1 or n2)
                n1++;
                w1+=sample[2]; 
            } else {
                n2++;
                w2+=sample[2]; 
            }
        }
        double u1=w1-(n1*(n1+1)/2f);
        double u2=w2-(n2*(n2+1)/2f);
//        System.err.println("n1="+n1+", w1="+w1+"   u1="+u1+"   reciprocal="+(( (n1+n2)*(n1+n2+1)/2f  )-w2));        
//        System.err.println("n2="+n2+", w2="+w2+"   u2="+u2+"   reciprocal="+(( (n1+n2)*(n1+n2+1)/2f  )-w1));        
        return new double[]{u1,u2};
    } 
    
    
//    private class SampleComparator implements Comparator<Double[]> {
//            @Override
//            public int compare(Double[] sample1, Double[] sample2) {
//                return sample1[0].compareTo(sample2[0]);
//            }    
//    }
//    
//    
//    private void bootstrapValues(ArrayList<Double[]> values, int bootstrapsamples, int n1, int n2, double u1, double u2, OperationTask task) throws InterruptedException {
//        ArrayList<Integer> index=new ArrayList<Integer>(values.size());
//        for (int i=0;i<values.size();i++) index.add(new Integer(i));        
//        // shuffle the labels (instead of the values)
//        int moreextremeU1count=0; // more extreme is less than or equal to
//        int moreextremeU2count=0; // more extreme is less than or equal to
//        int moreextremeUcount=0; // more extreme is less than or equal to
//        double u = Math.min(u1, u2);
//        for (int k=0;k<bootstrapsamples;k++) {
//            if (k%100==0) {
//                task.checkExecutionLock(); // checks to see if this task should suspend execution
//                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
//                task.setStatusMessage("Estimating statistical significance ("+(int)(k*100/bootstrapsamples)+"%)");
//                task.setProgress(k, bootstrapsamples); //      
//            }
//            Collections.shuffle(index);
//            for (int i=0;i<index.size();i++) {
//                int mapping=index.get(i);
//                if (i<n1) values.get(mapping)[1]=1.0;
//                else values.get(mapping)[1]=2.0;
//            }        
//            double[] U=MannWhitneyU(values);
//            double testu = Math.min(U[0], U[1]);
//            //if (k%100==0) System.err.println("U="+testu+"    U1="+U[0]+"   U2="+U[1]);
//            if (U[0]<=u1) moreextremeU1count++;
//            if (U[1]<=u2) moreextremeU2count++;
//            if (testu<=u) moreextremeUcount++;
//        }
//        double p1=moreextremeU1count/(double)bootstrapsamples;
//        double p2=moreextremeU2count/(double)bootstrapsamples;
//        double p=moreextremeUcount/(double)bootstrapsamples;
//        System.err.println("U1="+u1+"   p1="+p1+"  moreextreme="+moreextremeU1count);        
//        System.err.println("U2="+u2+"   p2="+p2+"  moreextreme="+moreextremeU2count);        
//        System.err.println("U="+u+"   p="+p+"  moreextreme="+moreextremeUcount);      
//    } 


    @Override
    protected JPanel getDisplayPanel(MotifLabGUI gui, boolean modal) {
        OutputData document=new OutputData("temp");
        try {document=formatHTML(document, gui.getEngine(), null, null, null);}
        catch (Exception e) {document.append("ERROR:"+e.getMessage(), HTML);}
        document.setShowAsHTML(true);
        OutputPanel panel=new OutputPanel(document, gui);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8,5,0,5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)
        ));
        panel.setPreferredSize(new Dimension(820,600));
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

}
