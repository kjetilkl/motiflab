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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import motiflab.engine.data.*;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.Graph;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.task.OperationTask;
import motiflab.gui.VisualizationSettings;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 *
 * @author kjetikl
 */
public class SingleMotifRegressionAnalysis extends Analysis {
    private final static String typedescription="Analysis: single motif regression";
    private final static String analysisName="single motif regression";
    private final static String description="Performs regression analysis of a single motif against gene expression (or other sequence related values)";
    private double[] statistics=null; // 
    private double[][] datapoints=null;
    private String sequenceCollectionName=null;  
    private String motifTrackName=null;
    private String sequenceMapName=null;
    private String motifName=null;
    private boolean ignoreMissing=false;
    private boolean normalize=false;    
    
    private static final int MOTIF_TOTAL_COUNT=0;
    private static final int MOTIF_TOTAL_SCORE=1;
    private static final int REGRESSION_COEFFICIENT=2;
    private static final int INTERCEPT=3;
    private static final int SIGNIFICANCE=4;
    private static final int PEARSONS_R=5;
    private static final int R_SQUARED=6;
    private static final int SLOPE_STDERR=7;
    private static final int INTERCEPT_STDERR=8;
    private static final int SUM_SQUARE_ERROR=9;
    private static final int MEAN_SQUARE_ERROR=10;
    private static final int REGRESSION_SUM_SQUARES=11;
    private static final int DATA_POINTS_USED=12;    
    
    public static final String PARAMETER_MOTIF_TRACK="Motif track";
    public static final String PARAMETER_MOTIF="Motif";
    public static final String PARAMETER_SEQUENCES="Sequences";
    public static final String PARAMETER_SEQUENCE_VALUES="Sequence values";
    public static final String PARAMETER_SKIP_NONREGULATED="Skip non-regulated";
    public static final String PARAMETER_NORMALIZE="Normalize";    

