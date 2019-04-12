
package motiflab.engine.dataformat;

import de.erichseifert.vectorgraphics2d.EPSGraphics2D;
import de.erichseifert.vectorgraphics2d.PDFGraphics2D;
import de.erichseifert.vectorgraphics2d.SVGGraphics2D;
import de.erichseifert.vectorgraphics2d.VectorGraphics2D;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import javax.imageio.ImageIO;
import motiflab.engine.ExecutionError;
import motiflab.engine.Graph;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.data.TextVariable;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.task.ExecutableTask;
import motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class DataFormat_Graph extends DataFormat {

    private Class[] supportedTypes=new Class[]{OutputData.class,TextVariable.class};
    public static final String PIE_CHART="Pie chart";
    public static final String LINE_CHART="Line chart";
    public static final String BAR_CHART="Bar chart";
    public static final String BOX_PLOT="Box plot";    
    public static final String HISTOGRAM_CHART="Histogram";    
    public static final String VENN_DIAGRAM="Venn diagram";    
    
    public static final String DATAFORMAT_LINES="Lines";
    public static final String DATAFORMAT_ROWS="Rows";
    public static final String DATAFORMAT_COLUMNS="Columns";   
    
    private static Color shadowColor=new Color(0,0,0,25);

    public DataFormat_Graph() {
        addParameter("Graph type", LINE_CHART, new String[]{LINE_CHART,BAR_CHART,BOX_PLOT,PIE_CHART,HISTOGRAM_CHART, VENN_DIAGRAM},"Graph type");
        addParameter("Data format", DATAFORMAT_LINES, new String[]{DATAFORMAT_LINES,DATAFORMAT_COLUMNS,DATAFORMAT_ROWS},"The layout of the data in the source");
        addOptionalParameter("Plot settings", "", null,"Controls additional settings for the plot. These can be graph type dependent. See the full documentation for more information.");   
        addParameter("Image width", new Integer(500), new Integer[]{1,10000},"The width (in pixels) of the image before scaling");            
        addParameter("Image height", new Integer(300), new Integer[]{1,10000},"The height (in pixels) of the image before scaling"); 
        addOptionalParameter("Image scale", new Integer(100), new Integer[]{1,10000},"Scale of image");           
    }
        
    @Override
    public String getName() {
        return "Graph";
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof TextVariable || data instanceof OutputData);
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass==TextVariable.class || dataclass==OutputData.class);
    }
    
    @Override
    public boolean canParseInput(Data data) {
        return false;
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {  
        return false;
    }
    
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }
    
    @Override
    public String getSuffix() {
        return "html";
    }

    @Override
    public boolean isAppendable() {
        return false;
    }

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!(dataobject instanceof OutputData || dataobject instanceof TextVariable)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in Graph format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in Graph format");
        }        
        setProgress(5);
        ArrayList<String> data=null;
        if (dataobject instanceof TextVariable) {
            data=((TextVariable)dataobject).getAllStrings();
        } else {
            data=((OutputData)dataobject).getContentsAsStrings();
        }
        String imageFormat="png";     
        String graphType=LINE_CHART;
        String dataFormat=DATAFORMAT_LINES;
        String plotSettingsString="";
        int imageWidth=0;
        int imageHeight=0;
        int scalepercent=100;
        if (settings!=null) {
          try {
             Parameter[] defaults=getParameters();
             graphType=(String)settings.getResolvedParameter("Graph type",defaults,engine);
             dataFormat=(String)settings.getResolvedParameter("Data format",defaults,engine);             
             imageWidth=(Integer)settings.getResolvedParameter("Image width",defaults,engine);   
             imageHeight=(Integer)settings.getResolvedParameter("Image height",defaults,engine);  
             scalepercent=(Integer)settings.getResolvedParameter("Image scale",defaults,engine);     
             plotSettingsString=(String)settings.getResolvedParameter("Plot settings",defaults,engine);  
          } 
          catch (ExecutionError e) {throw e;} 
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        } else {
             graphType=(String)getDefaultValueForParameter("Graph type");    
             dataFormat=(String)getDefaultValueForParameter("Data format");             
             //imageFormat=(String)getDefaultValueForParameter("Image format");
             imageWidth=(Integer)getDefaultValueForParameter("Image width");  
             imageHeight=(Integer)getDefaultValueForParameter("Image height");               
             scalepercent=(Integer)getDefaultValueForParameter("Image scale");               
        }                       
        double scale=(scalepercent==100)?1.0:(((double)scalepercent)/100.0);        
        File imagefile=outputobject.createDependentFile(engine,imageFormat); 
        File legendFile=null;
        Dataset dataset=new Dataset();
        boolean sort=true;
        if (graphType.equals(PIE_CHART)) sort=false; // retain order used in dataset
        dataset.parseData(dataFormat, data, "\t", sort);        
        PlotSettings plotSettings=new PlotSettings();
        plotSettings.parsePlotSettings(plotSettingsString,dataset);
        boolean separateLegend=plotSettings.globalSettingEquals("legend", "separate");
        if (separateLegend) {
            plotSettings.setGlobalSetting("legend", "none");
        } // remove from further consideration
        try {
            drawImage(graphType, imageFormat, imagefile, dataset, plotSettings, imageWidth, imageHeight, scale, engine);
            if (separateLegend) {
                legendFile=outputobject.createDependentFile(engine,imageFormat); 
                drawLegendImage(imageFormat, legendFile, dataset, plotSettings, scale, engine, task);
            }
        } catch (InterruptedException ie) {
            throw ie;
        } catch (ExecutionError ee) {
            throw ee;
        } catch (Exception e) {
            throw new ExecutionError(e.getMessage(),e);
        }
        setProgress(50);        
        formatDocument(outputobject, imagefile, legendFile, engine, settings, task, this);
        outputobject.setShowAsHTML(true);    
        setProgress(100);        
        return outputobject;
    }    
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
         throw new ParseError("Unable to parse input to target data of type '"+target.getTypeDescription()+"' using DataFormat_Graph"); 
    }    
    
     
    
   private void formatDocument(OutputData outputobject, File imagefile, File legendimagefile, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        engine.createHTMLheader("Graph", null, null, false, false, false, outputobject);
        outputobject.append("<img align=middle src=\"file:///"+imagefile.getAbsolutePath()+"\" />",DataFormat_HTML.HTML);
        if (legendimagefile!=null) {
            outputobject.append("&nbsp;&nbsp;<img align=middle src=\"file:///"+legendimagefile.getAbsolutePath()+"\" />",DataFormat_HTML.HTML);
        }
   }
     
   
   private void drawImage(String graphType, String imageFormat, File imagefile, Dataset dataset, PlotSettings plotSettings, int width, int height, double scale, MotifLabEngine engine) throws ExecutionError, IOException, InterruptedException {
        int scaledWidth=(int)Math.ceil(width*scale);
        int ScaledHeight=(int)Math.ceil(height*scale);
        imageFormat=imageFormat.toLowerCase();
        if (imageFormat.equals("gif") || imageFormat.equals("png")) {
            BufferedImage image=new BufferedImage(scaledWidth, ScaledHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();
            g.scale(scale, scale);          
            g.setClip(0, 0, width, height);            
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            drawChart(g, graphType, dataset, plotSettings, engine.getClient().getVisualizationSettings());
            OutputStream output=MotifLabEngine.getOutputStreamForFile(imagefile);
            ImageIO.write(image, imageFormat, output);
            output.close();             
            g.dispose();            
        } else {                     
            VectorGraphics2D g=null;
                 if (imageFormat.equals("svg")) g = new SVGGraphics2D(0, 0, scaledWidth, ScaledHeight);
            else if (imageFormat.equals("pdf")) g = new PDFGraphics2D(0, 0, scaledWidth, ScaledHeight);
            else if (imageFormat.equals("eps")) g = new EPSGraphics2D(0, 0, scaledWidth, ScaledHeight);
            else throw new IOException("Unknown image format: "+imageFormat);
            g.scale(scale, scale);                 
            g.setClip(0, 0, width, height);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            drawChart(g, graphType, dataset, plotSettings, engine.getClient().getVisualizationSettings());                          
            OutputStream fileStream = MotifLabEngine.getOutputStreamForFile(imagefile); // should this be buffered?
            try {
                fileStream.write(g.getBytes());
            } finally {
                fileStream.close();
            } 
            g.dispose();            
        }   
       
   }
   
   private void drawLegendImage(String imageFormat, File imagefile, Dataset dataset, PlotSettings plotSettings, double scale, MotifLabEngine engine, ExecutableTask task) throws ExecutionError, IOException, InterruptedException {
        String[] legendStrings=new String[dataset.getNumberofSeries()];
        Color[] legendColors=new Color[dataset.getNumberofSeries()];
        int index=0;
        for (String series:dataset.getSeriesNames()) {
            legendStrings[index]=series;
            legendColors[index]=plotSettings.getColorSetting(series);
            index++;
        }            
        int legendSpacing=(int)plotSettings.getGlobalSetting("legendSpacing",  new Integer(10));
        boolean box=(boolean)plotSettings.getGlobalSetting("legendBox",  Boolean.TRUE);  
        boolean horizontal=(boolean)plotSettings.getGlobalSetting("legendHorizontal",  Boolean.FALSE);  
        Font legendFont=engine.getClient().getVisualizationSettings().getSystemFont("graph.legendFont"); // used in the legend box
        Dimension dim=(horizontal)?Graph.getHorizontalLegendDimension(legendStrings, legendSpacing, legendFont):Graph.getLegendDimension(legendStrings, legendFont);
        int width=dim.width;
        int height=dim.height;
        int scaledWidth=(int)Math.ceil(width*scale);
        int ScaledHeight=(int)Math.ceil(height*scale);
        imageFormat=imageFormat.toLowerCase();
        if (imageFormat.equals("gif") || imageFormat.equals("png")) {
            BufferedImage image=new BufferedImage(scaledWidth, ScaledHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();
            g.scale(scale, scale);          
            g.setClip(0, 0, width, height);            
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            drawLegend(g, legendStrings, legendColors, box, horizontal, legendSpacing, legendFont);
            OutputStream output=MotifLabEngine.getOutputStreamForFile(imagefile);
            ImageIO.write(image, imageFormat, output);
            output.close(); 
            g.dispose();            
        } else {                     
            VectorGraphics2D g=null;
                 if (imageFormat.equals("svg")) g = new SVGGraphics2D(0, 0, scaledWidth, ScaledHeight);
            else if (imageFormat.equals("pdf")) g = new PDFGraphics2D(0, 0, scaledWidth, ScaledHeight);
            else if (imageFormat.equals("eps")) g = new EPSGraphics2D(0, 0, scaledWidth, ScaledHeight);
            else throw new IOException("Unknown image format: "+imageFormat);
            g.scale(scale, scale);                 
            g.setClip(0, 0, width, height);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            drawLegend(g, legendStrings, legendColors, box, horizontal, legendSpacing, legendFont);                        
            OutputStream fileStream = MotifLabEngine.getOutputStreamForFile(imagefile); // should this be buffered?
            try {
                fileStream.write(g.getBytes());
            } finally {
                fileStream.close();
            } 
            g.dispose();            
        }   
       
   }   
           
   private void drawChart(Graphics2D g, String graphType, Dataset dataset, PlotSettings plotSettings, VisualizationSettings visualizationSettings) throws ExecutionError {
       int width=g.getClipBounds().width; // These are set to the size of the image
       int height=g.getClipBounds().height;
       plotSettings.setGlobalSetting("imageWidth", width);
       plotSettings.setGlobalSetting("imageHeight", height);       
       
       if (graphType.equalsIgnoreCase(LINE_CHART)) drawLineChart(g, dataset, plotSettings);
       else if (graphType.equalsIgnoreCase(BAR_CHART)) drawBarChart(g, dataset, plotSettings);        
       else if (graphType.equalsIgnoreCase(HISTOGRAM_CHART)) drawHistogram(g, dataset, plotSettings);
       else if (graphType.equalsIgnoreCase(PIE_CHART)) drawPieChart(g, dataset, plotSettings, visualizationSettings);
       else if (graphType.equalsIgnoreCase(BOX_PLOT)) drawBoxPlot(g, dataset, plotSettings);       
       else throw new ExecutionError("Unknown graph type: "+graphType);
   }
   
   
   private void drawLineChart(Graphics2D g, Dataset dataset, PlotSettings plotSettings) throws ExecutionError {
        int tupleSize=dataset.getMinTupleSize();
        if (tupleSize<2) throw new ExecutionError("Line chart requires at least two values (x,y) per data point");
                
        double[] minmax_X=dataset.getMinAndMaxFromValues(0);
        double[] minmax_Y=dataset.getMinAndMaxFromValues(1);
        plotSettings.setGlobalSetting("plotrange", new double[]{minmax_X[0],minmax_X[1],minmax_Y[0],minmax_Y[1]}); // this needs to be passed to the plotSettings
        
        HashMap<String,Object> graphProperties=processPlotSettings(plotSettings, dataset);  
        double minX=(double)graphProperties.get("minX");
        double maxX=(double)graphProperties.get("maxX");
        double minY=(double)graphProperties.get("minY");
        double maxY=(double)graphProperties.get("maxY");

        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
       
        graphProperties=Graph.layoutGraphImage(true, graphProperties, vizSettings, g); // find best layout
        int translateX=(Integer)graphProperties.get("translateX");   // the X coordinate for the left of the graph
        int translateY=(Integer)graphProperties.get("translateY");   // the Y coordinate for the top of the graph
        int graphwidth=(Integer)graphProperties.get("graphWidth");   // width of graph in pixels (just the actual axis-system)
        int graphheight=(Integer)graphProperties.get("graphHeight"); // height of graph in pixels (just the actual axis-system)        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Graph graph=new Graph(g, minX, maxX, minY, maxY, graphwidth, graphheight, translateX, translateY);
        graph.drawAxesAndStuff(graphProperties, vizSettings); // draws axes, ticks, labels, titles and legend box as specified
        Shape clip=g.getClip();       
        g.setClip(translateX, translateY, graphwidth, graphheight); // do not allow drawing outside the graphbox (in case ranges have been adjusted smaller than the actual graph)
        Stroke defaultStroke=g.getStroke();
        int pointSize=(int)plotSettings.getGlobalSetting("point size", new Integer(3));
        int shadowOffset=(int)plotSettings.getGlobalSetting("shadow", new Integer(0));
        if (shadowOffset>0) {
            Color color=g.getColor();
            g.setColor(shadowColor);
            g.translate(shadowOffset,shadowOffset);
            for (String series:dataset.getSeriesNames()) {
                drawSeriesAsLine(g, graph, series, pointSize, dataset, plotSettings, defaultStroke);
            }
            g.translate(-shadowOffset,-shadowOffset);
            g.setColor(color);
        }        
        for (String series:dataset.getSeriesNames()) {
            g.setColor(plotSettings.getColorSetting(series));
            drawSeriesAsLine(g, graph, series, pointSize, dataset, plotSettings, defaultStroke);
        }
        g.setClip(clip);
        g.setColor(Color.BLACK);
        g.setStroke(defaultStroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
               
   }  
   
   private void drawSeriesAsLine(Graphics2D g, Graph graph, String series, int pointSize, Dataset dataset, PlotSettings plotSettings, Stroke defaultStroke) {
        double[] xpoints=dataset.getTupleDataByIndex(series, 0);
        double[] ypoints=dataset.getTupleDataByIndex(series, 1);
        BasicStroke stroke=plotSettings.getStrokeSetting(series);
        g.setStroke(stroke);
        if (stroke.getLineWidth()>0) graph.drawCurve(xpoints, ypoints);
        int pointStyle=plotSettings.getPointStyleSetting(series);
        if (pointStyle>=0) {
            g.setStroke(defaultStroke);
            for (int i=0;i<xpoints.length;i++) {
                double x=xpoints[i];double y=ypoints[i];
                graph.drawDataPoint(x, y, pointStyle, pointSize);
            }
        }       
   }
   
   
   private void drawBarChart(Graphics2D g, Dataset dataset, PlotSettings plotSettings) throws ExecutionError {
        int[] tupleSizes=dataset.getMinMaxTupleSize();
        int minTupleSize=tupleSizes[0];
        int maxTupleSize=tupleSizes[1];
        if (minTupleSize<2) throw new ExecutionError("Bar chart requires at least two values (category,y) per data point");       
        int barwidth=17;
        int whiskerswidth=11;
        double barspacing=(Double)plotSettings.getGlobalSetting("barspacing", new Double(0));
        double groupspacing=(Double)plotSettings.getGlobalSetting("groupspacing", new Double(0));
     
        String groupString=(String)plotSettings.getGlobalSetting("group", "series");
        boolean groupByCategories=false;
        if (groupString.equalsIgnoreCase("category") || groupString.equalsIgnoreCase("categories")) groupByCategories=true;
        int bevel=(Integer)plotSettings.getGlobalSetting("bevel", new Integer(-1));
        int shadowOffset=(Integer)plotSettings.getGlobalSetting("shadow", new Integer(0));
        boolean drawWhiskers=(Boolean)plotSettings.getGlobalSetting("whiskers", Boolean.TRUE);   
        boolean doubleWhiskers=(Boolean)plotSettings.getGlobalSetting("doublewhisker", Boolean.FALSE);         
        int numberofseries=dataset.getNumberofSeries();
        ArrayList<String> seriesNames=dataset.getSeriesNames();        
        if (!dataset.isCategorized) {
            boolean test=dataset.forceCategories();
            if (!test) throw new ExecutionError("Dataset for bar chart must either use categories or X-values must be integer starting at 1");
        }
        int categories=dataset.getNumberOfCategories();
        int groups=(groupByCategories)?categories:numberofseries;
        int groupsize=(groupByCategories)?numberofseries:categories;
        double groupwidth=groupsize+(groupsize-1)*barspacing+groupspacing;        

        double minX=0;
        double maxX=numberofseries*categories;
        maxX+=(groups-1)*groupspacing; // add extra space between groups
        maxX+=(groupByCategories)?((numberofseries-1)*categories*barspacing):((categories-1)*numberofseries*barspacing); // add extra space between bars
        maxX+=1; // +1 to create some margin at the right (same as on the left since first bar is at 1.0)
        double[] minmax_Y=dataset.getMinAndMaxFromValues(1);   // NB: make sure to include space for Standard Deviation or stacked data       
        double minY=minmax_Y[0]; 
        double maxY=minmax_Y[1];    
        if (drawWhiskers && maxTupleSize>2) { // data includes standard deviations. make sure the Y-range includes space for SDs
            for (String series:seriesNames) {
                ArrayList<double[]> tuples=dataset.getAllSeriesValues(series);
                for (double[] tuple:tuples) {
                    if (tuple.length>=3) {
                        if (tuple[1]>=0 && tuple[1]+tuple[2]>maxY) maxY=tuple[1]+tuple[2];
                        if (tuple[1]<0 && tuple[1]-tuple[2]<minY) minY=tuple[1]-tuple[2];                        
                        if (tuple[1]>=0 && tuple[1]-tuple[2]<minY) minY=tuple[1]-tuple[2]; // these last two come in effect if the SD is larger than the value itself
                        if (tuple[1]<0  && tuple[1]+tuple[2]>maxY) maxY=tuple[1]+tuple[2]; // these last two come in effect if the SD is larger than the value itself
                    }
                }
            }
        }
        // extend Y range by 10% if needed
        double rangeSize=maxY-minY;
        if (minY<0) minY=minY-(rangeSize*0.1);
        if (maxY>0) maxY=maxY+(rangeSize*0.1);   
        if (maxY>0 && minY>0) minY=0;      
        if (maxY<0 && minY<0) minY=0;  // all negative values         
        
        plotSettings.setGlobalSetting("plotrange", new double[]{minX,maxX,minY,maxY}); // this needs to be passed to the plotSettings
        
        HashMap<String,Object> graphProperties=processPlotSettings(plotSettings, dataset);  
        // allow extending range only
        double newminX=(double)graphProperties.get("minX");
        if (newminX<minX) minX=newminX;
        double newminY=(double)graphProperties.get("minY");
        if (newminY<minY) minY=newminY;        
        double newmaxX=(double)graphProperties.get("maxX");
        if (newmaxX>maxX) maxX=newmaxX;
        double newmaxY=(double)graphProperties.get("maxY");
        if (newmaxY>maxY) maxY=newmaxY;

        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        graphProperties=Graph.layoutGraphImage(true, graphProperties, vizSettings, g); // find best layout
        int translateX=(Integer)graphProperties.get("translateX");   // the X coordinate for the left of the graph
        int translateY=(Integer)graphProperties.get("translateY");   // the Y coordinate for the top of the graph
        int graphwidth=(Integer)graphProperties.get("graphWidth");   // width of graph in pixels (just the actual axis-system)
        int graphheight=(Integer)graphProperties.get("graphHeight"); // height of graph in pixels (just the actual axis-system)        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        // if vertical gridlines are not explicitly set => assing them between each group
        if (!graphProperties.containsKey("verticalGridLines") && numberofseries>1) {
            double[] lines=new double[groups-1];
            for (int i=0;i<lines.length;i++) {
                lines[i]=(groupwidth*(i+1))-(groupspacing/2)+0.5;
            }            
            graphProperties.put("verticalGridLines",lines);
        }
        // use tickmarks based on series or categories
        HashMap<Double,String> labels=new HashMap<>();
        ArrayList<String> labelStrings=(groupByCategories)?dataset.categories:seriesNames;
        for (int i=0;i<labelStrings.size();i++) {
            double groupHalfWidth=(groupsize+(groupsize-1)*barspacing)/2;
            double labelXpos=groupwidth*i+groupHalfWidth+0.5; // 
            labels.put(labelXpos, labelStrings.get(i));
        }          
        graphProperties.put("xTickLabels", labels);
        Graph graph=new Graph(g, minX, maxX, minY, maxY, graphwidth, graphheight, translateX, translateY);
        graph.drawAxesAndStuff(graphProperties, vizSettings); // draws axes, ticks, labels, titles and legend box as specified
        Shape clip=g.getClip();       
        g.setClip(translateX, translateY, graphwidth, graphheight); // do not allow drawing outside the graphbox (in case ranges have been adjusted smaller than the actual graph)       
        Stroke defaultStroke=g.getStroke();     
        g.setStroke(defaultStroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);       
        if (plotSettings.hasGlobalSetting("barwidth")) {
            double width=plotSettings.getGlobalSettingAsNumber("barwidth");
            if (width>0 && width<=1) { // regard as percentage
              int factor=graph.getXforValue(1.0)-graph.getXforValue(0); // number of pixels between x-values 0 and 1 in the graph
              barwidth=(int)(factor*width);
            } else if (width>1) barwidth=(int)width;
        }
        if (plotSettings.hasGlobalSetting("whiskerswidth")) {
            double width=plotSettings.getGlobalSettingAsNumber("whiskerswidth");
            if (width>0 && width<=1) { // regard as percentage
              int factor=graph.getXforValue(1.0)-graph.getXforValue(0); // number of pixels between x-values 0 and 1 in the graph
              whiskerswidth=(int)(factor*width);
            } else if (width>1) whiskerswidth=(int)width;
        } else whiskerswidth=barwidth/2;        
        
        if (plotSettings.hasGlobalSetting("fillcolor")) plotSettings.setGlobalSetting("fill", plotSettings.getGlobalSetting("fillcolor")); // copy setting to allow alias
        String fill=(String)plotSettings.getGlobalSetting("fill","color");  
        String whiskercolorString=(String)plotSettings.getGlobalSetting("whiskercolor","color");     
        
        for (int i=0;i<numberofseries;i++) {
            String series=seriesNames.get(i);        
            ArrayList<double[]> tuples=dataset.getAllSeriesValues(series);
            Color color=plotSettings.getColorSetting(series);
            Color whiskerColor=Graph.getColorFromSetting(whiskercolorString, color);
            if (tuples.size()<categories) throw new ExecutionError("Bar chart dataset appears to be missing some values for series '"+series+"' (expected "+categories+" found "+tuples.size()+")");
            for (int j=0;j<tuples.size();j++) { // each series should already have been sorted by X-values when parsing the data, so the tuples should be correctly ordered
                double[] tuple=tuples.get(j);
                int group=(groupByCategories)?j:i; // counting from 0
                int withingroup=(groupByCategories)?i:j; // counting from 0
                double useX=group*groupwidth+withingroup+(withingroup*barspacing)+1;
                int xPos=graph.getXforValue(useX)-barwidth/2;
                if (tuple.length>=3 && tuple[2]<0) throw new ExecutionError("Standard deviation values can not be negative (for series '"+series+"')");
                graph.drawBar(xPos, tuple[1], (tuple.length>=3)?tuple[2]:0, color, fill, barwidth, (drawWhiskers)?whiskerswidth:0, whiskerColor, doubleWhiskers, bevel, shadowOffset);
            }
        }        
        g.setClip(clip);
        g.setColor(Color.BLACK);
        g.setStroke(defaultStroke);
        if ((Boolean)plotSettings.getGlobalSetting("axisline", Boolean.TRUE)) graph.drawHorizontalGridLine(graph.getYforValue(0), Graph.SOLID, Color.BLACK); 
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);              
   }    
   
   
   private void drawBoxPlot(Graphics2D g, Dataset dataset, PlotSettings plotSettings) throws ExecutionError {
        boolean drawAverageStdDev=(Boolean)plotSettings.getGlobalSetting("average", Boolean.FALSE);
        boolean drawOutliers=(Boolean)plotSettings.getGlobalSetting("outliers", Boolean.TRUE);
        boolean drawQuartilesBox=(Boolean)plotSettings.getGlobalSetting("plotbox", Boolean.TRUE);
        boolean drawWhiskers=(Boolean)plotSettings.getGlobalSetting("whiskers", Boolean.TRUE);
        boolean drawStdDev=(Boolean)plotSettings.getGlobalSetting("SD", drawAverageStdDev); // if drawAverageStdDev==TRUE, then draw Std unless explicitly told otherwise
        int boxwidth=17;
        int whiskerswidth=17;
        int bevel=(Integer)plotSettings.getGlobalSetting("bevel", new Integer(0));
        int shadowOffset=(Integer)plotSettings.getGlobalSetting("shadow", new Integer(0));
        int diamondsize=(Integer)plotSettings.getGlobalSetting("diamondsize", new Integer(5));
        int numberofseries=dataset.getNumberofSeries();
        if (plotSettings.hasGlobalSetting("fillcolor")) plotSettings.setGlobalSetting("fill", plotSettings.getGlobalSetting("fillcolor")); // copy setting to allow alias
        ArrayList<double[]> statistics=new ArrayList<>();        
        if (plotSettings.hasGlobalSetting("percentiles")) { // calculate statistics from data
            double[] percentiles=plotSettings.getNumericValues("percentiles", 0, 100);
            checkPercentiles(percentiles);
            for (int i=0;i<5;i++) percentiles[i]=percentiles[i]/100; // scale between 0 and 1.0            
            for (String series:dataset.getSeriesNames()) {
                ArrayList<Double> values=dataset.getAllSeriesValuesFlattened(series);
                if (values.isEmpty()) throw new ExecutionError("Data series '"+series+"' has no values");
                Collections.sort(values);
                double min=(percentiles[0]==0)?values.get(0):MotifLabEngine.getPercentileValue(values,percentiles[0]);
                double first=MotifLabEngine.getPercentileValue(values,percentiles[1]);
                double median=(percentiles[2]==0.5)?MotifLabEngine.getMedianValue(values):MotifLabEngine.getPercentileValue(values,percentiles[2]);
                double third=MotifLabEngine.getPercentileValue(values,percentiles[3]);
                double max=(percentiles[4]==1.0)?values.get(values.size()-1):MotifLabEngine.getPercentileValue(values,percentiles[4]);             
                double[] mean=MotifLabEngine.getAverageAndStandardDeviation(values);
                ArrayList<Double> outliers=new ArrayList<>();
                for (Double value:values) {
                    if (value<min || value>max) outliers.add(value);
                }
                double[] finalstats=new double[7+outliers.size()];
                finalstats[0]=min;finalstats[1]=first;finalstats[2]=median;finalstats[3]=third;finalstats[4]=max;
                finalstats[5]=mean[0];finalstats[6]=mean[1];
                for (int i=0;i<outliers.size();i++) finalstats[7+i]=outliers.get(i);
                statistics.add(finalstats);
            }                                  
        } else { // assume statistics are provided directly. Just validate and copy into statistics
           for (String series:dataset.getSeriesNames()) {
               ArrayList<double[]> values=dataset.getAllSeriesValues(series);
               if (values.isEmpty()) throw new ExecutionError("Data series '"+series+"' has no values");
               double[] finalstat=values.get(0);
               if (finalstat.length<5) throw new ExecutionError("The data for series '"+series+"' should be at least five numbers: min, 1st quartile, median, 3rd quartile, max, [average, std.dev], [outliers]");
               double prev=Double.MIN_VALUE;
               for (int i=0;i<5;i++) {
                   if (finalstat[i]<prev) throw new ExecutionError("The 'min,1stQuart,median,3rdQuart,max' values for data series '"+series+"' are not in sorted order");
                   prev=finalstat[i];
               }
               if (finalstat.length==6) throw new ExecutionError("Missing value for 'standard deviation' to go with the 'mean' for data series '"+series+"'");
               statistics.add(finalstat);
           } 
        }
        // set range based on data
        double minX=0;
        double maxX=numberofseries+1; // to create some margin
        double minY=Double.MAX_VALUE; // ? This could be negative
        double maxY=Double.MIN_VALUE; // set 10% higher than highest value in dataset
        for (int i=0;i<statistics.size();i++) {
            double[] stat=statistics.get(i);
            if (stat[0]<minY) minY=stat[0];
            if (stat[4]>maxY) maxY=stat[4]; 
            if(stat.length>=7) { // make sure avg/std could be included
                if (stat[5]-stat[6]<minY) minY=stat[5]-stat[6];                
                if (stat[5]+stat[6]>maxY) maxY=stat[5]+stat[6];
            }
            if (stat.length>7) { // make sure all outliers are included
                for (int j=6;j<stat.length;j++) {
                   if (stat[j]<minY) minY=stat[j];                
                   if (stat[j]>maxY) maxY=stat[j];                    
                }
            }
        }
        // extend Y range by 10%
        double rangeSize=maxY-minY;
        minY=minY-(rangeSize*0.1);
        maxY=maxY+(rangeSize*0.1);
        
        plotSettings.setGlobalSetting("plotrange", new double[]{minX,maxX,minY,maxY}); // this needs to be passed to the plotSettings
        
        HashMap<String,Object> graphProperties=processPlotSettings(plotSettings, dataset);  
        // allow extending range only
        double newminX=(double)graphProperties.get("minX");
        if (newminX<minX) minX=newminX;
        double newminY=(double)graphProperties.get("minY");
        if (newminY<minY) minY=newminY;        
        double newmaxX=(double)graphProperties.get("maxX");
        if (newmaxX>maxX) maxX=newmaxX;
        double newmaxY=(double)graphProperties.get("maxY");
        if (newmaxY>maxY) maxY=newmaxY;

        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        graphProperties=Graph.layoutGraphImage(true, graphProperties, vizSettings, g); // find best layout
        int translateX=(Integer)graphProperties.get("translateX");   // the X coordinate for the left of the graph
        int translateY=(Integer)graphProperties.get("translateY");   // the Y coordinate for the top of the graph
        int graphwidth=(Integer)graphProperties.get("graphWidth");   // width of graph in pixels (just the actual axis-system)
        int graphheight=(Integer)graphProperties.get("graphHeight"); // height of graph in pixels (just the actual axis-system)        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        // if vertical gridlines are not explicitly set => assing them between each box
        if (!graphProperties.containsKey("verticalGridLines") && numberofseries>1) {
            double[] lines=new double[numberofseries-1];
            for (int i=1;i<numberofseries;i++) {
                lines[i-1]=i+0.5;
            }
            graphProperties.put("verticalGridLines",lines);
        }
        // use tickmarks based on series
        HashMap<Double,String> labels=new HashMap<>();
        ArrayList<String> seriesNames=dataset.getSeriesNames();
        for (int i=0;i<seriesNames.size();i++) labels.put((double)(i+1), seriesNames.get(i));
        graphProperties.put("xTickLabels", labels);
        Graph graph=new Graph(g, minX, maxX, minY, maxY, graphwidth, graphheight, translateX, translateY);
        graph.drawAxesAndStuff(graphProperties, vizSettings); // draws axes, ticks, labels, titles and legend box as specified
        Shape clip=g.getClip();       
        g.setClip(translateX, translateY, graphwidth, graphheight); // do not allow drawing outside the graphbox (in case ranges have been adjusted smaller than the actual graph)       
        Stroke defaultStroke=g.getStroke();     
        g.setStroke(defaultStroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);       
        if (plotSettings.hasGlobalSetting("boxwidth")) {
            double width=plotSettings.getGlobalSettingAsNumber("boxwidth");
            if (width>0 && width<=1) { // regard as percentage
              int factor=graph.getXforValue(1.0)-graph.getXforValue(0); // number of pixels between x-values 0 and 1 in the graph
              boxwidth=(int)(factor*width);
            } else if (width>1) boxwidth=(int)width;
        }
        if (plotSettings.hasGlobalSetting("whiskerswidth")) {
            double width=plotSettings.getGlobalSettingAsNumber("whiskerswidth");
            if (width>0 && width<=1) { // regard as percentage
              int factor=graph.getXforValue(1.0)-graph.getXforValue(0); // number of pixels between x-values 0 and 1 in the graph
              whiskerswidth=(int)(factor*width);
            } else if (width>1) whiskerswidth=(int)width;
        } else whiskerswidth=boxwidth;        
        
        for (int i=0;i<statistics.size();i++) {
            String series=seriesNames.get(i);        
            double[] data=statistics.get(i);
            double x=i+1;
            if (data.length<5) continue; // not enough statistics?!
            Color color=plotSettings.getColorSetting(series);
            g.setColor(color);
            Paint fillcolor=null;
            int xPos=graph.getXforValue(x);
            Rectangle box=graph.getBoxRectangle(data[1], data[3], xPos, boxwidth);
            if (plotSettings.hasGlobalSetting("fill")) {
                String fill=plotSettings.getGlobalSetting("fill").toString();
                if (fill.equals("none")) fillcolor=null;
                else if (fill.equals("bar") || fill.equals("hbar")) fillcolor=Graph.getHorizontalGradientPaint(color, box.width, box.x);
                else if (fill.equals("vbar")) fillcolor=Graph.getVerticalGradientPaint(color, box.height, box.y);
                else if (fill.startsWith("gradient")) {
                    int dir=0;
                    if (fill.startsWith("gradient:")) try {dir=Integer.parseInt(fill.substring("gradient:".length()));} catch (Throwable e) {}
                    fillcolor=Graph.getDiagonalGradientPaint(color, box.x, box.y, box.width, box.height, dir);
                }
                else fillcolor=Graph.getColorFromSetting(fill, color);  
            } else fillcolor=new Color(color.getRed(),color.getGreen(),color.getBlue(),80);            
            
            if (drawOutliers && data.length>=8) { // draw outliers first so they are beneath the plot just in case
                if (plotSettings.hasGlobalSetting("outliercolor")) {
                    g.setColor(Graph.getColorFromSetting(plotSettings.getGlobalSetting("outliercolor").toString(),color));
                }
                int pointSize=(int)plotSettings.getGlobalSetting("point size", new Integer(3));                         
                int pointStyle=plotSettings.getPointStyleSetting(series);
                for (int j=7;j<data.length;j++) {
                    graph.drawDataPoint(x, data[j], pointStyle, pointSize);
                }                 
            }            
            if (shadowOffset>0) {
                g.setColor(shadowColor);
                g.translate(shadowOffset, shadowOffset);
                if (drawQuartilesBox) graph.drawVerticalBoxAndWhiskers(data[0], data[4], data[1], data[1],  data[3],  xPos, boxwidth, (drawWhiskers)?whiskerswidth:0, (fillcolor!=null)?shadowColor:null, (fillcolor==null)?0:-1); // the median is drawn at the first quartile
                if (drawAverageStdDev && data.length>=7) { // shadow for avg/std. 
                    if (drawQuartilesBox && fillcolor!=null) { // The following clipping trick excludes the shadow from the drawQuartilesBox box, so that shadows are not drawn on top of each other
                        Shape saveclip=g.getClip();                      
                        Shape exclude=excludeClip(g, saveclip.getBounds(), graph.getYforValue(data[3]), xPos-boxwidth/2, graph.getYforValue(data[1]), xPos-boxwidth/2+boxwidth);
                        g.setClip(exclude);
                        graph.drawVerticalMeanAndStdDeviation(data[5], data[6], xPos, (drawStdDev)?whiskerswidth:0, diamondsize, -1);  
                        g.setClip(saveclip);
                    } else { // either dont draw quartiles box or just draw the outline of it. Draw 
                        graph.drawVerticalMeanAndStdDeviation(data[5], data[6], xPos, (drawStdDev)?whiskerswidth:0, diamondsize, -1);
                    }
                }         
                g.translate(-shadowOffset, -shadowOffset);
                g.setColor(color);
            }            
            if (drawQuartilesBox) graph.drawVerticalBoxAndWhiskers(data[0], data[4], data[2], data[1],  data[3],  xPos, boxwidth, (drawWhiskers)?whiskerswidth:0, fillcolor, bevel);
            
            if (drawAverageStdDev && data.length>=7) {
                graph.drawVerticalMeanAndStdDeviation(data[5], data[6], xPos, (drawStdDev)?whiskerswidth:0, diamondsize, bevel);
            }
        }
        g.setClip(clip);
        g.setColor(Color.BLACK);
        g.setStroke(defaultStroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);              
   } 
   
   
   private boolean checkPercentiles(double[] percentiles) throws ExecutionError {
        if (percentiles.length<5) throw new ExecutionError("The 'percentiles' list must begin with 5 numbers between 0 and 100 in sorted order");
        double prev=-1;   
        for (int i=0;i<5;i++) {
            if (percentiles[i]<=prev || percentiles[i]<0 || percentiles[i]>100) throw new ExecutionError("The 'percentiles' list must begin with 5 numbers between 0 and 100 in sorted order");
            prev=percentiles[i];
        }
        return true;
   }   
   
   private void drawHistogram(Graphics2D g, Dataset dataset, PlotSettings plotSettings) throws ExecutionError {
        HashMap<String,double[]> histograms=new HashMap<>();
        int bins=(int)plotSettings.getGlobalSetting("bins", new Integer(100));
        double[] rangeX=dataset.getMinAndMaxFromValues();
        double[] rangeY=new double[]{0,0};
        if (plotSettings.hasGlobalSetting("minX")) rangeX[0]=(double)plotSettings.getGlobalSetting("minX",rangeX[0]);
        if (plotSettings.hasGlobalSetting("maxX")) rangeX[1]=(double)plotSettings.getGlobalSetting("maxX",rangeX[1]);
        if (rangeX[1]<rangeX[0]) rangeX[1]=rangeX[0]+1; // just in case
        double binrange=(rangeX[1]-rangeX[0])/(double)bins;
        for (String series:dataset.getSeriesNames()) {
           double[] counts=new double[bins];
           ArrayList<double[]> alldata=dataset.getAllSeriesValues(series);
           for (double[] tuple:alldata) {
              for (double value:tuple) {
                 int bin=(int)((value-rangeX[0])/binrange);
                 if (bin>=bins) bin=bins-1;  
                 if (bin<0) bin=0;
                 counts[bin]++;                   
              }
           }
           histograms.put(series, counts);
           for (int i=0;i<counts.length;i++) {
              if (counts[i]>rangeY[1]) rangeY[1]=counts[i];
           } 
        }
        
        if (plotSettings.hasGlobalSetting("maxY")) rangeY[1]=(double)plotSettings.getGlobalSetting("maxY",rangeY[1]);
        
        plotSettings.setGlobalSetting("plotrange", new double[]{rangeX[0],rangeX[1],rangeY[0],rangeY[1]}); // this needs to be passed to the plotSettings        
        HashMap<String,Object> graphProperties=processPlotSettings(plotSettings, dataset); // this could change the ranges!
        
        double minX=(double)graphProperties.get("minX");
        double maxX=(double)graphProperties.get("maxX");
        double maxY=(double)graphProperties.get("maxY");
        boolean outline=(boolean)plotSettings.getGlobalSetting("outline", Boolean.TRUE); 
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        graphProperties=Graph.layoutGraphImage(true, graphProperties, vizSettings, g); // find best layout
        int translateX=(Integer)graphProperties.get("translateX");   // the X coordinate for the left of the graph
        int translateY=(Integer)graphProperties.get("translateY");   // the Y coordinate for the top of the graph
        int graphwidth=(Integer)graphProperties.get("graphWidth");   // width of graph in pixels (just the actual axis-system)
        int graphheight=(Integer)graphProperties.get("graphHeight"); // height of graph in pixels (just the actual axis-system)        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Graph graph=new Graph(g, minX, maxX, 0, maxY, graphwidth, graphheight, translateX, translateY);
        graph.drawAxesAndStuff(graphProperties, vizSettings); // draws axes, ticks, labels, titles and legend box as specified
        Shape clip=g.getClip();       
        g.setClip(translateX, translateY, graphwidth, graphheight); // do not allow drawing outside the graphbox (in case ranges have been adjusted smaller than the actual graph)
       
        for (String series:dataset.getSeriesNames()) {
            Color color=plotSettings.getColorSetting(series);
            g.setColor(color);
            double[] counts=histograms.get(series);
            graph.drawHistogram(counts, outline, rangeX[0], rangeX[1]);
        }
        g.setClip(clip);
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);        
   }    
   

  private void drawPieChart(Graphics2D g, Dataset dataset, PlotSettings plotSettings, VisualizationSettings vizSettings) throws ExecutionError {
        if (dataset.getMinTupleSize()<1) throw new ExecutionError("Pie chart requires at least one value per data point");      
        int width=g.getClipBounds().width; // 
        int height=g.getClipBounds().height;
        boolean outline=(boolean)plotSettings.getGlobalSetting("border",  Boolean.TRUE);
        boolean outlineSegments=(boolean)plotSettings.getGlobalSetting("segmentBorder",  Boolean.FALSE);
        boolean bevel=(boolean)plotSettings.getGlobalSetting("bevel",  Boolean.FALSE);
        int translateX=1;   // the X coordinate for the left of the graph
        int translateY=1;   // the Y coordinate for the top of the graph
        int graphwidth=width-2; // width of graph in pixels (make room for a little border or bleed)
        int graphheight=height-2; // width of graph in pixels  (make room for a little border or bleed)  
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        String legend=(String)plotSettings.getGlobalSetting("legend", "none");
        boolean horizontal=(boolean)plotSettings.getGlobalSetting("legendHorizontal", Boolean.FALSE); 
        boolean legendBox=(boolean)plotSettings.getGlobalSetting("legendBox", Boolean.TRUE); 
        boolean forceRound=(boolean)plotSettings.getGlobalSetting("round", Boolean.TRUE); 
        int legendSpacing=(int)plotSettings.getGlobalSetting("legendSpacing", new Integer(10)); 
        int legendMargin=(int)plotSettings.getGlobalSetting("legendMargin", new Integer(20)); 
        String[] series=new String[dataset.getNumberofSeries()];
        series=dataset.getSeriesNames().toArray(series);
        int legendX=1;int legendY=1;
        Font legendFont=vizSettings.getSystemFont("graph.legendFont");        
        if (!(legend.equalsIgnoreCase("none"))) {
            Dimension dim=(horizontal)?Graph.getHorizontalLegendDimension(series, legendSpacing, legendFont):Graph.getLegendDimension(series, legendFont);
            if (legend.equalsIgnoreCase("top")) {
                int used=dim.height+legendMargin;                
                translateY=used;
                graphheight-=used;
            } else if (legend.equalsIgnoreCase("bottom")) {
                int used=dim.height+legendMargin;
                graphheight-=used;
                legendY=graphheight+legendMargin;
            } else if (legend.equalsIgnoreCase("left")) {
                int used=dim.width+legendMargin;
                translateX=used;                
                graphwidth-=used;
            } else if (legend.equalsIgnoreCase("right")) {
                int used=dim.width+legendMargin;
                graphwidth-=used;
                legendX=graphwidth+legendMargin;
            }
            if (graphwidth<10) graphwidth=10;
            if (graphheight<10) graphheight=10;
        }
        if (forceRound && graphwidth!=graphheight) {
            if (graphwidth<graphheight) graphheight=graphwidth; 
            else graphwidth=graphheight;
        }
        Graph graph=new Graph(g, 0,1,0,1, graphwidth, graphheight, translateX, translateY);
        int size=dataset.getNumberofSeries();    
        double[] values=new double[size];
        Color[] colors=new Color[size];

        for (int i=0;i<size;i++) colors[i]=vizSettings.getColorFromIndex(i); // use as default
        int index=0;
        for (String seriesName:dataset.getSeriesNames()) {
            double[] datapoints=dataset.getTupleDataByIndex(seriesName, 0);
            values[index]=(datapoints.length>0)?datapoints[0]:0; // only use first value from each series
            if (plotSettings.hasSetting(seriesName,"color")) colors[index]=plotSettings.getColorSetting(seriesName);
            index++;
        }
        graph.drawPieChart(values, colors, outline, outlineSegments, bevel);      
        g.setColor(Color.BLACK);
        if (!legend.equalsIgnoreCase("none")) {
            Font usedFont=g.getFont();
            g.setFont(legendFont);
            if (horizontal) graph.drawHorizontalLegendBox(series, colors, legendX, legendY, legendSpacing, legendBox);
            else graph.drawLegendBox(series, colors, legendX, legendY, legendBox);
            g.setFont(usedFont);
        }                
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
   }   
  
  private void drawLegend(Graphics2D g, String[] strings, Color[] colors, boolean box, boolean horizontal, int legendSpacing, Font legendFont) { // draw legend as separate image
        g.setFont(legendFont);
        Graph legendGraph=new Graph(g, 0, 0, 0, 0, 0, 0, 0, 0); // just a mock graph
        if (horizontal) legendGraph.drawHorizontalLegendBox(strings, colors, 0, 0, legendSpacing, box);
        else legendGraph.drawLegendBox(strings, colors, 0, 0, box);     
  }
   
    
  /** Converts plot-settings to the format used by the Graph class */
  private HashMap<String,Object> processPlotSettings(PlotSettings plotSettings, Dataset dataset) {
        HashMap<String,Object> graphProperties=new HashMap<>();        
        double[] range=(double[])plotSettings.getGlobalSetting("plotrange", null); // this has to be set before calling this method!
        double[] minmax_X=(range==null)?new double[]{0,1}:new double[]{range[0],range[1]};
        double[] minmax_Y=(range==null)?new double[]{0,1}:new double[]{range[2],range[3]};
        
        if (plotSettings.hasGlobalSetting("extendRange")) {    
            String extendString=(String)plotSettings.getGlobalSetting("extendRange");
            String[] parts=extendString.trim().split("\\s*,\\s*");
            if (parts.length==1) parts=new String[]{parts[0],parts[0],parts[0],parts[0]};
            else if (parts.length==2) parts=new String[]{parts[0],parts[0],parts[1],parts[1]};
            if (parts.length==3) parts=new String[]{parts[0],parts[1],parts[2],parts[2]};
            double xRange=minmax_X[1]-minmax_X[0];
            double yRange=minmax_Y[1]-minmax_Y[0];
            for (int i=0;i<4;i++) {
                boolean isPercentage=false;
                String part=parts[i];
                if (part.endsWith("%")) {part=part.substring(0,part.length()-1);isPercentage=true;}
                double value=0;
                try {value=Double.parseDouble(part);} catch (NumberFormatException e){}
                if (value<0) value=-value; // value should be positive!
                if (isPercentage) value=(value/100.0);
                if (i==0) {
                   if (isPercentage) {minmax_X[0]=minmax_X[0]-(xRange*value);}
                   else minmax_X[0]=minmax_X[0]-value;
                } else if (i==1) {
                   if (isPercentage) {minmax_X[1]=minmax_X[1]+(xRange*value);}
                   else minmax_X[1]=minmax_X[1]+value;                    
                } else if (i==2) {
                   if (isPercentage) {minmax_Y[0]=minmax_Y[0]-(yRange*value);}
                   else minmax_Y[0]=minmax_Y[0]-value;                    
                } else if (i==3) {
                   if (isPercentage) {minmax_Y[1]=minmax_Y[1]+(yRange*value);}
                   else minmax_Y[1]=minmax_Y[1]+value;                      
                }
            }
        } 
        // explicitly defined range settings will override any extendRange adjustments
        if (plotSettings.hasGlobalSetting("minX")) minmax_X[0]=(double)plotSettings.getGlobalSetting("minX",  minmax_X[0]);
        if (plotSettings.hasGlobalSetting("maxX")) minmax_X[1]=(double)plotSettings.getGlobalSetting("maxX",  minmax_X[1]);
        if (plotSettings.hasGlobalSetting("minY")) minmax_Y[0]=(double)plotSettings.getGlobalSetting("minY",  minmax_Y[0]);
        if (plotSettings.hasGlobalSetting("maxY")) minmax_Y[1]=(double)plotSettings.getGlobalSetting("maxY",  minmax_Y[1]); 

        int axisLayout=Graph.BOX;
        int horizontalGrid=Graph.DOTTED;
        int verticalGrid=Graph.DOTTED;
        boolean boundingBox=true;
        boolean drawXticks=true;
        boolean drawYticks=true;
        boolean xPercentage=false;
        boolean yPercentage=false;
        boolean verticalXTicks=false;
        boolean verticalXTicksUp=false;

        String xLabel=(String)plotSettings.getGlobalSetting("xLabel","");
        String yLabel=(String)plotSettings.getGlobalSetting("yLabel","");
        String title=(String)plotSettings.getGlobalSetting("title","");
        String legend=(String)plotSettings.getGlobalSetting("legend","none");
        HashMap<Double,String> xTickLabels=null;
        HashMap<Double,String> yTickLabels=null;
        if (plotSettings.globalSettingEquals("axis","cross")) axisLayout=Graph.CROSS;
        if (plotSettings.hasGlobalSetting("verticalGrid")) plotSettings.setGlobalSetting("vgrid", plotSettings.getGlobalSetting("verticalGrid")); // copy for backwards compatibility
        if (plotSettings.hasGlobalSetting("horizontalGrid")) plotSettings.setGlobalSetting("hgrid", plotSettings.getGlobalSetting("horizontalGrid")); // copy for backwards compatibility
        String vGrid=(String)plotSettings.getGlobalSettingAsClass("vgrid",String.class);
        if (vGrid!=null) {
            if (vGrid.equalsIgnoreCase("none")) verticalGrid=Graph.NONE;    
            if (vGrid.startsWith("dot")) verticalGrid=Graph.DOTTED;    
            if (vGrid.startsWith("dash")) verticalGrid=Graph.DASHED;    
            if (vGrid.equalsIgnoreCase("line") || vGrid.equalsIgnoreCase("solid")) verticalGrid=Graph.SOLID;
        }
        String hGrid=(String)plotSettings.getGlobalSettingAsClass("hgrid",String.class);
        if (hGrid!=null) {
            if (hGrid.equalsIgnoreCase("none")) horizontalGrid=Graph.NONE;    
            if (hGrid.startsWith("dot")) horizontalGrid=Graph.DOTTED;    
            if (hGrid.startsWith("dash")) horizontalGrid=Graph.DASHED;    
            if (hGrid.equalsIgnoreCase("line") || hGrid.equalsIgnoreCase("solid")) horizontalGrid=Graph.SOLID;
        }
        if (plotSettings.hasGlobalSetting("horizontalGridLines")) {
            double[] gridlines=plotSettings.getNumericValues("horizontalGridLines",minmax_Y[0],minmax_Y[1]);
            graphProperties.put("horizontalGridLines", gridlines);
        }
        if (plotSettings.hasGlobalSetting("verticalGridLines")) {
            double[] gridlines=plotSettings.getNumericValues("verticalGridLines",minmax_X[0],minmax_X[1]);
            graphProperties.put("verticalGridLines", gridlines);
        }        
        if (plotSettings.hasGlobalSetting("tickX") && plotSettings.globalSettingEquals("tickX", Boolean.FALSE)) drawXticks=false;
        if (plotSettings.hasGlobalSetting("tickY") && plotSettings.globalSettingEquals("tickY", Boolean.FALSE)) drawYticks=false;
        if (plotSettings.hasGlobalSetting("box") && plotSettings.globalSettingEquals("box", Boolean.FALSE)) boundingBox=false;      
        yPercentage=plotSettings.globalSettingEquals("percentageY",Boolean.TRUE);     
        xPercentage=plotSettings.globalSettingEquals("percentageX",Boolean.TRUE); 
        verticalXTicks=plotSettings.globalSettingEquals("verticalXticks",Boolean.TRUE);
        verticalXTicksUp=plotSettings.globalSettingEquals("verticalXticksUp",Boolean.TRUE);
        if (dataset.isCategorized) { // get tick labels from dataset
            drawXticks=false;
            xTickLabels=dataset.getCategoryIndexMap();           
        }        
        if (plotSettings.hasGlobalSetting("tickXLabels")) {
            drawXticks=false;
            xTickLabels=plotSettings.getTicks("tickXLabels");        
        }
        if (plotSettings.hasGlobalSetting("tickYLabels")) {
            drawYticks=false;
            yTickLabels=plotSettings.getTicks("tickYLabels");        
        }       
        
        int width=(int)plotSettings.getGlobalSetting("imageWidth", new Integer(100));
        int height=(int)plotSettings.getGlobalSetting("imageHeight", new Integer(100));
        graphProperties.put("imageWidth", width);
        graphProperties.put("imageHeight", height);
        graphProperties.put("drawXticks", (drawXticks || plotSettings.hasGlobalSetting("tickXLabels")));
        graphProperties.put("drawYticks", drawYticks);  
        graphProperties.put("minX", minmax_X[0]); 
        graphProperties.put("maxX", minmax_X[1]); 
        graphProperties.put("minY", minmax_Y[0]); 
        graphProperties.put("maxY", minmax_Y[1]); 
        graphProperties.put("formatAsPercentageX", xPercentage);
        graphProperties.put("formatAsPercentageY", yPercentage);   
        graphProperties.put("verticalXticks", verticalXTicks); 
        graphProperties.put("verticalXticksUp", verticalXTicksUp); 
        graphProperties.put("box",boundingBox);
        graphProperties.put("horizontalGrid",horizontalGrid);
        graphProperties.put("verticalGrid",verticalGrid);
        graphProperties.put("axisLayout",axisLayout);        
        if (xTickLabels!=null) graphProperties.put("xTickLabels",xTickLabels);
        if (yTickLabels!=null) graphProperties.put("yTickLabels",yTickLabels);
        if (xLabel!=null && !xLabel.isEmpty()) graphProperties.put("xLabel",xLabel);
        if (yLabel!=null && !yLabel.isEmpty()) graphProperties.put("yLabel",yLabel);
        if (title!=null && !title.isEmpty()) graphProperties.put("title",title);
        graphProperties.put("margin", (int)plotSettings.getGlobalSetting("margin", new Integer(5)));
        graphProperties.put("yLabelMargin", (int)plotSettings.getGlobalSetting("yLabelMargin", new Integer(10)));
        graphProperties.put("xLabelMargin", (int)plotSettings.getGlobalSetting("xLabelMargin", new Integer(10)));
        graphProperties.put("yTicksMargin", (int)plotSettings.getGlobalSetting("yTicksMargin", new Integer(10)));
        graphProperties.put("xTicksMargin", (int)plotSettings.getGlobalSetting("xTicksMargin", new Integer(10)));
        graphProperties.put("titleMargin", (int)plotSettings.getGlobalSetting("titleMargin", new Integer(10)));          
        
        if (!legend.equalsIgnoreCase("none")) { // include legend
            String[] legendStrings=null;
            Color[] legendColors=null;
            if (plotSettings.hasGlobalSetting("legendText")) {
                String string=(String)plotSettings.getGlobalSettingAsClass("legendText", String.class);
                if (string==null) string="error";
                legendStrings=string.trim().split("\\s*,\\s*");
                legendColors=new Color[legendStrings.length];
                for (int i=0;i<legendStrings.length;i++) {
                    legendColors[i]=plotSettings.getColorSetting(legendStrings[i]);
                }                  
            } else {
                legendStrings=new String[dataset.getNumberofSeries()];
                legendColors=new Color[dataset.getNumberofSeries()];
                int index=0;
                for (String series:dataset.getSeriesNames()) {
                    legendStrings[index]=series;
                    legendColors[index]=plotSettings.getColorSetting(series);
                    index++;
                }       
            }
            graphProperties.put("legend",legendStrings);
            graphProperties.put("legendColors",legendColors);
            graphProperties.put("legendPlacement",legend);
            graphProperties.put("legendMargin",(int)plotSettings.getGlobalSetting("legendMargin",  new Integer(20)));
            graphProperties.put("legendSpacing",(int)plotSettings.getGlobalSetting("legendSpacing",  new Integer(10)));
            graphProperties.put("legendBox",(boolean)plotSettings.getGlobalSetting("legendBox",  Boolean.TRUE));  
            graphProperties.put("legendHorizontal",(boolean)plotSettings.getGlobalSetting("legendHorizontal",  Boolean.FALSE));              
        }  
        return graphProperties;
  }
  
  
   /**
    * A class to hold the graph data, including some convenience methods for accessing it in various ways
    */
   private class Dataset {       
       private HashMap<String,ArrayList<double[]>> dataset=null;
       private boolean isCategorized=false;
       private ArrayList<String> categories=null; 
       private ArrayList<String> seriesNames=null; // keep series in some sorted order
       
       private Comparator<double[]> datasorter=new Comparator<double[]>() { // sorts tuples by first value (X-values)
            @Override
            public int compare(double[] d1, double[] d2) {
                return Double.compare(d1[0], d2[0]);
            }      
       };
               
       public Dataset() {
           
       }
            
       public void parseData(String dataFormat, ArrayList<String> data, String separator, boolean sort) throws ExecutionError {
           if (dataFormat.equalsIgnoreCase(DATAFORMAT_LINES)) parseDataLines(data, separator, sort);
           else if (dataFormat.equalsIgnoreCase(DATAFORMAT_COLUMNS)) parseDataColumns(data, separator, sort);
           else if (dataFormat.equalsIgnoreCase(DATAFORMAT_ROWS)) parseDataRows(data, separator, sort);
           else throw new ExecutionError("Unknown format for graph data: "+dataFormat);
       }
       
       private void parseDataLines(ArrayList<String> data, String separator, boolean sort) throws ExecutionError {
           seriesNames=new ArrayList<String>();
           HashMap<String,ArrayList<double[]>> results=new HashMap<>();
           for (String line:data) {
               if (line.isEmpty() || line.startsWith("#")) continue; // why not...
               String[] parts=line.split(separator);
               if (parts.length<2) throw new ExecutionError("Expected at least two columns in data");
               String item=parts[0];
               if (!results.containsKey(item)) {
                   results.put(item,new ArrayList<double[]>());
                   seriesNames.add(item);
               }
               ArrayList<double[]> values=results.get(item);
               double[] tuple=new double[parts.length-1];
               for (int i=0;i<tuple.length;i++) {
                   String valueAsString=parts[i+1];
                   try {
                       double value=Double.parseDouble(valueAsString);
                       tuple[i]=value;
                   } catch (NumberFormatException e) {
                       if (i==0) { // The second column is not a numeric value. Treat it as a category value
                           tuple[i]=getIndexForCategory(valueAsString);
                           isCategorized=true;
                       } 
                       else throw new ExecutionError("Data contains invalid numeric value: "+valueAsString);
                   }
               }
               values.add(tuple);
           }
           if (sort) {// sort all the series in case they were not already          
               for (String item:results.keySet()) {
                   Collections.sort(results.get(item), datasorter); // just in case the values are not in sorted order already 
               }          
           }
           dataset=results;      
       }
       
       private void parseDataColumns(ArrayList<String> data, String separator, boolean sort) throws ExecutionError {
           seriesNames=new ArrayList<String>();
           HashMap<String,ArrayList<double[]>> results=new HashMap<>();
           boolean headerIsParsed=false;
           for (String line:data) {
               if (line.isEmpty() || line.startsWith("#")) continue; // why not...
               String[] parts=line.split(separator);
               if (parts.length<2) throw new ExecutionError("Expected at least two columns in data");
               if (!headerIsParsed) { // this line should contain the series names
                   for (int i=1;i<parts.length;i++) { // ignore i==0
                       String seriesName=parts[i];                       
                       seriesNames.add(seriesName);
                       results.put(seriesName,new ArrayList<double[]>());
                   }   
                   headerIsParsed=true;
               } else { // regular data line. First column is X-value or category name, the rest are Y-values for different series
                   String xValueString=parts[0];
                   double xValue=0;
                   try {
                       xValue=Double.parseDouble(xValueString);
                   } catch (NumberFormatException e) { // The first column is not a numeric value. Treat it as a category value                      
                       xValue=getIndexForCategory(xValueString);
                       isCategorized=true;
                   } 
                   // process the rest of the y-values for different series
                   for (int i=1;i<parts.length;i++) {
                       String seriesName=seriesNames.get(i-1);
                       ArrayList<double[]> values=results.get(seriesName);
                       double yValue=0;
                       try {yValue=Double.parseDouble(parts[i]);} catch (NumberFormatException ne) {throw new ExecutionError("Data contains invalid numeric value: "+parts[i]);}
                       double[] tuple=new double[]{xValue,yValue};
                       values.add(tuple);
                   }
               }
           }
           if (sort) {// sort all the series in case they were not already          
               for (String item:results.keySet()) {
                   Collections.sort(results.get(item), datasorter); // just in case the values are not in sorted order already 
               }          
           }
           dataset=results;      
       }
       
       private void parseDataRows(ArrayList<String> data, String separator, boolean sort) throws ExecutionError {
           seriesNames=new ArrayList<String>();
           HashMap<String,ArrayList<double[]>> results=new HashMap<>();
           boolean headerIsParsed=false;
           for (String line:data) { // go through all the lines and find the series names (first column)
               if (line.isEmpty() || line.startsWith("#")) continue; // why not...
               if (!headerIsParsed) {headerIsParsed=true;continue;} // skip first line at this time
               String[] parts=line.split(separator);
               if (parts.length<2) throw new ExecutionError("Expected at least two columns in data"); 
               String seriesName=parts[0];                       
               seriesNames.add(seriesName);
               results.put(seriesName,new ArrayList<double[]>());               
           }
           headerIsParsed=false;
           // now start over again
           double[] xValues=null;
           for (String line:data) { // go through all the lines and find the series names (first column)
               if (line.isEmpty() || line.startsWith("#")) continue; // why not...
               String[] parts=line.split(separator);               
               if (!headerIsParsed) {
                   xValues=new double[parts.length-1];
                   for (int i=1;i<parts.length;i++) {
                       String xValueString=parts[i];
                       double xValue=0;
                       try {
                           xValue=Double.parseDouble(xValueString);
                       } catch (NumberFormatException e) { // The first column is not a numeric value. Treat it as a category value                      
                           xValue=getIndexForCategory(xValueString);
                           isCategorized=true;
                       }
                       xValues[i-1]=xValue;
                   }
                   headerIsParsed=true;
               } else { // regular data line. columns 1-N are y-values
                   String seriesName=parts[0];
                   ArrayList<double[]> values=results.get(seriesName);                   
                   for (int i=1;i<parts.length;i++) {
                       double yValue=0;
                       try {yValue=Double.parseDouble(parts[i]);} catch (NumberFormatException ne) {throw new ExecutionError("Data contains invalid numeric value: "+parts[i]);}
                       double[] tuple=new double[]{xValues[i-1],yValue};
                       values.add(tuple);
                   }                  
               }           
           }           
           if (sort) {// sort all the series in case they were not already          
               for (String item:results.keySet()) {
                   Collections.sort(results.get(item), datasorter); // just in case the values are not in sorted order already 
               }          
           }
           dataset=results;      
       }       
   
       private double getIndexForCategory(String category) {
           if (categories==null) categories=new ArrayList<String>();
           int index=categories.indexOf(category);
           if (index>=0) return index+1; // start at 1
           categories.add(category);
           return categories.size();
       }
       
       private HashMap<Double,String> getCategoryIndexMap() {
           HashMap<Double,String> map=new HashMap<Double,String>();
           if (categories!=null) {
               for (int i=0;i<categories.size();i++) {
                   String category=categories.get(i);
                   map.put(new Double(i+1.0),category); // start at 1
               }
           }
           return map;
       }
       
       public boolean isCategoryData() {
           return isCategorized;
       }
       
       public int getNumberOfCategories() {
           return (categories!=null)?categories.size():0;
       }
       
       /** Forces the X-values to be categories unless they already are so
        *  For this to work. The X-values should be positive integers starting at 1
        *  @return TRUE if the X-values could be turned into categories, else FALSE. 
        *  Note that the method does not check that each series has values for each category
        */
       public boolean forceCategories() {
           if (isCategorized) return true;
           
           double[] minmax=getMinAndMaxFromValues(0);
           if (minmax[0]!=1) return false;
           int min=1;
           if (minmax[1]!=Math.floor(minmax[1])) return false; // not an integer
           int max=(int)minmax[1];
                
           // check the datasets
           for (String series:seriesNames) {
               double[] xvalues=getTupleDataByIndex(series,0); // get all "x-values" (index=0) from the series         
               for (double value:xvalues) {
                    if (value!=Math.floor(value)) return false; // not an integer   
               }
           }
           // just make up category names
           categories=new ArrayList<>();
           for (int i=min;i<=max;i++) {
               categories.add(""+i); // just use the numbers as category labels
           }
           isCategorized=true;
           return true;
           
       }
       
       /** Returns the highest and lowest values among all the values in the dataset at the specified index location in each tuple */
       public double[] getMinAndMaxFromValues(int index) {
           double[] minmax=new double[]{Double.NaN,Double.NaN};
           for (ArrayList<double[]> values:dataset.values()) {
               for (double[] tuple:values) {
                   if (index>=tuple.length) continue;
                   if (Double.isNaN(minmax[0]) || tuple[index]<minmax[0]) minmax[0]=tuple[index];
                   if (Double.isNaN(minmax[1]) || tuple[index]>minmax[1]) minmax[1]=tuple[index];           
               }
           }
           return minmax;
       }   
       public double[] getMinAndMaxFromTuples(String series, int index) {
           ArrayList<double[]> values=dataset.get(series);
           if (values==null) return null;
           double[] minmax=new double[]{Double.NaN,Double.NaN};
           for (double[] tuple:values) {
               if (index>=tuple.length) continue;
               if (Double.isNaN(minmax[0]) || tuple[index]<minmax[0]) minmax[0]=tuple[index];
               if (Double.isNaN(minmax[1]) || tuple[index]>minmax[1]) minmax[1]=tuple[index];           
           }
           return minmax;
       }
       /** Returns smallest and largest among all values in the dataset*/
       public double[] getMinAndMaxFromValues() {
           double[] minmax=new double[]{Double.NaN,Double.NaN};
           for (ArrayList<double[]> values:dataset.values()) {
               for (double[] tuple:values) {
                   for (int i=0;i<tuple.length;i++) {
                       if (Double.isNaN(minmax[0]) || tuple[i]<minmax[0]) minmax[0]=tuple[i];
                       if (Double.isNaN(minmax[1]) || tuple[i]>minmax[1]) minmax[1]=tuple[i];     
                   }
               }
           }
           return minmax;
       }          
       public int getMinTupleSize() {
           int minLength=Integer.MAX_VALUE;
           for (ArrayList<double[]> values:dataset.values()) {
               for (double[] tuple:values) {
                   if (tuple.length<minLength) minLength=tuple.length;
               }
           }
           return minLength;
       }
       
       public int[] getMinMaxTupleSize() {
           int minLength=Integer.MAX_VALUE;
           int maxLength=Integer.MIN_VALUE;           
           for (ArrayList<double[]> values:dataset.values()) {
               for (double[] tuple:values) {
                   if (tuple.length<minLength) minLength=tuple.length;
                   if (tuple.length>maxLength) maxLength=tuple.length;                   
               }
           }
           return new int[]{minLength,maxLength};
       }       
       
       public double[] getTupleDataByIndex(String series, int index) {
            ArrayList<double[]> values=dataset.get(series);           
            double[] result=new double[values.size()];
            for (int i=0;i<values.size();i++) {
                result[i]=values.get(i)[index];
            }
            return result;
       }     
       
       public ArrayList<String> getSeriesNames() {
           return seriesNames;
       }
       
       public int getNumberofSeries() {
           return dataset.size();
       }
       
       public boolean isDataSeries(String name) {
           return (dataset!=null && dataset.containsKey(name));
       }
       
       public ArrayList<double[]> getAllSeriesValues(String series) {
           if (dataset==null) return null;
           else return dataset.get(series);
       }
       
       public ArrayList<Double> getAllSeriesValuesFlattened(String series) {
           ArrayList<Double> list=new ArrayList<>();
           if (dataset==null) return list;
           ArrayList<double[]> alldata=dataset.get(series);
           for (double[] tuple:alldata) {
              for (double value:tuple) {
                 list.add(value);                  
              }
           }
           return list;
       }       
   } // end of class dataset
   
   private class PlotSettings {
       private float strokewidth=1.8f;
       private BasicStroke DASHED_STROKE=new BasicStroke(strokewidth,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f,5f}, 0f);
       private BasicStroke DASHED_LONG_STROKE=new BasicStroke(strokewidth,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8f,8f}, 0f);
       private BasicStroke DOTTED_STROKE=new BasicStroke(strokewidth,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{1f,5f}, 0f);
       private BasicStroke LINE_STROKE=new BasicStroke(strokewidth);     
       private BasicStroke NO_STROKE=new BasicStroke(0);        
       
       HashMap<String,HashMap<String,Object>> settings=null;
       
       public PlotSettings() {
           settings=new HashMap<String,HashMap<String,Object>>();
       }
       
       public void parsePlotSettings(String settings, Dataset dataset) throws ExecutionError {
           if (settings==null || settings.trim().isEmpty()) return;
           String[] parts=settings.trim().split("\\s*;\\s*");
           for (String part:parts) {
               String[] pair=part.split("\\s*=\\s*");
               if (pair.length!=2) throw new ExecutionError("Plot settings should be on the form \"key=value\" separated by semicolons. Found: "+part);
               String item=pair[0].trim();
               String value=pair[1].trim();
               if (!dataset.isDataSeries(item)) {
                   // handle some special settings in a special way
                   if (item.equals("point size")) {
                       try {
                           int size=Integer.parseInt(value);
                           setGlobalSetting("point size", new Integer(size));
                       } catch (NumberFormatException e) {  }
                   } else setGlobalSetting(item, value);
               } else { // regular plot series setting                   
                   String series=item;
                   String[] props=value.split("\\s*,\\s*");
                   if (props.length>=1) {                  
                       Color color=Graph.getColorFromSetting(props[0],null);
                       if (color!=null) setSetting(series, "color", color);
                   }
                   if (props.length>=2) { // second should be line style
                       Stroke stroke=getLineStyleByName(props[1]);
                       if (stroke!=null) setSetting(series, "stroke", stroke);                       
                   }
                   if (props.length>=3) { // third should be point style
                       int pointstyle=getPointStyleByName(props[2]); 
                       if (pointstyle>=0) setSetting(series, "pointstyle", new Integer(pointstyle));
                   }                   
                           
               }
           } // end of parsing settings
           // Now go through each series and see if they have specifically assigned styles
           // if not, see if they have been defined with "graph.<property>.<series>" display settings
           for (String series:dataset.getSeriesNames()) {
               if (!hasSetting(series, "color")) {
                   Object color=engine.getClient().getVisualizationSettings().getSetting("graph.color."+series);
                   if (color instanceof Color) setSetting(series, "color", color);
               }
               if (!hasSetting(series, "stroke")) {
                   Object stroke=engine.getClient().getVisualizationSettings().getSetting("graph.linestyle."+series);
                   if (stroke instanceof String) {
                       BasicStroke lineStyle=getLineStyleByName((String)stroke);
                       if (lineStyle!=null) setSetting(series, "stroke", lineStyle);
                   }
               }   
               if (!hasSetting(series, "pointstyle")) {
                   Object pointstyle=engine.getClient().getVisualizationSettings().getSetting("graph.pointstyle."+series);
                   if (pointstyle instanceof Integer) {
                       if ((Integer)pointstyle>=0) setSetting(series, "pointstyle", pointstyle);
                   } else if (pointstyle instanceof String) {
                       int style=getPointStyleByName((String)pointstyle);
                       if (style>=0) setSetting(series, "pointstyle", style);
                   }
               }               
           }
       } 
       
       private BasicStroke getLineStyleByName(String name) {
           BasicStroke stroke=null;
           if (name.equalsIgnoreCase("dashed") || name.equalsIgnoreCase("dash")) stroke=DASHED_STROKE;
           else if (name.equalsIgnoreCase("longdashed") || name.equalsIgnoreCase("longdash")) stroke=DASHED_LONG_STROKE;
           else if (name.equalsIgnoreCase("dotted") || name.equalsIgnoreCase("dot")) stroke=DOTTED_STROKE;                     
           else if (name.equalsIgnoreCase("line") || name.equalsIgnoreCase("solid")) stroke=LINE_STROKE;                   
           else if (name.equalsIgnoreCase("none")) stroke=NO_STROKE;  
           return stroke;
       }
       
       private int getPointStyleByName(String name) {
           // 0=circle, 1=filled circle, 2=box, 3=filled box, 4=x-cross, 5= +cross, 6= *cross, 7=pixel (dot)         
           int pointstyle=-1;
           if (name.equalsIgnoreCase("circle")) pointstyle=0;
           else if (name.equalsIgnoreCase("filledcircle") || name.equalsIgnoreCase("fcircle")) pointstyle=1;
           else if (name.equalsIgnoreCase("box") || name.equalsIgnoreCase("square")) pointstyle=2;
           else if (name.equalsIgnoreCase("filledbox")  || name.equalsIgnoreCase("filledsquare") || name.equalsIgnoreCase("fbox") || name.equalsIgnoreCase("fsquare")) pointstyle=3;
           else if (name.equalsIgnoreCase("cross") || name.equalsIgnoreCase("x")) pointstyle=4;
           else if (name.equalsIgnoreCase("plus")) pointstyle=5;                       
           else if (name.equalsIgnoreCase("star")) pointstyle=6;    
           else if (name.equalsIgnoreCase("pixel") || name.equalsIgnoreCase("point") || name.equalsIgnoreCase("dot")) pointstyle=7;    
           return pointstyle;
       }
       
       public void setSetting(String series, String property, Object value) {
           if (!settings.containsKey(series)) settings.put(series, new HashMap<String,Object>());
           HashMap<String,Object> properties=settings.get(series);
           properties.put(property, value);
           //engine.logMessage("Plot setting ["+series+", "+property+"] => "+value);
       }        
       
       public Object getSetting(String series, String property) {
           HashMap<String,Object> properties=settings.get(series);
           if (properties==null) return null;
           return properties.get(property);
       }
       
       public boolean hasSetting(String series, String property) {
           HashMap<String,Object> properties=settings.get(series);
           if (properties==null) return false;
           return properties.containsKey(property);          
       }
       
       public Color getColorSetting(String series) {
           Object setting=getSetting(series, "color");
           if (setting instanceof Color) return (Color)setting;
           else { // assign new color and return it!
               return Color.BLACK;
           }
       }   
       
       public BasicStroke getStrokeSetting(String series) {
           Object setting=getSetting(series, "stroke");
           if (setting instanceof BasicStroke) return (BasicStroke)setting;
           else return LINE_STROKE;
       }     
       
       public int getPointStyleSetting(String series) {
           Object setting=getSetting(series, "pointstyle");
           if (setting instanceof Integer) return (Integer)setting;
           else return -1;
       }   
       
       public Object getGlobalSetting(String property) {
           Object setting=getSetting("_global_settings",property);
           return setting;
       }    
       public boolean hasGlobalSetting(String property) {
           HashMap<String,Object> properties=settings.get("_global_settings");
           if (properties==null) return false;
           return properties.containsKey(property);  
       }          
       
       public void setGlobalSetting(String property, Object value) {
           if (!settings.containsKey("_global_settings")) settings.put("_global_settings", new HashMap<String,Object>());
           HashMap<String,Object> properties=settings.get("_global_settings");
           properties.put(property, value);
           // engine.logMessage("Global setting: "+property+" => "+value);
       }    
       
       /** If the given property is defined and has the same class as the default value it is returned.
        *  If not, the default value is returned
        */
       public Object getGlobalSetting(String property, Object defaultValue) {
           Object setting=getSetting("_global_settings",property);
           if (setting==null) return defaultValue;
           if (defaultValue==null || setting.getClass()==defaultValue.getClass()) return setting;
           else {
               setting=getGlobalSettingAsClass(property, defaultValue.getClass());
               if (setting==null) return defaultValue;
               else return setting;
           }        
       }
       
       /**
        * Returns a given property as an object of a selected class (Boolean, Integer, Double or String)
        * if that is possible.
        * @param property
        * @param type
        * @return 
        */
       public Object getGlobalSettingAsClass(String property, Class type) {
           Object setting=getSetting("_global_settings",property);
           if (setting==null) return null;
           if (type==Boolean.class) {
               if (setting instanceof Boolean) return setting;
               else {
                   String boolstring=setting.toString().toLowerCase();
                   return (boolstring.equals("true") || boolstring.equals("yes"));
               }
           } else if (type==Integer.class) {
               if (setting instanceof Number) return ((Number)setting).intValue();
               else {
                   try {return Integer.parseInt(setting.toString());} catch (NumberFormatException e) {return null;}
               } 
           } else if (type==Double.class) {
               if (setting instanceof Number) return ((Number)setting).doubleValue();
               else {
                   try {return Double.parseDouble(setting.toString());} catch (NumberFormatException e) {return null;}
               }               
           } else if (type==String.class) {
               return setting.toString();
           } 
           else return null;          
       }  
       
       /** */
       public double getGlobalSettingAsNumber(String property) {
           boolean percentage=false;
           Object setting=getSetting("_global_settings",property);
           if (setting==null) return 0;    
           String string=setting.toString();
           if (string.endsWith("%")) {percentage=true;string=string.substring(0,string.length()-1);}
           double value=0;
           try {value=Double.parseDouble(string);} catch (NumberFormatException n) {}
           if (percentage) value=value/100.0;
           return value;
       }
       
       public boolean globalSettingEquals(String property, Object othervalue) {
           Object value=getGlobalSetting(property);
           if (value==null) return false;
           if (value.getClass()!=othervalue.getClass()) value=getGlobalSettingAsClass(property, othervalue.getClass());
           if (value==null) return false;
           if (value instanceof String && othervalue instanceof String) {
               return ((String)value).equalsIgnoreCase((String)othervalue);
           } else return value.equals(othervalue);
       }
       
       public boolean globalSettingStartsWith(String property, String prefix) {
           String value=(String)getGlobalSettingAsClass(property,String.class);
           if (value==null) return false;
           return value.startsWith(prefix);
       }       
       
       /**
        * Assuming the specified property is a string on the form:
        * N1:S1,N2:S2,N3:S3... Nn=Sn    where the Ns are numbers and
        * the Ss are strings, it will return a HashMap with these pairs
        */
       public HashMap<Double, String> getTicks(String property) {
           HashMap<Double, String> ticks=new HashMap<Double, String>();
           String ticksString=(String)getGlobalSettingAsClass(property,String.class);
           if (ticksString==null) return ticks;
           String[] parts=ticksString.split("\\s*,\\s*");
           for (String part:parts) {
               String[] pair=part.split("\\s*:\\s*");
               if (pair.length!=2) continue;
               try {
                   double value=Double.parseDouble(pair[0]);
                   ticks.put(value,pair[1]);
               } catch (NumberFormatException e) {}
           }
           return ticks;
       }
       
       /**
        * Assuming the specified property is a string containing a comma-separated
        * list of numeric values, this method will return those values as an array.
        * Values that are not numeric or are outside the allow range will be discarded
        * Alternatively, the string can be specified as 3 colon separated values,
        * where the first is the start, the second the step and the last the end.
        * The first value returned will then be "start" and these will be followed by
        * values incremented by "step" until the value is larger than end.
        */       
       public double[] getNumericValues(String property, double min, double max) {
            ArrayList<Double> list=new ArrayList<>();           
            String line=(String)getGlobalSettingAsClass(property,String.class);
            if (line.contains(":")) {
               String[] parts=line.split(":"); 
               if (parts.length!=3) return new double[0]; // ignore
               try {
                    double start=Double.parseDouble(parts[0]);
                    double step=Double.parseDouble(parts[1]);
                    double end=Double.parseDouble(parts[2]);
                    // ensure that start<=end and step is positive
                    if (end<start) {double swap=end; end=start; start=swap;} 
                    if (step<0) step=-step;
                    double value=start;
                    while (value<=end) {
                        if (value>=min && value<=max) list.add(value);
                        value+=step;
                    }
               } catch (NumberFormatException e) {}               
            } 
            else {
                String[] parts=line.split(",");
                for (String part:parts) {
                    try {
                        double value=Double.parseDouble(part);
                        if (value>=min && value<=max) list.add(value);
                    } catch (NumberFormatException e) {}
                }
            }
            double[] array=new double[list.size()];
            for (int i=0;i<array.length;i++) array[i]=list.get(i);     
            return array;
       }
   }
   
   static public Shape excludeClip(Graphics2D gc, Rectangle bb, int top, int left, int bottom, int right) {
       int ot=bb.y;
       int ol=bb.x; 
       int ob=(bb.y+bb.height);
       int or=(bb.x+bb.width );
       return new Polygon( new int[]{ ol, ol, or, or, ol, ol,   left, right, right, left, left },new int[]{ top, ot, ot, ob, ob, top,   top, top, bottom, bottom, top },11);
   }
}
