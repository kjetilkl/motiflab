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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import motiflab.engine.data.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.Graph;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Parameter;
import motiflab.engine.TaskRunner;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.util.Excel_PieChartMaker;
import motiflab.gui.VisualizationSettings;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author kjetikl
 */
public class CompareRegionDatasetsAnalysis extends Analysis {
    private final static String typedescription="Analysis: compare region datasets";
    private final static String analysisName="compare region datasets";
    private final static String description="Compares one Region Dataset to another and returns nucleotide level statistics of their similarity";
    private int nTP=0,nFP=0,nFN=0,nTN=0;
    private int sTP=0,sFP=0,sFN=0,sTN=0; // reserved for later use...
    HashMap<String,Double> storage=new HashMap<String, Double>();    
    private double siteOverlapFraction=0.25; // minimum overlap required to call a True Positive on the site level     
    private String predictionTrackName=null;
    private String answerTrackName=null;
    private String sequenceCollectionName=null;
    private int sequenceCollectionSize=0;
    private final String[] variables = new String[]{"nTP","nFP","nTN","nFN","nSn","nSp","nPPV","nNPV","nASP","nPC","nF","nAcc","nCC"};
    
    public CompareRegionDatasetsAnalysis() {
        this.name="CompareRegionDatasetsAnalysis_temp";
        addParameter("First",RegionDataset.class, null,new Class[]{RegionDataset.class},"A 'prediction dataset'",true,false);
        addParameter("Second",RegionDataset.class, null,new Class[]{RegionDataset.class},"A second region dataset that the first dataset will be compared against ('answer dataset')",true,false);
        addParameter("Sequences",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"If provided, the analysis will be limited to the sequences in the given collection",false,false);
    }  
    
    /** Returns a list of output parameters that can be set when an Analysis is output */
    @Override
    public Parameter[] getOutputParameters(String dataformat) {
         Parameter imageformat=new Parameter("Image format",String.class, "png",new String[]{"png","svg","pdf","eps"},"The image format to use for the graph",false,false);                              
         Parameter scalepar=new Parameter("Graph scale",Integer.class,100,new Integer[]{10,2000},"Scale of graphics plot (in percent)",false,false);
         if (dataformat.equals(HTML)) return new Parameter[]{imageformat,scalepar};
         else return new Parameter[0];
    }    
    
//    @Override
//    public String[] getOutputParameterFilter(String parameter) {
//        if (parameter.equals("Graph scale") || parameter.equals("Image format")) return new String[]{"HTML"};
//        return null;
//    }     
    
