

package motiflab.engine.data.analysis;

import de.erichseifert.vectorgraphics2d.EPSGraphics2D;
import de.erichseifert.vectorgraphics2d.PDFGraphics2D;
import de.erichseifert.vectorgraphics2d.SVGGraphics2D;
import de.erichseifert.vectorgraphics2d.VectorGraphics2D;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import motiflab.engine.data.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import javax.swing.text.html.HTMLEditorKit;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.Graph;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.dataformat.DataFormat;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.VisualizationSettings;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
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
public class MapCorrelationAnalysis extends Analysis {
    private final static String typedescription="Analysis: numeric map correlation";
    private final static String analysisName="numeric map correlation";
    private final static String description="Compares two Numeric Maps to each other to determine if they are correlated";
    private String firstMapName=null;
    private String secondMapName=null;
    private String dataCollectionName=null;
    private int dataCollectionSize=0;
    private double[] datapointsFirst=null;  // these two arrays have the same order!
    private double[] datapointsSecond=null; // these two arrays have the same order!
    private double pearsonsCorrelation=0;
    private double spearmanCorrelation=0;

    
    public MapCorrelationAnalysis() {
        this.name="MapCorrelationAnalysis_temp";
        addParameter("First",NumericMap.class, null,new Class[]{NumericMap.class},"A Numeric Map",true,false);
        addParameter("Second",NumericMap.class, null,new Class[]{NumericMap.class},"A second Numeric Map to compare against the first to see if they are correlated",true,false);
        addParameter("Collection",DataCollection.class, null,new Class[]{DataCollection.class},"If specified, the correlation analysis will be limited to entries in this collection",false,false);
    }

    @Override
    public String[] getSourceProxyParameters() {return new String[]{"First","Second"};} 
    
    @Override
    public boolean canUseAsSourceProxy(Data data) {
        return (data instanceof NumericMap);
    }      
    
    /** Returns a list of output parameters that can be set when an Analysis is output */
    @Override
    public Parameter[] getOutputParameters() {        
         Parameter imageformat=new Parameter("Image format",String.class, "png",new String[]{"png","svg","pdf","eps"},"The image format to use for the graph",false,false);                       
         Parameter scalepar=new Parameter("Graph scale",Integer.class,100,new Integer[]{10,2000},"Scale of graphics plot (in percent)",false,false);
         Parameter sortBy=new Parameter("Sort by",String.class,"First",new String[]{"First","Second"},"Which of the two maps should be sorted in ascending order in the graph",false,false);
         return new Parameter[]{imageformat, sortBy,scalepar};
    }  

    @Override
    public String[] getOutputParameterFilter(String parameter) {
        if (parameter.equals("Sort by")) return new String[]{HTML,EXCEL};
        if (parameter.equals("Graph scale") || parameter.equals("Image format")) return new String[]{HTML};
        return null;
    }    
    