    public SingleMotifRegressionAnalysis() {
        this.name="SingleMotifRegressionAnalysis_temp";
        addParameter(PARAMETER_MOTIF_TRACK,RegionDataset.class, null,new Class[]{RegionDataset.class},"A track containing motif sites",true,false);
        addParameter(PARAMETER_MOTIF,Motif.class, null,new Class[]{Motif.class},"The single motif to use for the regression analysis",true,false);
        addParameter(PARAMETER_SEQUENCE_VALUES,SequenceNumericMap.class, null,new Class[]{SequenceNumericMap.class},"A numeric map containing sequence values to regress against (for instance gene expression values)",true,false);
        addParameter(PARAMETER_SEQUENCES,SequenceCollection.class, null,new Class[]{SequenceCollection.class},"If specified, the analysis will be limited to sequence in this collection",false,false);
        addParameter(PARAMETER_SKIP_NONREGULATED,Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Only include sequences where the motif actually occurs in the regression analysis",false,false);
        addParameter(PARAMETER_NORMALIZE,Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Normalize sequence motifscore range to [0,1]",false,false);
    }

   @Override
    public String[] getSourceProxyParameters() {return new String[]{PARAMETER_MOTIF_TRACK,PARAMETER_MOTIF,PARAMETER_SEQUENCE_VALUES};}
    
    /** Returns a list of output parameters that can be set when an Analysis is output */
    @Override
    public Parameter[] getOutputParameters() {   
         Parameter imageformat=new Parameter("Image format",String.class, "png",new String[]{"png","gif","svg","pdf","eps"},"The image format to use for the graph",false,false);            
         Parameter scalepar=new Parameter("Graph scale",Integer.class,100,new Integer[]{10,2000},"Scale of graphics plot (in percent)",false,false);
         return new Parameter[]{imageformat,scalepar};
    } 
    
    @Override
    public String[] getOutputParameterFilter(String parameter) {
        if (parameter.equals("Graph scale") || parameter.equals("Image format")) return new String[]{HTML};
        return null;
    }      

    @Override
    public String[] getResultVariables() {
        return new String[]{"Regression coefficient","Intercept","p-value","R","R2","MSE","SSE","SSR","Slope StdErr","Intercept StdErr","Motif count","Motif score sum"};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename==null || variablename.isEmpty()) throw new ExecutionError("'"+getName()+"' does not have a result for ''");
        else if (variablename.equals("Regression coefficient")) return new NumericVariable("temp",statistics[REGRESSION_COEFFICIENT]);
        else if (variablename.equals("Intercept")) return new NumericVariable("temp",statistics[INTERCEPT]);
        else if (variablename.equals("p-value")) return new NumericVariable("temp",statistics[SIGNIFICANCE]);
        else if (variablename.equals("R")) return new NumericVariable("temp",statistics[PEARSONS_R]);
        else if (variablename.equals("R2")) return new NumericVariable("temp",statistics[R_SQUARED]);
        else if (variablename.equals("MSE")) return new NumericVariable("temp",statistics[MEAN_SQUARE_ERROR]);
        else if (variablename.equals("SSE")) return new NumericVariable("temp",statistics[SUM_SQUARE_ERROR]);
        else if (variablename.equals("SSR")) return new NumericVariable("temp",statistics[REGRESSION_SUM_SQUARES]);
        else if (variablename.equals("Slope StdErr")) return new NumericVariable("temp",statistics[SLOPE_STDERR]);
        else if (variablename.equals("Intercept StdErr")) return new NumericVariable("temp",statistics[INTERCEPT_STDERR]);
        else if (variablename.equals("Motif count")) return new NumericVariable("temp",statistics[MOTIF_TOTAL_COUNT]);
        else if (variablename.equals("Motif score sum")) return new NumericVariable("temp",statistics[MOTIF_TOTAL_SCORE]);
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else return NumericVariable.class;
    }

    @Override
    public String getAnalysisName() {
        return analysisName;
    }

    @Override
    public String getDescription() {return description;}


    @Override
    @SuppressWarnings("unchecked")
    public SingleMotifRegressionAnalysis clone() {
        SingleMotifRegressionAnalysis newanalysis=new SingleMotifRegressionAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.statistics=this.statistics.clone();
        newanalysis.datapoints=this.datapoints.clone();
        newanalysis.sequenceCollectionName=this.sequenceCollectionName;
        newanalysis.motifName=this.motifName;
        newanalysis.motifTrackName=this.motifTrackName;
        newanalysis.sequenceMapName=this.sequenceMapName; 
        newanalysis.ignoreMissing=this.ignoreMissing;   
        newanalysis.normalize=this.normalize;         
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        SingleMotifRegressionAnalysis other=((SingleMotifRegressionAnalysis)source);
        this.statistics=other.statistics;
        this.datapoints=other.datapoints;
        this.sequenceCollectionName=other.sequenceCollectionName;
        this.motifName=other.motifName;
        this.motifTrackName=other.motifTrackName;
        this.sequenceMapName=other.sequenceMapName;    
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

    @Override
    protected Dimension getDefaultDisplayPanelDimensions() {
        return new Dimension(620,560);
    }    

    @Override
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        int scalepercent=100;
        String imageformat="png";
        if (format!=null) format.setProgress(5);
        if (settings!=null) {
          try {
                 Parameter[] defaults=getOutputParameters();
                 scalepercent=(Integer)settings.getResolvedParameter("Graph scale",defaults,engine);
                 imageformat=(String)settings.getResolvedParameter("Image format",defaults,engine);                 
          } 
          catch (ExecutionError e) {throw e;} 
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        } 
        double scale=(scalepercent==100)?1.0:(((double)scalepercent)/100.0);
        File imagefile=outputobject.createDependentFile(engine,imageformat);      
        Dimension dim=null;
        try {
            dim=saveGraphAsImage(imagefile, scale, imageformat, engine.getClient().getVisualizationSettings());
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        if (format!=null) format.setProgress(50);
        engine.createHTMLheader("Single Motif Regression Analysis", null, null, false, true, true, outputobject);
        outputobject.append("<div align=\"center\">\n",HTML);
        outputobject.append("<h2 class=\"headline\">Single Motif Regression Analysis</h2>\n",HTML);
        outputobject.append("<div class=\"summary\">\n",HTML);
        outputobject.append("The analysis was performed with motif <span class=\"dataitem\">"+motifName+"</span> from track <span class=\"dataitem\">"+motifTrackName+"</span>\n",HTML);
        outputobject.append("<br />\n",HTML);
        outputobject.append("Sequence values were taken from <span class=\"dataitem\">"+sequenceMapName+"</span>",HTML);
        if (sequenceCollectionName!=null) outputobject.append(" and limited to sequences in <span class=\"dataitem\">"+sequenceCollectionName+"</span>\n",HTML);
        outputobject.append("<br />\n",HTML);
        if (ignoreMissing) outputobject.append("Only sequences containing hits for the motif were included in the regression analysis<br />\n",HTML);
        outputobject.append("</div>\n",HTML);
        
        if (imageformat.equals("pdf")) outputobject.append("<object type=\"application/pdf\" data=\"file:///"+imagefile.getAbsolutePath()+"\"></object>",HTML);
        else {
            outputobject.append("<img src=\"file:///"+imagefile.getAbsolutePath()+"\"",HTML);
            if (dim!=null) {
                outputobject.append(" width="+(int)Math.ceil(dim.width*scale)+" height="+(int)Math.ceil(dim.height*scale),HTML);
            }                    
            outputobject.append(" />\n",HTML);  
        }        
        
        outputobject.append("<br />\n<br />\n<table>\n<tr>",HTML);
            outputobject.append("<th><i>&alpha;</i></th>",HTML);
            outputobject.append("<th><i>&beta;</i></th>",HTML);
            outputobject.append("<th><i>R</i></th>",HTML);
            outputobject.append("<th><i>R<sup>2</sup></i></th>",HTML);
            outputobject.append("<th><i>&alpha;</i>&nbsp;stdErr</th>",HTML);
            outputobject.append("<th><i>&beta;</i>&nbsp;stdErr</th>",HTML);
        outputobject.append("</tr>\n<tr>",HTML);
            outputobject.append("<td style=\"text-align:center\">"+formatNumber(statistics[INTERCEPT])+"</td>",HTML);
            outputobject.append("<td style=\"text-align:center\">"+formatNumber(statistics[REGRESSION_COEFFICIENT])+"</td>",HTML);
            outputobject.append("<td style=\"text-align:center\">"+formatNumber(statistics[PEARSONS_R])+"</td>",HTML);
            outputobject.append("<td style=\"text-align:center\">"+formatNumber(statistics[R_SQUARED])+"</td>",HTML);
            outputobject.append("<td style=\"text-align:center\">"+formatNumber(statistics[INTERCEPT_STDERR])+"</td>",HTML);
            outputobject.append("<td style=\"text-align:center\">"+formatNumber(statistics[SLOPE_STDERR])+"</td>",HTML);
        outputobject.append("</tr>\n</table>\n",HTML);
          outputobject.append("<br />\n<table>\n<tr>",HTML);
            outputobject.append("<th>MSE</th>",HTML);
            outputobject.append("<th>SSE</th>",HTML);
            outputobject.append("<th>SSR</th>",HTML);
            outputobject.append("<th>Sig</th>",HTML);
            outputobject.append("<th>Data&nbsp;points</th>",HTML);
        outputobject.append("</tr>\n<tr>",HTML);
            outputobject.append("<td style=\"text-align:center\">"+formatNumber(statistics[MEAN_SQUARE_ERROR])+"</td>",HTML);
            outputobject.append("<td style=\"text-align:center\">"+formatNumber(statistics[SUM_SQUARE_ERROR])+"</td>",HTML);
            outputobject.append("<td style=\"text-align:center\">"+formatNumber(statistics[REGRESSION_SUM_SQUARES])+"</td>",HTML);
            outputobject.append("<td style=\"text-align:center\">"+formatNumber(statistics[SIGNIFICANCE])+"</td>",HTML);
            outputobject.append("<td style=\"text-align:center\">"+formatNumber(statistics[DATA_POINTS_USED])+"</td>",HTML);
        outputobject.append("</tr>\n</table>\n",HTML);
        
        outputobject.append("</div>\n",HTML);
        outputobject.append("</body>\n</html>\n",HTML);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }

    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        if (format!=null) format.setProgress(5);
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setStatusMessage("Executing operation: output");
        outputobject.append("#Single motif regression analysis with motif '"+motifName+"' from track '"+motifTrackName+"'",RAWDATA);
        if (sequenceCollectionName!=null) outputobject.append(" in Sequence Collection '"+sequenceCollectionName+"'",RAWDATA);
        outputobject.append("\n#Sequence values were taken from '"+sequenceMapName+"'\n",RAWDATA);
        if (ignoreMissing) outputobject.append("#Only sequences containing hits for the motif were included in the regression analysis\n",RAWDATA);        
        outputobject.append("Regression coefficient (beta)\t"+statistics[REGRESSION_COEFFICIENT]+"\n",RAWDATA);
        outputobject.append("Intercept (alpha)\t"+statistics[INTERCEPT]+"\n",RAWDATA);
        outputobject.append("Regression coefficient StdErr\t"+statistics[SLOPE_STDERR]+"\n",RAWDATA);
        outputobject.append("Intercept StdErr\t"+statistics[INTERCEPT_STDERR]+"\n",RAWDATA);
        outputobject.append("R\t"+statistics[PEARSONS_R]+"\n",RAWDATA);
        outputobject.append("R^2\t"+statistics[R_SQUARED]+"\n",RAWDATA);
        outputobject.append("Mean Square Error\t"+statistics[MEAN_SQUARE_ERROR]+"\n",RAWDATA);
        outputobject.append("Sum Squared Errors\t"+statistics[SUM_SQUARE_ERROR]+"\n",RAWDATA);
        outputobject.append("Regression Sum Squares\t"+statistics[REGRESSION_SUM_SQUARES]+"\n",RAWDATA);
        outputobject.append("Significance\t"+statistics[SIGNIFICANCE]+"\n",RAWDATA);
        outputobject.append("Data points\t"+(int)statistics[DATA_POINTS_USED]+"\n",RAWDATA);     
        outputobject.append("\n\n\n",RAWDATA); 
            for (int i=0;i<datapoints.length;i++) {
                double x=datapoints[i][0];
                double y=datapoints[i][1];  
                if (! (Double.isNaN(x) || Double.isNaN(y))) {
                    outputobject.append(x+"\t"+y+"\n",RAWDATA);  
                }
            }         
        if (format!=null) format.setProgress(100);
        return outputobject;
    }

    private String formatNumber(double number) {
        return Graph.formatNumber(number, false);
    }    
    
    private Dimension saveGraphAsImage(File file, double scale, String imageformat, VisualizationSettings settings) throws IOException {
        double maxX=-Double.MAX_VALUE;
        double maxY=-Double.MAX_VALUE;
        double minX=Double.MAX_VALUE;
        double minY=Double.MAX_VALUE;
        for (int i=0;i<datapoints.length;i++) {
            if (!Double.isNaN(datapoints[i][0]) && datapoints[i][0]>maxX) maxX=datapoints[i][0];
            if (!Double.isNaN(datapoints[i][0]) && datapoints[i][0]<minX) minX=datapoints[i][0];
            if (!Double.isNaN(datapoints[i][0]) && datapoints[i][1]>maxY) maxY=datapoints[i][1];
            if (!Double.isNaN(datapoints[i][0]) && datapoints[i][1]<minY) minY=datapoints[i][1];
        }
        if (minX==Double.MAX_VALUE) minX=(maxX==-Double.MAX_VALUE)?0:maxX;
        if (maxX==-Double.MAX_VALUE) maxX=0;
        if (minY==Double.MAX_VALUE) minY=(maxY==-Double.MAX_VALUE)?0:maxY;
        if (maxY==-Double.MAX_VALUE) maxY=0;
        double xRange=maxX-minX; if (xRange==0) {maxX=minX+1;xRange=1;}
        double yRange=maxY-minY; if (yRange==0) {maxY=minY+1;yRange=1;}
        minX=minX-(xRange*0.05);
        maxX=maxX+(xRange*0.05);
        minY=minY-(yRange*0.05);
        maxY=maxY+(yRange*0.05);

        int graphheight=250; // height of graph in pixels (just the histogram);
        int graphwidth=350;
        
        int translateY=10; // the Y coordinate for the top of the graph

        // make a test to find the width of the vertical ticks        
        ArrayList<Double> ticks=Graph.getVerticalTicks(minY,maxY);
        BufferedImage testimage=new BufferedImage(10,10, BufferedImage.TYPE_INT_RGB);
        Graphics2D testg=testimage.createGraphics();
        Font tickFont=settings.getSystemFont("graph.tickFont");
        FontMetrics metrics=testg.getFontMetrics(tickFont);
        int tickswidth=0;
        int fontheight=(int)metrics.getStringBounds("Motif score for sequence", testg).getHeight();
        
        int decimals=Graph.getDecimalCount(ticks,false);
        for (double tick:ticks) {
            String text=Graph.formatNumber(tick, false, decimals);
            int width=metrics.stringWidth(text);
            if (width>tickswidth) tickswidth=width;
        }
        int translateX=tickswidth+fontheight+16; // make room for ticks and vertical label
        int width=graphwidth+translateX+20;
        int height=graphheight+translateY+(2*fontheight)+10; // image height
        if (imageformat.equals("png") || imageformat.equals("gif")) {
            BufferedImage image=new BufferedImage((int)Math.ceil(width*scale),(int)Math.ceil(height*scale), BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();
            paintGraphImage(g, scale, width, height, minX, maxX, minY, maxY, graphwidth, graphheight, translateX, translateY, settings);  
            // write the image to file
            if (file!=null) {
                OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
                ImageIO.write(image, imageformat, output);
                output.close(); 
            }
            g.dispose();
            return new Dimension(width,height); 
        } else {// vector format   
            VectorGraphics2D g=null;
            String filename=file.getName();
                 if (filename.endsWith(".svg")) g = new SVGGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
            else if (filename.endsWith(".pdf")) g = new PDFGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
            else if (filename.endsWith(".eps")) g = new EPSGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
            g.setClip(0, 0, (int)Math.ceil(width*scale),(int)Math.ceil(height*scale));
            paintGraphImage(g, scale, width, height, minX, maxX, minY, maxY, graphwidth, graphheight, translateX, translateY, settings);                                 
            FileOutputStream fileStream = new FileOutputStream(file);
            try {
                fileStream.write(g.getBytes());
            } finally {
                fileStream.close();
            } 
            g.dispose();
            return new Dimension(width,height); 
        }            
        
    }    
    
    private void paintGraphImage(Graphics2D g, double scale, int width, int height, double minX, double maxX, double minY, double maxY, int graphwidth, int graphheight, int translateX, int translateY, VisualizationSettings settings) {
        Color color1=settings.getSystemColor("color1");
        Color color2=settings.getSystemColor("color2");
        Font tickFont=settings.getSystemFont("graph.tickFont");
        int dotStyle=(Integer)settings.getSettingAsType("graph.dotStyle", 0);
        int dotSize=(Integer)settings.getSettingAsType("graph.dotSize", 2);
                
        g.scale(scale, scale);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width+2, height+2);
        g.setColor(java.awt.Color.BLACK);
        Graph graph=new Graph(g, minX, maxX, minY, maxY, graphwidth, graphheight, translateX, translateY);
        Font save=g.getFont();
        g.setFont(tickFont);
        graph.drawAxes(Graph.BOX, Graph.DOTTED, true);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color2);
        for (int i=0;i<datapoints.length;i++) {
            double x=datapoints[i][0];
            double y=datapoints[i][1];
            if (!Double.isNaN(x) && !Double.isNaN(y)) graph.drawDataPoint(x,y,dotStyle,dotSize);
        }        
        double slope=statistics[REGRESSION_COEFFICIENT];
        double intercept=statistics[INTERCEPT];
        Rectangle clipRect=g.getClipBounds();
        g.setClip(translateX+5, translateY+5, graphwidth-10, graphheight-10); // Set a clipRect to avoid drawing the regression line outside of the bounding box of the graph. Use 5px margin inside the box
        if (!Double.isNaN(slope) && !Double.isNaN(intercept)) {
            g.setColor(color1); // regression line
            double startY=intercept+minX*slope;
            double endY=intercept+maxX*slope;
            graph.drawLine(minX, startY, maxX, endY);
        }
        g.setClip(clipRect);
        g.setColor(Color.BLACK);
//        Font old=g.getFont();
//        g.setFont(old.deriveFont(Font.BOLD));
        int fontheight=(int)g.getFontMetrics(tickFont).getStringBounds("Motif score for sequence", g).getHeight();
        graph.drawAlignedString("Motif score for sequence", translateX+(int)(graphwidth/2.0), graphheight+translateY+5+fontheight, 0.5, 1);
        graph.drawVerticalAlignedString("Sequence value", 2, translateY+(int)(graphheight/2.0), 0.5, 1, true);
        g.setFont(save);        
    }
    
    
   @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        Workbook workbook=null;
        try {
            InputStream stream = DistributionAnalysis.class.getResourceAsStream("resources/AnalysisTemplate_SingleMotifRegression.xlt");
            workbook = WorkbookFactory.create(stream);
            stream.close();
                            
            // setup the data for the histogram                                      
            Sheet histogramsheet = workbook.getSheetAt(1);
            int rowIndex=0;
            for (int i=0;i<datapoints.length;i++) {
                double x=datapoints[i][0];
                double y=datapoints[i][1];  
                if (!(Double.isNaN(x) || Double.isNaN(y))) {
                    Row row=histogramsheet.getRow(rowIndex);
                    if (row==null) row=histogramsheet.createRow(rowIndex);
                    Cell cell=row.getCell(0);
                    if (cell==null) cell=row.createCell(0);
                    cell.setCellValue(x);
                    cell=row.getCell(1);
                    if (cell==null) cell=row.createCell(1);
                    cell.setCellValue(y);                    
                    rowIndex++;
                }
            }   

            if (rowIndex>0) {
                Name xValues = workbook.getName("valuesX"); //              
                String xValues_reference = "Sheet2!$A$1:$A$"+rowIndex;   //Set new range for named range                       
                xValues.setRefersToFormula(xValues_reference); //Assign to named range

                Name yValues = workbook.getName("valuesY"); //              
                String yValues_reference = "Sheet2!$B$1:$B$"+rowIndex;   //Set new range for named range                       
                yValues.setRefersToFormula(yValues_reference); //Assign to named range
            }
            
            // Now update the "front page"
            Sheet sheet = workbook.getSheetAt(0);              
            sheet.setForceFormulaRecalculation(true);
            String firstLine="The analysis was performed with motif \""+motifName+"\"  from track \""+motifTrackName+"\"";
            sheet.getRow(2).getCell(1).setCellValue(firstLine);
            String secondLine="Sequence values were taken from \""+sequenceMapName+"\"";
            if (sequenceCollectionName!=null) secondLine+=(" and limited to sequences in \""+sequenceCollectionName+"\"");
            sheet.getRow(3).getCell(1).setCellValue(secondLine);  
            if (ignoreMissing) {
                String thirdLine="Only sequences containing hits for the motif were included in the regression analysis";
                if (sheet.getRow(4)==null) sheet.createRow(4);
                if (sheet.getRow(4).getCell(1)==null) sheet.getRow(4).createCell(1);
                sheet.getRow(4).getCell(1).setCellValue(thirdLine);
            }                       
            
            // the matrix
            int offset=1;
            Row row=sheet.getRow(7);
            row.getCell(offset+0).setCellValue(statistics[INTERCEPT]);
            row.getCell(offset+1).setCellValue(statistics[REGRESSION_COEFFICIENT]);
            row.getCell(offset+2).setCellValue(statistics[PEARSONS_R]);
            row.getCell(offset+3).setCellValue(statistics[R_SQUARED]);
            row.getCell(offset+4).setCellValue(statistics[INTERCEPT_STDERR]);
            row.getCell(offset+5).setCellValue(statistics[SLOPE_STDERR]);
            row.getCell(offset+6).setCellValue(statistics[MEAN_SQUARE_ERROR]);
            row.getCell(offset+7).setCellValue(statistics[SUM_SQUARE_ERROR]);
            row.getCell(offset+8).setCellValue(statistics[REGRESSION_SUM_SQUARES]);
            row.getCell(offset+9).setCellValue(statistics[SIGNIFICANCE]);
            row.getCell(offset+10).setCellValue(statistics[DATA_POINTS_USED]);                                    
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new ExecutionError(e.getMessage());
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
        RegionDataset source=(RegionDataset)task.getParameter(PARAMETER_MOTIF_TRACK);
        if (!source.isMotifTrack()) throw new ExecutionError("Motif regression analysis can only be performed on motif tracks");
        motifTrackName=source.getName();
        Motif motif=(Motif)task.getParameter(PARAMETER_MOTIF);
        motifName=motif.getPresentationName();
        SequenceNumericMap geneExpression=(SequenceNumericMap)task.getParameter(PARAMETER_SEQUENCE_VALUES);
        sequenceMapName=geneExpression.getName();
        SequenceCollection collection=(SequenceCollection)task.getParameter(PARAMETER_SEQUENCES);
        if (collection==null) collection=task.getEngine().getDefaultSequenceCollection();
        else sequenceCollectionName=collection.getName();
        if (sequenceCollectionName!=null && sequenceCollectionName.equals(task.getEngine().getDefaultSequenceCollectionName())) sequenceCollectionName=null;
        if (collection.isEmpty()) throw new ExecutionError("Sequence collection '"+collection.getName()+"' is empty");
        datapoints=new double[collection.size()][2];
        ArrayList<String> sequenceNames=collection.getAllSequenceNames();
        Boolean skipnr=(Boolean)task.getParameter(PARAMETER_SKIP_NONREGULATED);
        ignoreMissing=(skipnr==null)?false:skipnr.booleanValue();
        Boolean donormalize=(Boolean)task.getParameter(PARAMETER_NORMALIZE);
        normalize=(donormalize==null)?false:donormalize.booleanValue(); 
        SimpleRegression regression=new SimpleRegression();
        int totalmotifcount=0;
        double totalMotifScore=0;
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setStatusMessage("Executing analysis: "+getAnalysisName());
        task.setProgress(20); //
        Thread.yield();     
        String motifID=motif.getName();
        int index=0;
        double minSeqScore=Double.MAX_VALUE;
        double maxSeqScore=-Double.MAX_VALUE;
        for (String sequenceName:sequenceNames) {
            boolean present=false;
            RegionSequenceData seq=(RegionSequenceData)source.getSequenceByName(sequenceName);
            double motifSequenceScore=0;
            double geneExpressionScore=geneExpression.getValue(sequenceName);
            for (Region r:seq.getOriginalRegions()) {
              if (r.getType().equals(motifID)) {
                  totalmotifcount++;
                  present=true;
                  motifSequenceScore+=r.getScore();
              }
            } 
            datapoints[index][0]=motifSequenceScore;
            datapoints[index][1]=geneExpressionScore;
            if (ignoreMissing && !present) datapoints[index][0]=Double.NaN;
            else {
                if (motifSequenceScore<minSeqScore) minSeqScore=motifSequenceScore;
                if (motifSequenceScore>maxSeqScore) maxSeqScore=motifSequenceScore;                
                //regression.addData(motifSequenceScore, geneExpressionScore);
            }  
            totalMotifScore+=motifSequenceScore;
            index++;
        } // end for each sequence
        for (int i=0;i<datapoints.length;i++) {
            if (Double.isNaN(datapoints[i][0])) continue;
            if (normalize) datapoints[i][0]=(datapoints[i][0]-minSeqScore)/(maxSeqScore-minSeqScore);
            regression.addData(datapoints[i][0],datapoints[i][1]);
        }
        // set the motif stats  
        statistics=new double[13];
        statistics[MOTIF_TOTAL_COUNT]=totalmotifcount;
        statistics[MOTIF_TOTAL_SCORE]=totalMotifScore;
        statistics[REGRESSION_COEFFICIENT]=regression.getSlope();
        statistics[INTERCEPT]=regression.getIntercept();
        statistics[SLOPE_STDERR]=regression.getSlopeStdErr();
        statistics[INTERCEPT_STDERR]=regression.getInterceptStdErr();
        statistics[SIGNIFICANCE]=regression.getSignificance();
        statistics[PEARSONS_R]=regression.getR();
        statistics[R_SQUARED]=regression.getRSquare();
        statistics[REGRESSION_SUM_SQUARES]=regression.getRegressionSumSquares();
        statistics[SUM_SQUARE_ERROR]=regression.getSumSquaredErrors();
        statistics[MEAN_SQUARE_ERROR]=regression.getMeanSquareError();
        statistics[DATA_POINTS_USED]=regression.getN();
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