    @Override
    public String[] getSourceProxyParameters() {return new String[]{"First","Second"};} 
    
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
             if (variablename.equals("nTP")) return new NumericVariable("result", nTP);
        else if (variablename.equals("nFP")) return new NumericVariable("result", nFP);
        else if (variablename.equals("nTN")) return new NumericVariable("result", nTN);
        else if (variablename.equals("nFN")) return new NumericVariable("result", nFN);
        if (storage!=null && storage.containsKey(variablename)) return new NumericVariable("result", storage.get(variablename));
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }
        
    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else return NumericVariable.class; // all exported values in this analysis are numerical
    }    
    
    
    private double getStatistic(String name) {
        if (storage!=null && storage.containsKey(name)) return storage.get(name);
        else return Double.NaN;
    }
    
    @Override
    @SuppressWarnings("unchecked")    
    public CompareRegionDatasetsAnalysis clone() {
        CompareRegionDatasetsAnalysis newanalysis=new CompareRegionDatasetsAnalysis();
        super.cloneCommonSettings(newanalysis); 
        newanalysis.storage=(HashMap<String,Double>)this.storage.clone();         
        newanalysis.nTP=this.nTP; 
        newanalysis.nFP=this.nFP; 
        newanalysis.nTN=this.nTN; 
        newanalysis.nFN=this.nFN;
        newanalysis.sTP=this.sTP; 
        newanalysis.sFP=this.sFP; 
        newanalysis.sTN=this.sTN; 
        newanalysis.sFN=this.sFN;    
        newanalysis.siteOverlapFraction=this.siteOverlapFraction;
        newanalysis.predictionTrackName=this.predictionTrackName;   
        newanalysis.answerTrackName=this.answerTrackName;
        newanalysis.sequenceCollectionName=this.sequenceCollectionName;
        newanalysis.sequenceCollectionSize=this.sequenceCollectionSize;
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        this.storage=((CompareRegionDatasetsAnalysis)source).storage;
        this.nTP=((CompareRegionDatasetsAnalysis)source).nTP;
        this.nFP=((CompareRegionDatasetsAnalysis)source).nFP;
        this.nTN=((CompareRegionDatasetsAnalysis)source).nTN;
        this.nFN=((CompareRegionDatasetsAnalysis)source).nFN;
        this.sTP=((CompareRegionDatasetsAnalysis)source).sTP;
        this.sFP=((CompareRegionDatasetsAnalysis)source).sFP;
        this.sTN=((CompareRegionDatasetsAnalysis)source).sTN;
        this.sFN=((CompareRegionDatasetsAnalysis)source).sFN;
        this.siteOverlapFraction=((CompareRegionDatasetsAnalysis)source).siteOverlapFraction;        
        this.predictionTrackName=((CompareRegionDatasetsAnalysis)source).predictionTrackName;
        this.answerTrackName=((CompareRegionDatasetsAnalysis)source).answerTrackName;
        this.sequenceCollectionName=((CompareRegionDatasetsAnalysis)source).sequenceCollectionName;
        this.sequenceCollectionSize=((CompareRegionDatasetsAnalysis)source).sequenceCollectionSize;
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
        double nSn=getStatistic("nSn");
        double nSp=getStatistic("nSp");
        double nPPV=getStatistic("nPPV");
        double nNPV=getStatistic("nNPV");
        double nPC=getStatistic("nPC");
        double nASP=getStatistic("nASP");   
        double nF=getStatistic("nF");          
        double nAcc=getStatistic("nAcc");
        double nCC=getStatistic("nCC");
        int scalepercent=100;
        String imageFormat="png";
        if (format!=null) format.setProgress(5);
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             imageFormat=(String)settings.getResolvedParameter("Image format",defaults,engine);              
             scalepercent=(Integer)settings.getResolvedParameter("Graph scale",defaults,engine);
          } 
          catch (ExecutionError e) {throw e;} 
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        } 
        double scale=(scalepercent==100)?1.0:(((double)scalepercent)/100.0);
        File imagefile=outputobject.createDependentFile(engine,imageFormat);
        Object dimension=null;
        try {
            dimension=createGraphImage(imagefile, scale, engine.getClient().getVisualizationSettings());
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        if (format!=null) format.setProgress(50);
        DecimalFormat formatter=new DecimalFormat("#.###");
        DecimalFormat decimalFormatter=new DecimalFormat("#.#");         
        engine.createHTMLheader("Compare Region Datasets Analysis", null, null, true, true, true, outputobject);
        double total=nTP+nFP+nTN+nFN;
        outputobject.append("<div align=\"center\">\n<h2 class=\"headline\">Comparing '"+predictionTrackName+"' against '"+answerTrackName+"'</h2>\n",HTML);
        outputobject.append("Analysis based on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),HTML);
        if (sequenceCollectionName!=null) outputobject.append(" from collection <span class=\"dataitem\">"+sequenceCollectionName+"</span>",HTML);
        outputobject.append("<br><br>\n<h2>Base Counts</h2>\n<table class=\"sortable\">\n",HTML);
        outputobject.append("<tr><th width=\"60\">TP</th><th width=\"60\">FP</th><th width=\"60\">TN</th><th width=\"60\">FN</th></tr>\n",HTML);
        outputobject.append("<tr><td style=\"text-align:center\">"+nTP+"</td><td style=\"text-align:center\">"+nFP+"</td><td style=\"text-align:center\">"+nTN+"</td><td style=\"text-align:center\">"+nFN+"</td></tr>",HTML);
        outputobject.append("<tr><td style=\"text-align:center\">"+decimalFormatter.format(nTP/total*100)+"%</td><td style=\"text-align:center\">"+decimalFormatter.format(nFP/total*100)+"%</td><td style=\"text-align:center\">"+decimalFormatter.format(nTN/total*100)+"%</td><td style=\"text-align:center\">"+decimalFormatter.format(nFN/total*100)+"%</td></tr>\n",HTML);
        outputobject.append("</table>\n<br />\n",HTML);
        
        outputobject.append("<h2>Statistics</h2>\n<table class=\"sortable\">\n",HTML);
        outputobject.append("<tr><th width=\"60\">SN</th><th width=\"60\">SP</th><th width=\"60\">PPV</th><th width=\"60\">NPV</th><th width=\"60\">PC</th><th width=\"60\">ASP</th><th width=\"60\">F</th><th width=\"60\">Acc</th><th width=\"60\">CC</th></tr>\n",HTML);
        outputobject.append("<tr><td style=\"text-align:center\">"+((Double.isNaN(nSn))?"-":formatter.format(nSn))+"</td><td style=\"text-align:center\">"+((Double.isNaN(nSp))?"-":formatter.format(nSp))+"</td><td style=\"text-align:center\">"+((Double.isNaN(nPPV))?"-":formatter.format(nPPV))+"</td><td style=\"text-align:center\">"+((Double.isNaN(nNPV))?"-":formatter.format(nNPV))+"</td><td style=\"text-align:center\">"+((Double.isNaN(nPC))?"-":formatter.format(nPC))+"</td>"
                + "<td style=\"text-align:center\">"+((Double.isNaN(nASP))?"-":formatter.format(nASP))+"</td>"
                + "<td style=\"text-align:center\">"+((Double.isNaN(nF))?"-":formatter.format(nF))+"</td>"
                + "<td style=\"text-align:center\">"+((Double.isNaN(nAcc))?"-":formatter.format(nAcc))+"</td><td style=\"text-align:center\">"+((Double.isNaN(nCC))?"-":formatter.format(nCC))+"</td></tr>\n</table>\n",HTML);
        outputobject.append("<br><br>\n",HTML);
        if (imageFormat.equals("pdf")) outputobject.append("<object type=\"application/pdf\" data=\"file:///"+imagefile.getAbsolutePath()+"\"></object>",HTML);
        else {
            outputobject.append("<img src=\"file:///"+imagefile.getAbsolutePath()+"\"",HTML);
            if (dimension instanceof Dimension) {
                int width=((Dimension)dimension).width;
                int height=((Dimension)dimension).height;
                outputobject.append(" width="+(int)Math.ceil(width*scale)+" height="+(int)Math.ceil(height*scale),HTML);
            }                    
            outputobject.append(" />\n<br>\n",HTML);  
        }                    
        outputobject.append("<br>\n",HTML);
        outputobject.append(decimalFormatter.format(nPPV*100)+"% of <span class=\"dataitem\">"+predictionTrackName+"</span> overlaps with <span class=\"dataitem\">"+answerTrackName+"</span> (PPV)<br>\n",HTML);
        outputobject.append(decimalFormatter.format(nSn*100)+"% of <span class=\"dataitem\">"+answerTrackName+"</span> overlaps with <span class=\"dataitem\">"+predictionTrackName+"</span> (SN)&nbsp;<br>\n",HTML);
        outputobject.append("The relative overlap is "+decimalFormatter.format(nPC*100)+"% with respect to both tracks (Performance Coefficient)<br>",HTML);
        outputobject.append("</div>\n",HTML);
        outputobject.append("</body>\n</html>\n",HTML);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }
    
    private Object createGraphImage(File file, double scale, VisualizationSettings settings) throws IOException {
        String[] legends=new String[]{"Overlap (TP)","Unique to "+predictionTrackName+" (FP)","Unique to "+answerTrackName+" (FN)","Background (TN)"};
        Font font=settings.getSystemFont("graph.legendFont");
        Dimension legendsdim=Graph.getLegendDimension(legends, font);
        int pieChartSize=160; //
        int pieMargin=40;
        int margin=5;
        int width=pieChartSize+legendsdim.width+pieMargin+margin+margin;
        int height=Math.max(pieChartSize,legendsdim.height)+margin+margin; // image height
        
        if (file!=null) {
            if (file.getName().endsWith(".png")) { // bitmap PNG format   
                BufferedImage image=new BufferedImage((int)Math.ceil(width*scale),(int)Math.ceil(height*scale), BufferedImage.TYPE_INT_RGB);
                Graphics2D g=image.createGraphics();   
                paintGraphImage(g, scale, width, height, pieChartSize, margin, pieMargin, legends, legendsdim, settings);      
                OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
                ImageIO.write(image, "png", output);
                output.close(); 
                g.dispose();
                return new Dimension(width,height);                                
            } else { // vector format      
                VectorGraphics2D g=null;
                String filename=file.getName();
                     if (filename.endsWith(".svg")) g = new SVGGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
                else if (filename.endsWith(".pdf")) g = new PDFGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
                else if (filename.endsWith(".eps")) g = new EPSGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
                g.setClip(0, 0, (int)Math.ceil(width*scale),(int)Math.ceil(height*scale));
                paintGraphImage(g, scale, width, height, pieChartSize, margin, pieMargin, legends, legendsdim, settings);                     
                FileOutputStream fileStream = new FileOutputStream(file);
                try {
                    fileStream.write(g.getBytes());
                } finally {
                    fileStream.close();
                } 
                return new Dimension(width,height); 
            }
        } else { // No output file. Create the image as a byte[] array for inclusion in Excel
            BufferedImage image=new BufferedImage((int)Math.ceil(width*scale),(int)Math.ceil(height*scale), BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();   
            paintGraphImage(g, scale, width, height, pieChartSize, margin, pieMargin, legends, legendsdim, settings);
            g.dispose();
            return image;                        
        }        
    }  
    

    private void paintGraphImage(Graphics2D g, double scale, int width, int height, int pieChartSize, int margin, int pieMargin, String[] legends, Dimension legendsdim, VisualizationSettings settings) throws IOException {
        Color firstTrackColor=settings.getForeGroundColor(predictionTrackName);
        Color secondTrackColor=settings.getForeGroundColor(answerTrackName);
        Color blendColor=VisualizationSettings.blendColors(firstTrackColor, secondTrackColor);
        Color[] colors=new Color[]{secondTrackColor,blendColor,firstTrackColor,Color.WHITE};
        
        g.scale(scale, scale);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.drawRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // draw piechart
        int translateY=margin;
        int legendY=(int)((height-legendsdim.height)/2)+margin;
        if (legendsdim.height>pieChartSize) {
            translateY=(int)((legendsdim.height-pieChartSize)/2.0)+margin;
            legendY=margin;
        }
        Graph piechart=new Graph(g, 0, 1, 0, 1, pieChartSize, pieChartSize, margin, translateY);            
        piechart.drawPieChart(new double[]{nFN,nTP, nFP,nTN}, colors, true); 
        
        Font legendFont=settings.getSystemFont("graph.legendFont");
        g.setFont(legendFont);
        piechart.drawLegendBox(legends, new Color[]{blendColor,firstTrackColor,secondTrackColor,Color.WHITE}, pieChartSize+pieMargin, legendY, true);
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }    
    
  
    
    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        double nSn=getStatistic("nSn");
        double nSp=getStatistic("nSp");
        double nPPV=getStatistic("nPPV");
        double nNPV=getStatistic("nNPV");
        double nPC=getStatistic("nPC");
        double nASP=getStatistic("nASP");
        double nF=getStatistic("nF");        
        double nAcc=getStatistic("nAcc");
        double nCC=getStatistic("nCC");        
        outputobject.append("#Comparing '"+predictionTrackName+"' against '"+answerTrackName+"'\n",RAWDATA);
        outputobject.append("#Analysis based on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),RAWDATA);
        if (sequenceCollectionName!=null) outputobject.append(" from collection '"+sequenceCollectionName+"'",RAWDATA);
        outputobject.append("\n",RAWDATA);
        outputobject.append("nTP="+nTP+"\n",RAWDATA);
        outputobject.append("nFP="+nFP+"\n",RAWDATA);
        outputobject.append("nTN="+nTN+"\n",RAWDATA);
        outputobject.append("nFN="+nFN+"\n",RAWDATA);
        outputobject.append("nSn="+((Double.isNaN(nSn))?"-":nSn)+"\n",RAWDATA);
        outputobject.append("nSp="+((Double.isNaN(nSp))?"-":nSp)+"\n",RAWDATA);
        outputobject.append("nPPV="+((Double.isNaN(nPPV))?"-":nPPV)+"\n",RAWDATA);
        outputobject.append("nNPV="+((Double.isNaN(nNPV))?"-":nNPV)+"\n",RAWDATA);
        outputobject.append("nPC="+((Double.isNaN(nPC))?"-":nPC)+"\n",RAWDATA);
        outputobject.append("nASP="+((Double.isNaN(nASP))?"-":nASP)+"\n",RAWDATA);
        outputobject.append("nF="+((Double.isNaN(nASP))?"-":nF)+"\n",RAWDATA);
        outputobject.append("nAcc="+((Double.isNaN(nAcc))?"-":nAcc)+"\n",RAWDATA);
        outputobject.append("nCC="+((Double.isNaN(nCC))?"-":nCC)+"\n",RAWDATA);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }
    
    @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        VisualizationSettings vizSettings = engine.getClient().getVisualizationSettings();       
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Compare Region Datasets");
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        
        CellStyle titleStyle = getExcelTitleStyle(workbook);
        CellStyle tableheaderStyle = getExcelTableHeaderStyle(workbook);
        CellStyle tableCellStyle = createExcelStyle(workbook, BorderStyle.THIN, null, HorizontalAlignment.CENTER, false);
        CellStyle tableCellPercentageStyle = createExcelStyle(workbook, BorderStyle.THIN, null, HorizontalAlignment.CENTER, false); 
                  tableCellPercentageStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));        
        CellStyle headerStyle = getExcelHeaderStyle(workbook, 1.5, false, false);
        CellStyle percentageStyle = getExcelPercentageStyle(workbook);
        CellStyle statisticsStyle = createExcelStyle(workbook, BorderStyle.THIN, null, HorizontalAlignment.CENTER, false); 
                  statisticsStyle.setDataFormat(workbook.createDataFormat().getFormat("0.000"));             
        

        
        // Output summary statistics on first sheet
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("Comparing \""+predictionTrackName+"\" against \""+answerTrackName+"\"");
        cell.setCellStyle(titleStyle);
        
        row = sheet.createRow(2);
        cell = row.createCell(1);
        String header2 = "Analysis based on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":"");
        cell.setCellValue(header2);
        
        row = sheet.createRow(5);
        cell = row.createCell(1);
        cell.setCellValue("Base Counts");
        cell.setCellStyle(headerStyle);
        
        row = sheet.createRow(6);
        int cellIndex=1;
        String[] headers = new String[]{"TP","FP","TN","FN"};
        for (String header:headers) {
            cell = row.createCell(cellIndex++);
            cell.setCellValue(header);
            cell.setCellStyle(tableheaderStyle);
        }
        int datarow = 7;
        row = sheet.createRow(datarow);
        cellIndex=1;
        cell = row.createCell(cellIndex++); cell.setCellValue(nTP); cell.setCellStyle(tableCellStyle);        
        cell = row.createCell(cellIndex++); cell.setCellValue(nFP); cell.setCellStyle(tableCellStyle);
        cell = row.createCell(cellIndex++); cell.setCellValue(nTN); cell.setCellStyle(tableCellStyle);
        cell = row.createCell(cellIndex++); cell.setCellValue(nFN); cell.setCellStyle(tableCellStyle);
                
        row = sheet.createRow(8); // percentages
        cellIndex=1;
        cell = row.createCell(cellIndex++); cell.setCellFormula("B8/(B8+C8+D8+E8)"); cell.setCellStyle(tableCellPercentageStyle); evaluator.evaluateFormulaCell(cell);        
        cell = row.createCell(cellIndex++); cell.setCellFormula("C8/(B8+C8+D8+E8)"); cell.setCellStyle(tableCellPercentageStyle); evaluator.evaluateFormulaCell(cell);
        cell = row.createCell(cellIndex++); cell.setCellFormula("D8/(B8+C8+D8+E8)"); cell.setCellStyle(tableCellPercentageStyle); evaluator.evaluateFormulaCell(cell);
        cell = row.createCell(cellIndex++); cell.setCellFormula("E8/(B8+C8+D8+E8)"); cell.setCellStyle(tableCellPercentageStyle); evaluator.evaluateFormulaCell(cell);
               
        row = sheet.createRow(11);
        cell = row.createCell(1);
        cell.setCellValue("Statistics");
        cell.setCellStyle(headerStyle);        
              
        row = sheet.createRow(12);
        cellIndex=1;
        headers = new String[]{"Sn","Sp","PPV","NPV","PC","ASP","F","Acc","CC"};
        for (String header:headers) {
            cell = row.createCell(cellIndex++);
            cell.setCellValue(header);
            cell.setCellStyle(tableheaderStyle);
        }
        row = sheet.createRow(13);
        cellIndex=1;
        cell = row.createCell(cellIndex++); cell.setCellFormula("B8/(B8+E8)"); cell.setCellStyle(statisticsStyle); evaluator.evaluateFormulaCell(cell);  // Sensitivity      
        cell = row.createCell(cellIndex++); cell.setCellFormula("D8/(C8+D8)"); cell.setCellStyle(statisticsStyle); evaluator.evaluateFormulaCell(cell);  // Specificity 
        cell = row.createCell(cellIndex++); cell.setCellFormula("B8/(B8+C8)"); cell.setCellStyle(statisticsStyle); evaluator.evaluateFormulaCell(cell);  // Positive predictive value 
        cell = row.createCell(cellIndex++); cell.setCellFormula("D8/(D8+E8)"); cell.setCellStyle(statisticsStyle); evaluator.evaluateFormulaCell(cell);  // Negative predictive value 
        cell = row.createCell(cellIndex++); cell.setCellFormula("B8/(B8+C8+E8)"); cell.setCellStyle(statisticsStyle); evaluator.evaluateFormulaCell(cell);  // Performance coefficient      
        cell = row.createCell(cellIndex++); cell.setCellFormula("((B8/(B8+E8))+(B8/(B8+C8)))/2"); cell.setCellStyle(statisticsStyle); evaluator.evaluateFormulaCell(cell);  // Average Site Performance (Sn+PPv)/2
        cell = row.createCell(cellIndex++); cell.setCellFormula("(2*B8)/((2*B8)+C8+E8)"); cell.setCellStyle(statisticsStyle); evaluator.evaluateFormulaCell(cell);  // F-measure
        cell = row.createCell(cellIndex++); cell.setCellFormula("(B8+D8)/(B8+C8+D8+E8)"); cell.setCellStyle(statisticsStyle); evaluator.evaluateFormulaCell(cell);  // Accurracy
        cell = row.createCell(cellIndex++); cell.setCellFormula("((B8*D8)-(C8*E8))/SQRT((B8+C8)*(B8+E8)*(D8+C8)*(D8+E8))"); cell.setCellStyle(statisticsStyle); evaluator.evaluateFormulaCell(cell);  // Matthews Correlation Coefficient       
        
        for (int i=0;i<=9;i++) {
           sheet.setColumnWidth(i,3200); 
        }  
        
        double nSn=getStatistic("nSn");
        double nPPV=getStatistic("nPPV");
        double nPC=getStatistic("nPC");        
        cell = sheet.createRow(24).createCell(9); cell.setCellValue(nPPV); cell.setCellStyle(percentageStyle);
        cell = sheet.getRow(24).createCell(10);   cell.setCellValue(" of \""+predictionTrackName+"\" overlaps with \""+answerTrackName+"\" (PPV)");
  
        cell = sheet.createRow(25).createCell(9); cell.setCellValue(nSn); cell.setCellStyle(percentageStyle);
        cell = sheet.getRow(25).createCell(10);   cell.setCellValue(" of \""+answerTrackName+"\" overlaps with \""+predictionTrackName+"\" (Sn)");        

        cell = sheet.createRow(26).createCell(9); cell.setCellValue(nPC); cell.setCellStyle(percentageStyle);
        cell = sheet.getRow(26).createCell(10);   cell.setCellValue(" relative overlap with respect to both tracks (Performance Coefficient)");

        // pie chart
        
        Excel_PieChartMaker pieChart = new Excel_PieChartMaker(sheet, 1, 16, 7, 18);

        // boxplot.setupDataFromColumns(firstRow,rowIndex, 0,STATISTIC_MIN+offset,STATISTIC_MAX+offset,STATISTIC_MEDIAN+offset,STATISTIC_1ST_QUARTILE+offset,STATISTIC_3RD_QUARTILE+offset);    
        String[] labels = new String[]{"Unique to "+predictionTrackName+" (FP)", "Overlap (TP)", "Unique to "+answerTrackName+" (FN)","Background (TN)"};
        Color firstTrackColor=vizSettings.getForeGroundColor(predictionTrackName);
        Color secondTrackColor=vizSettings.getForeGroundColor(answerTrackName);
        Color blendColor=VisualizationSettings.blendColors(firstTrackColor, secondTrackColor);
        Color[] colors=new Color[]{firstTrackColor,blendColor,secondTrackColor,Color.WHITE};        
        pieChart.setupDataFromCells(new int[][]{ {datarow,2}, {datarow,1}, {datarow,4}, {datarow,3} }, labels, colors); // FP, TP, FN, TN
        pieChart.drawPieChart();   
        
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
    public void runAnalysis(OperationTask task) throws Exception {
        RegionDataset prediction=(RegionDataset)task.getParameter("First");        
        RegionDataset answer=(RegionDataset)task.getParameter("Second");
        predictionTrackName=prediction.getName();
        answerTrackName=answer.getName();
        SequenceCollection sequenceCollection=(SequenceCollection)task.getParameter("Sequences");
        if (sequenceCollection==null) sequenceCollection=task.getEngine().getDefaultSequenceCollection();
        if (sequenceCollection.getName().equals(task.getEngine().getDefaultSequenceCollectionName())) sequenceCollectionName=null;
        else sequenceCollectionName=sequenceCollection.getName();
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(task.getEngine());
        sequenceCollectionSize=sequences.size();

        TaskRunner taskRunner=task.getEngine().getTaskRunner();
        task.setProgress(0L,sequenceCollectionSize);
        long[] counters=new long[]{0,0,sequenceCollectionSize}; // counters[0]=motifs started, [1]=motifs completed, [2]=total number of motifs

        ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(sequenceCollectionSize);
        for (int i=0;i<sequenceCollectionSize;i++) {
            String sequenceName=sequences.get(i).getName();
            processTasks.add(new ProcessSequenceTask(prediction, answer, sequenceName, task, counters));
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
        if (countOK!=sequenceCollectionSize) {
            throw new ExecutionError("Some mysterious error occurred while performing analysis: "+getAnalysisName());
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
        storage.put("nSn",nSn);
        storage.put("nSp",nSp);
        storage.put("nPPV",nPPV);
        storage.put("nNPV",nNPV);
        storage.put("nPC",nPC);
        storage.put("nASP",nASP);
        storage.put("nF",nF);
        storage.put("nAcc",nAcc);
        storage.put("nCC",nCC);
        storage.put("sSn",sSn);
        storage.put("sPPV",sPPV);
        storage.put("sASP",sASP);
        storage.put("sPC",sPC);
        storage.put("sF",sF); 
    }
    
    private int[] processSequence(RegionSequenceData predictionSequence, RegionSequenceData answerSequence) {
        int[] results=countTrueFalse(predictionSequence, answerSequence);
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
            
            int[] results=processSequence(predictionSequence,answerSequence);           
            
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                nTP+=results[0];
                nFP+=results[1];
                nTN+=results[2];
                nFN+=results[3];
                if (results.length>=7) {
                    sTP=results[4];
                    sFP=results[5];
                    sFN=results[6];
                }   
                if (results.length>=8) {sTN=results[7];}
                task.setStatusMessage("Executing analysis: "+getAnalysisName()+"  ("+counters[1]+"/"+counters[2]+")");
                task.setProgress(counters[1],counters[2]);                              
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();    
            return sequenceName;
        }   
    }       
    
    
    
    /** count the number of TP, FP, TN and FN in a sequence*/
//    private int[] countTrueFalse_old(RegionSequenceData prediction, RegionSequenceData answer) {
//        int[] results= new int[]{0,0,0,0};
//        for (int i=0;i<prediction.getSize();i++) {
//            boolean hit1=prediction.isWithinRegion(i);
//            boolean hit2=answer.isWithinRegion(i);            
//                 if (hit1 && hit2) results[0]++; // a TP
//            else if (hit1 && !hit2) results[1]++; // a FP
//            else if (!hit1 && !hit2) results[2]++; // a TN
//            else if (!hit1 && hit2) results[3]++; // a FN
//        }
//        return results;
//    }
    
    /** count the number of TP, FP, TN and FN in a sequence */
    /*  This implementation is a new optimized version compared to the old method above. It flattens sections into int[] buffers
     *  which are then compared position by position by a tight loop, rather than calling isWithinRegion(pos) for each position.
     *  isWithinRegion(pos) is currently very slow since it always searches from the beginning of the region list to determine
     *  if the position is overlapped by a region which results in a O(n^2) running time and poor scaling
     */   
    private int[] countTrueFalse(RegionSequenceData prediction, RegionSequenceData answer) {
        int[] results= new int[]{0,0,0,0};
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
        return results;
    }       
    
    

    @Override
    protected Dimension getDefaultDisplayPanelDimensions() {
        return new Dimension(600,606);
    }




    // ------------ Serialization ---------
    private static final long serialVersionUID = 1L;

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
         short currentinternalversion=2; // this is an internal version number for serialization of objects of this type
         out.writeShort(currentinternalversion);
         out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
         short currentinternalversion=in.readShort(); // the internalversion number is used to determine correct format of data
         if (currentinternalversion==1) {
             in.defaultReadObject(); // this will fail...
         } else if (currentinternalversion==2) {
             in.defaultReadObject();
         } else if (currentinternalversion>2) throw new ClassNotFoundException("Newer version");
    }
    
}