    @Override
    public String[] getResultVariables() {
        return new String[]{"Pearson's correlation","Spearman's rank correlation"};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename==null || variablename.isEmpty()) throw new ExecutionError("'"+getName()+"' does not have a result for ''");
        else if (variablename.equals("Pearson's correlation")) return new NumericVariable("temp",pearsonsCorrelation);
        else if (variablename.equals("Spearman's rank correlation")) return new NumericVariable("temp",spearmanCorrelation);
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
    public MapCorrelationAnalysis clone() {
        MapCorrelationAnalysis newanalysis=new MapCorrelationAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.firstMapName=this.firstMapName;
        newanalysis.secondMapName=this.secondMapName;
        newanalysis.dataCollectionName=this.dataCollectionName;     
        newanalysis.dataCollectionSize=this.dataCollectionSize;
        newanalysis.pearsonsCorrelation=this.pearsonsCorrelation;
        newanalysis.spearmanCorrelation=this.spearmanCorrelation;
        newanalysis.datapointsFirst=this.datapointsFirst.clone();
        newanalysis.datapointsSecond=this.datapointsSecond.clone();
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        MapCorrelationAnalysis other=((MapCorrelationAnalysis)source);
        this.firstMapName=other.firstMapName;
        this.secondMapName=other.secondMapName;
        this.dataCollectionName=other.dataCollectionName;    
        this.dataCollectionSize=other.dataCollectionSize;
        this.datapointsFirst=other.datapointsFirst;
        this.datapointsSecond=other.datapointsSecond;
        this.pearsonsCorrelation=other.pearsonsCorrelation;
        this.spearmanCorrelation=other.spearmanCorrelation;
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
        int scalepercent=100;
        String showAscendingString="First";
        String imageFormat="png";
        if (settings!=null) {
          try {
                 Parameter[] defaults=getOutputParameters();
                 scalepercent=(Integer)settings.getResolvedParameter("Graph scale",defaults,engine); 
                 imageFormat=(String)settings.getResolvedParameter("Image format",defaults,engine);                 
                 showAscendingString=(String)settings.getResolvedParameter("Sort by",defaults,engine);
          } 
          catch (ExecutionError e) {throw e;} 
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        } 
        double scale=(scalepercent==100)?1.0:(((double)scalepercent)/100.0);
        int showAscending=(showAscendingString.equals("Second"))?1:0;
        File imagefile=outputobject.createDependentFile(engine,imageFormat); 
        Object dimension=null;
        VisualizationSettings vz=engine.getClient().getVisualizationSettings();        
        try {
            dimension=createGraphImage(imagefile,showAscending, scale, vz.getSystemColor("color1"), vz.getSystemColor("color2"), vz.getSystemFont("graph.legendFont"), vz.getSystemFont("graph.tickFont"));
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        if (format!=null) format.setProgress(50);
        engine.createHTMLheader("Map Correlation Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<div align=\"center\">\n",HTML);
        outputobject.append("<h2 class=\"headline\">Map Correlation Analysis</h2>\n",HTML);
        outputobject.append("<br />\n",HTML);
        outputobject.append("Analysis of the correlation between <span class=\"dataitem\">"+firstMapName+"</span> and <span class=\"dataitem\">"+secondMapName+"</span>\n",HTML);
        outputobject.append("<br>The analysis was based on "+dataCollectionSize+" pair"+((dataCollectionSize!=1)?"s":"")+" of values\n",HTML);
        if (dataCollectionName!=null) {
            outputobject.append(" from collection <span class=\"dataitem\">",HTML);
            outputobject.append(dataCollectionName,HTML);
            outputobject.append("</span>\n",HTML);
        }
        outputobject.append("<br><br>\n",HTML);
        
        if (imageFormat.equals("pdf")) outputobject.append("<object type=\"application/pdf\" data=\"file:///"+imagefile.getAbsolutePath()+"\"></object>",HTML);
        else {
            outputobject.append("<img src=\"file:///"+imagefile.getAbsolutePath()+"\"",HTML);
            if (dimension instanceof Dimension) {
                int width=((Dimension)dimension).width;
                int height=((Dimension)dimension).height;
                outputobject.append(" width="+(int)Math.ceil(width*scale)+" height="+(int)Math.ceil(height*scale),HTML);
            }                    
            outputobject.append(" />\n",HTML);  
        }
        
        outputobject.append("<br /><br /><table>\n",HTML);
        outputobject.append("<tr><th>Pearson's Correlation</th><th>Spearman's Correlation</th></tr>\n",HTML);
        outputobject.append("<tr><td style=\"text-align:center\">",HTML);
        outputobject.append(Graph.formatNumber(pearsonsCorrelation, false),HTML);
        outputobject.append("</td><td style=\"text-align:center\">",HTML);
        outputobject.append(Graph.formatNumber(spearmanCorrelation, false),HTML);
        outputobject.append("</td></tr>\n",HTML);
        outputobject.append("</table>\n",HTML);
        outputobject.append("</div>\n",HTML);
        outputobject.append("</body>\n</html>\n",HTML);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }

    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setStatusMessage("Executing operation: output");
        outputobject.append("#Map correlation between '"+firstMapName+"' and '"+secondMapName+"'\n",RAWDATA);
        outputobject.append("#Analysis based on "+dataCollectionSize+" pair"+((dataCollectionSize!=1)?"s":"")+" of values"+((dataCollectionName!=null)?(" from collection '"+dataCollectionName+"'"):"")+"\n",RAWDATA);
        outputobject.append("Pearson's Correlation\t"+pearsonsCorrelation+"\n",RAWDATA);
        outputobject.append("Spearmans's Rank Correlation\t"+spearmanCorrelation+"\n",RAWDATA);
        format.setProgress(100);
        return outputobject;
    }
    
    private double[] getMinMax(double[] list) {
        double min=Double.MAX_VALUE;
        double max=-Double.MAX_VALUE;
        for (int i=0;i<list.length;i++) {
            if (list[i]>max) max=list[i];
            if (list[i]<min) min=list[i];
        }
        return new double[]{min,max};
    }
    
    
    private Object createGraphImage(File file, int sortBy, double scale, Color firstColor, Color secondColor, Font legendFont, Font tickFont) throws IOException {
        int graphheight=200; // height of graph in pixels (just the graph);
        int graphwidth=500; // width of graph in pixels (just the graph);
        int translateX=6; // the X coordinate for the left edge of the graph
        int translateY=6; // the Y coordinate for the top of the graph 
        int width=graphwidth+2*translateX;
        int legendEntriesSpacing=20;
        Dimension d=Graph.getHorizontalLegendDimension(new String[]{firstMapName,secondMapName}, legendEntriesSpacing, legendFont);
        if (width<(d.width+translateX+translateX)) width=(d.width+translateX+translateX); // just to make sure there is enough room for the legend box
        int height=graphheight+translateY+d.height;// +translateY;
        
        if (file!=null) {
            if (file.getName().endsWith(".png")) { // bitmap PNG format   
                BufferedImage image=new BufferedImage((int)Math.ceil(width*scale),(int)Math.ceil(height*scale), BufferedImage.TYPE_INT_RGB);
                Graphics2D g=image.createGraphics();   
                paintGraphImage(g, sortBy, scale, width, height, graphwidth, graphheight, translateX, translateY, legendEntriesSpacing, firstColor, secondColor, legendFont, tickFont);         
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
                paintGraphImage(g, sortBy, scale, width, height, graphwidth, graphheight, translateX, translateY, legendEntriesSpacing, firstColor, secondColor, legendFont, tickFont);                                   
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
            paintGraphImage(g, sortBy, scale, width, height, graphwidth, graphheight, translateX, translateY, legendEntriesSpacing, firstColor, secondColor, legendFont, tickFont);
            g.dispose();
            return image;                        
        }        
    }     
    
  /** Creates a graph based on the current data and saves it to file (if file is not null)*/
    private void paintGraphImage(Graphics2D g, int sortBy, double scale, int width, int height, int graphwidth, int graphheight, int translateX, int translateY, int legendEntriesSpacing, Color firstColor, Color secondColor, Font legendFont, Font tickFont) throws IOException {
        g.scale(scale, scale);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width+20, height+20);        
        double[] fMM=getMinMax(datapointsFirst);
        double[] sMM=getMinMax(datapointsSecond);
        double minValueFirst=fMM[0];
        double maxValueFirst=fMM[1];
        double minValueSecond=sMM[0];        
        double maxValueSecond=sMM[1];
        if (minValueFirst==maxValueFirst) maxValueFirst=minValueFirst+1;
        if (minValueSecond==maxValueSecond) maxValueSecond=minValueSecond+1;
        double firstOffset=(maxValueFirst-minValueFirst)*0.05;
        double secondOffset=(maxValueSecond-minValueSecond)*0.05;
        
        int size=datapointsFirst.length;
        Graph graph1=new Graph(g, 0, size-1, minValueFirst-firstOffset, maxValueFirst+firstOffset, graphwidth, graphheight, translateX, translateY);
        Graph graph2=new Graph(g, 0, size-1, minValueSecond-secondOffset, maxValueSecond+secondOffset, graphwidth, graphheight, translateX, translateY);
        // write the image to file
        g.setColor(Color.BLACK);
        ArrayList<double[]> inSortedOrder=new ArrayList<double[]>(size);
        for (int i=0;i<size;i++) inSortedOrder.add(new double[]{datapointsFirst[i],datapointsSecond[i]});
        Collections.sort(inSortedOrder,new SortOrderComparator(sortBy));
        double[] xPoints=new double[size];
        double[] firstPoints=new double[size];
        double[] secondPoints=new double[size];       
        for (int i=0;i<size;i++) {
            xPoints[i]=i;
            double[] pair=inSortedOrder.get(i);
            firstPoints[i]=pair[0];
            secondPoints[i]=pair[1];
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);        
        Stroke defaultStroke=g.getStroke();   
        BasicStroke fatStroke = new BasicStroke(1.5f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        if (sortBy==1) {
            g.setColor(firstColor);
            graph1.drawCurve(xPoints, firstPoints);
            g.setStroke(fatStroke);
        }
        g.setColor(secondColor);
        graph2.drawCurve(xPoints, secondPoints);
        if (sortBy==0) {
            g.setStroke(fatStroke);
            g.setColor(firstColor);
            graph1.drawCurve(xPoints, firstPoints); // draw this last to get it overlaid on top of the other
        }
        g.setStroke(defaultStroke);
        g.setColor(Color.BLACK);
        Font save=g.getFont();
                                     
        // draw ticks (inside of box)
        g.setFont(tickFont);
        g.setColor(firstColor);
        graph1.drawYaxisWithTicks(translateX, false, false, false, firstColor);
        g.setColor(secondColor);
        graph2.drawYaxisWithTicks(translateX+graphwidth, false, false, true, secondColor);

        // draw bounding box
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        graph1.drawAxes(Graph.BOX, Graph.NONE, true, false, false, false);
        
        // draw legend box
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
        g.setFont(legendFont);
        graph1.drawHorizontalLegendBox(new String[]{firstMapName,secondMapName}, new Color[]{firstColor,secondColor}, translateX, translateY+graphheight, legendEntriesSpacing, false);
        
        g.setFont(save);
    } 
    
    ArrayList<double[]> getMapValues(int sortBy) {
        int size=datapointsFirst.length;
        ArrayList<double[]> inSortedOrder=new ArrayList<double[]>(size);
        for (int i=0;i<size;i++) inSortedOrder.add(new double[]{datapointsFirst[i],datapointsSecond[i]});
        Collections.sort(inSortedOrder,new SortOrderComparator(sortBy));
        return inSortedOrder;
    }
    
   @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String showAscendingString="First";
        if (settings!=null) {
          try {
                 Parameter[] defaults=getOutputParameters();              
                 showAscendingString=(String)settings.getResolvedParameter("Sort by",defaults,engine);
          } 
          catch (ExecutionError e) {throw e;} 
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        } 
        int ascendingMap=(showAscendingString.equals("Second"))?1:0;        
       
        Workbook workbook=null;
        try {
            InputStream stream = MapCorrelationAnalysis.class.getResourceAsStream("resources/AnalysisTemplate_MapCorrelation.xlt");
            workbook = WorkbookFactory.create(stream);
            stream.close();
                        
            // setup the data for the maps which will be used to create the graph                                      
            ArrayList<double[]> map=getMapValues(ascendingMap);
            Sheet mapValuesSheet = workbook.getSheetAt(1);
            
            int entries=map.size();        
            for (int i=0;i<entries;i++) { // insert histogram values into Sheet2
                double[] mapsrow=map.get(i);
                Row row=mapValuesSheet.getRow(i+1);
                if (row==null) row=mapValuesSheet.createRow(i+1);
                Cell cell=row.getCell(0);
                if (cell==null) cell=row.createCell(0);
                cell.setCellValue(mapsrow[0]);
                cell=row.getCell(1);
                if (cell==null) cell=row.createCell(1);                
                cell.setCellValue(mapsrow[1]);                   
            }
            Name firstRange = workbook.getName("First"); //              
            String firstRange_reference = "Sheet2!$A$2:$A$"+(entries+1);   //Set new range for named range                       
            firstRange.setRefersToFormula(firstRange_reference); //Assign to named range

            Name secondRange = workbook.getName("Second"); //              
            String secondRange_reference = "Sheet2!$B$2:$B$"+(entries+1);   //Set new range for named range                       
            secondRange.setRefersToFormula(secondRange_reference); //Assign to named range
            
            // set the series names
            mapValuesSheet.getRow(0).getCell(0).setCellValue((ascendingMap==0)?firstMapName:secondMapName);
            mapValuesSheet.getRow(0).getCell(1).setCellValue((ascendingMap==1)?firstMapName:secondMapName);
                                 
            // Now update the "front page"
            Sheet sheet = workbook.getSheetAt(0);              
            sheet.setForceFormulaRecalculation(true);                     
            String description1="Analysis of the correlation between the numeric maps \""+firstMapName+"\" and \""+secondMapName+"\"";
            String description2="The analysis was based on "+dataCollectionSize+" pair"+((dataCollectionSize!=1)?"s":"")+" of values";
            if (dataCollectionName!=null) description2+=" from collection \""+dataCollectionName+"\"";
            sheet.getRow(2).getCell(0).setCellValue(description1);
            sheet.getRow(3).getCell(0).setCellValue(description2);
                                 
            // fill inn the correlation values
            sheet.getRow(30).getCell(1).setCellValue(pearsonsCorrelation);
            sheet.getRow(30).getCell(2).setCellValue(spearmanCorrelation);

                                 
            
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
        NumericMap firstMap=(NumericMap)task.getParameter("First");
        NumericMap secondMap=(NumericMap)task.getParameter("Second");
        if (!firstMap.getClass().equals(secondMap.getClass())) throw new ExecutionError("The two Numeric Maps must be of the same type");
        DataCollection collection=(DataCollection)task.getParameter("Collection");
        if (collection!=null) {
            if (firstMap instanceof SequenceNumericMap && !(collection instanceof SequenceCollection)) throw new ExecutionError("The Collection parameter must be a Sequence Collection if the maps are Sequence Numeric Maps");
            if (firstMap instanceof MotifNumericMap && !(collection instanceof MotifCollection)) throw new ExecutionError("The Collection parameter must be a Motif Collection if the maps are Motif Numeric Maps");           
            dataCollectionName=collection.getName();
        }
        firstMapName=firstMap.getName();
        secondMapName=secondMap.getName();
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setStatusMessage("Executing analysis: "+getAnalysisName());
        task.setProgress(20); //
        Thread.yield();     
        int index=0;
        ArrayList<String> keys=(collection!=null)?collection.getValues():firstMap.getAllKeys(task.getEngine());
        int size=keys.size();
        if (size<2) throw new ExecutionError("At least 2 data points is required to perform correlation analysis");
        dataCollectionSize=size;
        datapointsFirst=new double[size];
        datapointsSecond=new double[size];
        for (String key:keys) {
            datapointsFirst[index]=firstMap.getValue(key);
            datapointsSecond[index]=secondMap.getValue(key);
            index++;
        }
        // calculate statistics
        try {
            PearsonsCorrelation pearsons=new PearsonsCorrelation();
            pearsonsCorrelation=pearsons.correlation(datapointsFirst, datapointsSecond);
            SpearmansCorrelation spearman=new SpearmansCorrelation();
            spearmanCorrelation=spearman.correlation(datapointsFirst, datapointsSecond);
        } catch (Exception e) {
            throw new ExecutionError(e.getMessage());
        }
    }


    @Override
    protected JPanel getDisplayPanel(MotifLabGUI gui, boolean modal) {
        StringBuilder document=new StringBuilder();
        document.append("<html>");
        document.append("<head><style type=\"text/css\">");
        gui.getEngine().appendDefaultCSSstyle(document);
        document.append("</style></head><body>");
        document.append("<div align=\"center\"><table>");
        document.append("<tr><th>Pearson's Correlation</th><th>Spearman's Correlation</th></tr>");
        document.append("<tr><td style=\"text-align:center\">");
        document.append(Graph.formatNumber(pearsonsCorrelation, false));
        document.append("</td><td style=\"text-align:center\">");
        document.append(Graph.formatNumber(spearmanCorrelation, false));
        document.append("</td></tr>");
        document.append("</table>");
        document.append("<br>Analysis of the correlation between <span class=\"dataitem\">"+firstMapName+"</span> and <span class=\"dataitem\">"+secondMapName+"</span>");
        document.append("<br>The analysis was based on "+dataCollectionSize+" pair"+((dataCollectionSize!=1)?"s":"")+" of values");
        if (dataCollectionName!=null) {
            document.append(" from collection <span class=\"dataitem\">");
            document.append(dataCollectionName);
            document.append("</span>");
        }
        document.append("</div></body></html>");

        JEditorPane textEditor=new JEditorPane();
        textEditor.setEditorKit(new HTMLEditorKit());
        textEditor.setDocument(((HTMLEditorKit)textEditor.getEditorKit()).createDefaultDocument());
        try {
            textEditor.setText(document.toString());
        } catch (Exception e) {throw new NullPointerException(e.getMessage());}
        textEditor.setEditable(false); // this is the best solution. In this case we don't encounter problems/inconsistencies related to undo/redo


        JPanel outerpanel=new JPanel(new BorderLayout());
        GraphPanel graphpanel=new GraphPanel(gui.getVisualizationSettings());
        outerpanel.add(graphpanel,BorderLayout.NORTH);
        outerpanel.add(textEditor,BorderLayout.CENTER);
        Dimension dim=new Dimension(540,400);
        outerpanel.setPreferredSize(dim);
        outerpanel.setPreferredSize(dim);
        JScrollPane graphscrollpane=new JScrollPane(outerpanel);
        JPanel displayPanel=new JPanel(new BorderLayout());        
        displayPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8,5,0,5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)
        ));
        displayPanel.add(graphscrollpane);
        //displayPanel.setPreferredSize(new Dimension(600,320));
        return displayPanel;
    }

    private class GraphPanel extends JPanel {
        private BufferedImage image;
        private Font headerFont;
        private JComboBox sortByCombobox;
        private VisualizationSettings settings;

        public GraphPanel(VisualizationSettings settings) {
            super();
            this.settings=settings;
            this.setLayout(new FlowLayout(FlowLayout.LEFT));
            this.setBackground(Color.WHITE);           
            this.setOpaque(true);
            headerFont=new Font(Font.SANS_SERIF, Font.BOLD, 18);
            this.setMinimumSize(null);
            sortByCombobox=new JComboBox(new String[]{firstMapName,secondMapName});
            javax.swing.JLabel spacer=new javax.swing.JLabel();
            Dimension d=new Dimension(280,10);
            spacer.setPreferredSize(d);
            spacer.setMinimumSize(d);
            this.add(spacer);
            this.add(new javax.swing.JLabel("Sort by "));
            this.add(sortByCombobox);
            sortByCombobox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateGraphImage();
                }
            });
            sortByCombobox.setSelectedIndex(0);
        }

        public void updateGraphImage() {
            String order=(String)sortByCombobox.getSelectedItem();
            int showAscending=0;
            if (order.equals(secondMapName)) showAscending=1;
            try {
                Object imageObject=createGraphImage(null, showAscending, 1.0, settings.getSystemColor("color1"), settings.getSystemColor("color2"), settings.getSystemFont("graph.legendFont"), settings.getSystemFont("graph.tickFont"));
                if (imageObject instanceof BufferedImage) image=(BufferedImage)imageObject;
                else image=null;
            } catch (Exception e) {}
            if (image!=null) {
                int width=image.getWidth()+20;    if (width<540) width=540;
                int height=image.getHeight()+30;  if (height<280) height=280;
                this.setPreferredSize(new Dimension(width,height));
                this.setMinimumSize(new Dimension(width,height));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Dimension d=getSize();
            g.setColor(this.getBackground());
            g.fillRect(0, 0, d.width, d.height);
            if (image!=null) g.drawImage(image, 8, 30, this);
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.BLACK);
            g.setFont(headerFont);
            g.drawString("Map Correlation Analysis", 18, 25);
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
    }

    private class SortOrderComparator implements Comparator<double[]> {
        private int sortBy=0;

        public SortOrderComparator(int sortBy) {
            this.sortBy=sortBy;
        }

        @Override
        public int compare(double[] first, double[] second) { // 
            double firstValue=first[sortBy];
            double secondValue=second[sortBy];
            if (Double.isNaN(firstValue) && Double.isNaN(secondValue)) return 0;
            if (Double.isNaN(firstValue)) return 1;
            if (Double.isNaN(secondValue)) return -1;               
            if (firstValue<secondValue) return -1;
            else if (firstValue>secondValue) return 1;
            else { // equal values -> sort on other property
                firstValue=first[(sortBy==0)?1:0];
                secondValue=second[(sortBy==0)?1:0];
                if (Double.isNaN(firstValue) && Double.isNaN(secondValue)) return 0;
                if (Double.isNaN(firstValue)) return 1;
                if (Double.isNaN(secondValue)) return -1;                     
                if (firstValue<secondValue) return -1;
                else if (firstValue>secondValue) return 1;
                else return 0;
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
