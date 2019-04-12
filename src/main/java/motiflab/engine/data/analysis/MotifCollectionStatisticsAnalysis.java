/*
 
 
 */

package motiflab.engine.data.analysis;

import motiflab.engine.data.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.Graph;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Parameter;
import motiflab.engine.dataformat.DataFormat;
import motiflab.gui.OutputPanel;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class MotifCollectionStatisticsAnalysis extends Analysis {
    private static String typedescription="Analysis: motif collection statistics";
    private static String analysisName="motif collection statistics";
    private final static String description="Calculates various statistics for the motifs in a collection";
    private String motifCollectionName=null;
    private int collectionsize=0;
    private HashMap<String,Statistic> statistics=null;

    private static final int DOUBLE=0;
    private static final int INTEGERS=1;
    private static final int PERCENTAGE=2;

    private static final String NONE="None";   
    private static final String MEDIAN_QUARTILES="Median+Quartiles"; 
    private static final String MEAN_STD="Average+StDev"; 
    private static final String BOTH="All";     
    
    private static final int PLOT_NONE=0;
    private static final int PLOT_MEDIAN_QUARTILES=1;
    private static final int PLOT_MEAN_STD=2;
    private static final int PLOT_BOTH=3;
    
    private final String[] variables = new String[]{};


    public MotifCollectionStatisticsAnalysis() {
        this.name="MotifCollectionStatisticsAnalysis_temp";
        addParameter("Motif Collection",MotifCollection.class, null,new Class[]{MotifCollection.class},"The motif collection to calculate statistics for",true,false);
     }

    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Motif Collection"};}    
    
    @Override
    public Parameter[] getOutputParameters() {        
         Parameter boxplotpar=new Parameter("Box plot",String.class, BOTH,new String[]{NONE,MEDIAN_QUARTILES,MEAN_STD,BOTH},"Which statistics to show using box plots",false,false);        
         Parameter scalepar=new Parameter("Graph scale",Integer.class,100,new Integer[]{10,2000},"Scale of graphics plot (in percent)",false,false);
         return new Parameter[]{boxplotpar,scalepar};
    }
    
    @Override
    public String[] getOutputParameterFilter(String parameter) {
        if (parameter.equals("Box plot") || parameter.equals("Graph scale")) return new String[]{"HTML"};
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
        throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else return NumericVariable.class; // all exported values in this analysis are numerical
    }

    @Override
    @SuppressWarnings("unchecked")
    public MotifCollectionStatisticsAnalysis clone() {
        MotifCollectionStatisticsAnalysis newanalysis=new MotifCollectionStatisticsAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.motifCollectionName=this.motifCollectionName;
        newanalysis.collectionsize=this.collectionsize;
        newanalysis.statistics=new HashMap<String, Statistic>();
        for (String sname:this.statistics.keySet()) {
            Statistic copy=this.statistics.get(sname).clone();
            newanalysis.statistics.put(sname, copy);
        }
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        MotifCollectionStatisticsAnalysis other=(MotifCollectionStatisticsAnalysis)source;
        this.motifCollectionName=other.motifCollectionName;
        this.collectionsize=other.collectionsize;
        this.statistics=new HashMap<String, Statistic>();
        for (String sname:other.statistics.keySet()) {
            Statistic copy=other.statistics.get(sname).clone();
            this.statistics.put(sname, copy);
        }
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
        if (settings!=null) {
          try {
                 Parameter[] defaults=getOutputParameters();
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
        engine.createHTMLheader("Motif Collection Statistics", null, null, false, true, true, outputobject);
        outputobject.append("<h1 class=\"headline\">Statistics for motifs in '"+motifCollectionName+"' ("+collectionsize+" motif"+((collectionsize==1)?"":"s")+")\n",HTML);
        outputobject.append("<br />\n<h2>Motif size (bp)</h2>\n",HTML);
        formatStatisticInHTML(statistics.get("length"), Color.red, plots, scale, outputobject, engine);
        if (format!=null) format.setProgress(33);
        outputobject.append("<br />\n<h2>IC-content</h2>\n",HTML);
        formatStatisticInHTML(statistics.get("IC"), Color.blue, plots, scale,  outputobject, engine);
        if (format!=null) format.setProgress(66);
        outputobject.append("<br />\n<h2>GC-content</h2>\n",HTML);
        formatStatisticInHTML(statistics.get("GC"), Color.green, plots, scale,  outputobject, engine);
        outputobject.append("</body>\n</html>\n",HTML);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }


    private void formatStatisticInHTML(Statistic statistic, Color color, int plots, double scale, OutputData outputobject,  MotifLabEngine engine) {
        DecimalFormat decimalFormat=new DecimalFormat("#.###");
        File imagefile=outputobject.createDependentFile(engine,"png");
        try {
            saveGraphAsImage(imagefile,statistic,color, plots, scale);
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        int newred=Math.min(color.getRed()+160, 255);
        int newgreen=Math.min(color.getGreen()+160, 255);
        int newblue=Math.min(color.getBlue()+160, 255);

        Color brightercolor=new Color(newred,newgreen,newblue);
        String colorstring=VisualizationSettings.convertColorToHTMLrepresentation(brightercolor);
        outputobject.append("<img src=\"file:///"+imagefile.getAbsolutePath()+"\" />\n",HTML);
        outputobject.append("<table>\n",HTML);
        outputobject.append("<tr><th width=\"90\" style=\"background-color:"+colorstring+"\">Min</th><th width=\"90\" style=\"background-color:"+colorstring+"\">Max</th><th width=\"90\" style=\"background-color:"+colorstring+"\">Median</th><th width=\"90\" style=\"background-color:"+colorstring+"\">1st Quartile</th><th width=\"90\" style=\"background-color:"+colorstring+"\">3rd Quartile</th><th width=\"90\" style=\"background-color:"+colorstring+"\">Average</th><th width=\"90\" style=\"background-color:"+colorstring+"\">Std. dev.</th></tr>\n",HTML);
        outputobject.append("<tr><td class=\"num\">"+decimalFormat.format(statistic.min)+"</td><td class=\"num\">"+decimalFormat.format(statistic.max)+"</td><td class=\"num\">"+decimalFormat.format(statistic.median)+"</td><td class=\"num\">"+decimalFormat.format(statistic.firstQuartile)+"</td><td class=\"num\">"+decimalFormat.format(statistic.thirdQuartile)+"</td><td class=\"num\">"+decimalFormat.format(statistic.average)+"</td><td class=\"num\">"+decimalFormat.format(statistic.std)+"</td></tr>\n",HTML);
        outputobject.append("</table>\n<br>\n",HTML);
        outputobject.append("<table>\n",HTML);
        outputobject.append("<tr><th style=\"text-align:left\"><nobr>Value range</nobr></th>",HTML);
        for (int i=0;i<statistic.bins.length;i++) {
           double lower=statistic.startbin+i*statistic.binrange;
           double upper=lower+statistic.binrange;
           if (statistic.valuestype==INTEGERS) outputobject.append("<th> "+(int)lower+" </th>",HTML);
           else if(statistic.valuestype == PERCENTAGE) outputobject.append("<th> " + (int)Math.round(lower*100) + "% </th>", HTML);
           else outputobject.append("<th> [" + decimalFormat.format(lower) + "-" + decimalFormat.format(upper) + ") </th>", HTML);
        }
        outputobject.append("</tr>\n<tr><td style=\"text-align:left\"><nobr>Motifs with this value</nobr></td>",HTML);
        for (int i=0;i<statistic.bins.length;i++) {
           outputobject.append("<td class=\"num\">"+statistic.bins[i]+"</td>",HTML);
        }
        outputobject.append("</tr>\n<tr><td style=\"text-align:left\"><nobr>This value or lower</nobr></td>",HTML);
        int sum=0;
        for (int i=0;i<statistic.bins.length;i++) {
           sum+=statistic.bins[i];
           outputobject.append("<td class=\"num\">"+sum+"</td>",HTML);
        }
        outputobject.append("</tr>\n<tr><td style=\"text-align:left\"><nobr>This value or higher</nobr></td>",HTML);
        sum=collectionsize;
        for (int i=0;i<statistic.bins.length;i++) {
           outputobject.append("<td class=\"num\">"+sum+"</td>",HTML);
           sum-=statistic.bins[i];
        }
        outputobject.append("</tr>\n</table>\n",HTML);
        outputobject.append("<br /><br /><br /><br />\n",HTML);
    }



    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        outputobject.append("#Statistics for '"+motifCollectionName+"'\n",RAWDATA);
        outputobject.append("#Number of motifs="+collectionsize,RAWDATA);
        outputobject.append("\n\n#Motif length\n",RAWDATA);
        formatStatisticInRawFormat(statistics.get("length"), outputobject);
        if (format!=null) format.setProgress(30);
        outputobject.append("\n\n#IC-content\n",RAWDATA);
        formatStatisticInRawFormat(statistics.get("IC"), outputobject);
        if (format!=null) format.setProgress(60);
        outputobject.append("\n\n#GC-content\n",RAWDATA);
        formatStatisticInRawFormat(statistics.get("GC"), outputobject);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }

    private void formatStatisticInRawFormat(Statistic statistic, OutputData outputobject) {
        outputobject.append("Minimum\t",RAWDATA);
        outputobject.append(""+statistic.min+"\n",RAWDATA);
        outputobject.append("Maximum\t",RAWDATA);
        outputobject.append(""+statistic.max+"\n",RAWDATA);
        outputobject.append("Median\t",RAWDATA);
        outputobject.append(""+statistic.median+"\n",RAWDATA);
        outputobject.append("1st Quartile\t",RAWDATA);
        outputobject.append(""+statistic.firstQuartile+"\n",RAWDATA);
        outputobject.append("3rd Quartile\t",RAWDATA);
        outputobject.append(""+statistic.thirdQuartile+"\n",RAWDATA);        
        outputobject.append("Average\t",RAWDATA);
        outputobject.append(""+statistic.average+"\n",RAWDATA);
        outputobject.append("Std. dev\t",RAWDATA);
        outputobject.append(""+statistic.std+"\n",RAWDATA);
        outputobject.append("#Histogram\n",RAWDATA);
        outputobject.append("Bin width\t",RAWDATA);
        outputobject.append(""+statistic.binrange+"\n",RAWDATA);
        outputobject.append("First bin starts at\t",RAWDATA);
        outputobject.append(""+statistic.startbin+"\n",RAWDATA);
        for (int i=0;i<statistic.bins.length;i++) {
           if (i>0) outputobject.append("\t",RAWDATA);
           outputobject.append(""+statistic.bins[i],RAWDATA);

        }
        outputobject.append("\n",RAWDATA);
    }

    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        MotifCollection motifs=(MotifCollection)task.getParameter("Motif Collection");
        this.motifCollectionName=motifs.getName();
        collectionsize=motifs.size();
        statistics=new HashMap<String, Statistic>(3);
        MotifLabEngine engine=task.getEngine();
        int stats=3;
        statistics.put("length",new Statistic("length", motifs, 1.0, 0.0, INTEGERS, engine));
        task.setStatusMessage("Executing analysis: "+getAnalysisName()+" (1/"+stats+")");
        task.setProgress(1, stats);
        statistics.put("IC",new Statistic("IC", motifs, 0.5 , 0.0, DOUBLE, engine));
        task.setStatusMessage("Executing analysis: "+getAnalysisName()+" (1/"+stats+")");
        task.setProgress(2, stats);
        statistics.put("GC",new Statistic("GC", motifs, 0.01, 0.0, 1.0, PERCENTAGE, engine));
        task.setStatusMessage("Executing analysis: "+getAnalysisName()+" (1/"+stats+")");
        task.setProgress(3, stats);
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
    }




   /**
    * Creates a histogram chart based on a Statistics oject and saves it to file (if file is given)
    * @param file A file to save the image to
    */
    private BufferedImage saveGraphAsImage(File file, Statistic statistic, Color color, int plots, double scale) throws IOException {
        int width=500;
        int height=(plots==PLOT_NONE)?250:300; // image height
        int graphheight=200; // height of graph in pixels (just the histogram);
        int numbins=statistic.bins.length;
        double binwidth=400/numbins; // width of histogram bins in pixels
        BufferedImage image=new BufferedImage((int)Math.round(width*scale),(int)Math.round(height*scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.scale(scale, scale);
        Stroke defaultStroke=g.getStroke();
        //BasicStroke dashed = new BasicStroke(1f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3f}, 0f);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        double maxgraphvalue=0;
        for (int i=0;i<numbins;i++) {
            if (statistic.bins[i]>maxgraphvalue) maxgraphvalue=statistic.bins[i];
        }
        int translateX=50; // the X coordinate for the top of the graph
        int translateY=(plots==PLOT_NONE)?5:30; // the Y coordinate for the top of the graph
        double globalmin=statistic.startbin; // the smallest value in the numericdataset
        double globalmax=statistic.startbin+(statistic.bins.length*statistic.binrange); // the largest value in the numericdataset
        int graphwidth=(int)(binwidth*numbins);
        double tickscaleY=0;
        if (maxgraphvalue<=10) {tickscaleY=1f;}
        else if (maxgraphvalue<=50) {tickscaleY=5f;}
        else if (maxgraphvalue<=100) {tickscaleY=10f;}
        else if (maxgraphvalue<=200) {tickscaleY=20f;}
        else if (maxgraphvalue<=500) {tickscaleY=50f;}
        else if (maxgraphvalue<=1000) {tickscaleY=100f;}
        else {tickscaleY=100f;}
        maxgraphvalue=(((int)(maxgraphvalue/tickscaleY))+1)*tickscaleY; // ceil to nearest X
        Graph graph=new Graph(g, globalmin, globalmax, 0f, maxgraphvalue, graphwidth, graphheight, translateX, translateY);
        g.setColor(color);
        double[] histogram=new double[statistic.bins.length];
        for (int i=0;i<histogram.length;i++) histogram[i]=(double)statistic.bins[i];
        graph.drawHistogram(histogram);
        g.setColor(Color.BLACK);
        //g.drawRect(translateX, translateY, graphwidth, graphheight); // draw bounding box
        graph.drawXaxisWithTicks(graphheight+translateY, false, false);
        graph.drawYaxisWithTicks(translateX, false, false);
        int averageinsideXcoordinate=graph.getXforValue(statistic.average);
        int medianinsideXcoordinate=graph.getXforValue(statistic.median);
        g.setColor(color);
        if (plots==PLOT_MEDIAN_QUARTILES || plots==PLOT_BOTH) graph.drawHorizontalBoxAndWhiskers(statistic.min, statistic.max, statistic.median, statistic.firstQuartile, statistic.thirdQuartile, 0,7);
        if (plots==PLOT_MEAN_STD || plots==PLOT_BOTH) graph.drawHorizontalMeanAndStdDeviation(statistic.average, statistic.std, 0,7);        
        if (plots==PLOT_BOTH || plots==PLOT_MEAN_STD) {
            g.setStroke(Graph.DASHED_LONG_STROKE);
            g.drawLine(averageinsideXcoordinate, 15, averageinsideXcoordinate, graphheight+translateY+6);
            g.setStroke(defaultStroke);
        }
        if (plots==PLOT_BOTH || plots==PLOT_MEDIAN_QUARTILES) g.drawLine(medianinsideXcoordinate, 0, medianinsideXcoordinate, graphheight+translateY+6);
        // write the image to file
        if (file!=null) {
            OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
            ImageIO.write(image, "png", output);
            output.close(); 
        }
        g.dispose();
        return image;
    }







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
        panel.setPreferredSize(new Dimension(800,540));
        return panel;

    }


    private class Statistic implements Serializable {
        double min=0;
        double max=0;
        double average=0;      
        double std=0;
        double median=0;  
        double firstQuartile=0;  
        double thirdQuartile=0;          
        int[] bins=null; // histogram
        double binrange=1; // range of values within a bin. Usually 1.0 (e.g. one bin goes from 2.0 to 3.0- )
        double startbin=0; // the value the first bin should start at
        String statisticname=null;
        int valuestype=DOUBLE;

        public Statistic(String statisticname, MotifCollection motifs, double binrange, int valuesType, MotifLabEngine engine) {
            this.statisticname=statisticname;
            this.binrange=binrange;
            this.valuestype=valuesType;
            double[] values=getValues(statisticname, motifs, engine);
            calculateStatistics(values);
            makeHistogram(values, min-(min%binrange), max); // min, max are calculated on the line above
        }

        public Statistic(String statisticname, MotifCollection motifs, double binrange, double rangeMin, double rangeMax, int valuesType,  MotifLabEngine engine) {
            this.statisticname=statisticname;
            this.binrange=binrange;
            this.valuestype=valuesType;
            double[] values=getValues(statisticname, motifs, engine);
            calculateStatistics(values);
            makeHistogram(values, rangeMin, rangeMax);
        }

        public Statistic(String statisticname, MotifCollection motifs, double binrange, double rangeMin, int valuesType,  MotifLabEngine engine) {
            this.statisticname=statisticname;
            this.binrange=binrange;
            this.valuestype=valuesType;
            double[] values=getValues(statisticname, motifs, engine);
            calculateStatistics(values);
            makeHistogram(values, rangeMin, max);
        }

        public Statistic() {

        }

        /** Return values of a given statistic for all motifs */
        private double[] getValues(String stat, MotifCollection motifs, MotifLabEngine engine) {
            double[] values=new double[motifs.size()];
            int i=0;
            for (Motif motif:motifs.getAllMotifs(engine)) {
                      if (stat.equalsIgnoreCase("size") || stat.equalsIgnoreCase("length")) values[i]=(double)motif.getLength();
                else if (stat.equalsIgnoreCase("GC")) values[i]=(double)motif.getGCcontent();
                else if (stat.equalsIgnoreCase("IC")) values[i]=(double)motif.getICcontent();
                i++;
            }
            return values;
        }

        /** Calculates basic statistics from a list of values and sets the class fields */
        private void calculateStatistics(double[] values) {
            if (values==null || values.length==0) return;
            Arrays.sort(values);
            min=values[0];
            max=values[values.length-1];
            double sum=0;
            for (int i=0;i<values.length;i++) sum+=values[i];
            average=sum/(double)values.length;
            median=MotifLabEngine.getMedianValue(values);
            firstQuartile=MotifLabEngine.getFirstQuartileValue(values);
            thirdQuartile=MotifLabEngine.getThirdQuartileValue(values);
            double deviationSum=0;
            for (int i=0;i<values.length;i++) {
               deviationSum+=(values[i]-average)*(values[i]-average);
            }
            std=(double)Math.sqrt(deviationSum/(double)values.length);
        }

        private void makeHistogram(double[] values, double rangeMin, double rangeMax) {
            startbin=rangeMin;
            int numbins=(int)((rangeMax-startbin)/binrange)+1;
            bins=new int[numbins];
            for (int i=0;i<values.length;i++) {
                int bin=(int)((values[i]-startbin)/binrange);
                bins[bin]++;
            }
        }


        @Override
        @SuppressWarnings("unchecked")
        public Statistic clone() {
            Statistic newstat=new Statistic();
            newstat.statisticname=this.statisticname;
            newstat.min=this.min;
            newstat.max=this.max;
            newstat.average=this.average;
            newstat.median=this.median;
            newstat.std=this.std;
            newstat.firstQuartile=this.firstQuartile;
            newstat.thirdQuartile=this.thirdQuartile;            
            newstat.startbin=this.startbin;
            newstat.binrange=this.binrange;
            newstat.bins=(this.bins==null)?null:this.bins.clone();
            newstat.valuestype=this.valuestype;
            return newstat;
        }

        public void importData(Statistic other) {
            this.statisticname=other.statisticname;
            this.min=other.min;
            this.max=other.max;
            this.average=other.average;
            this.median=other.median;
            this.std=other.std;
            this.firstQuartile=other.firstQuartile;
            this.thirdQuartile=other.thirdQuartile;            
            this.startbin=other.startbin;
            this.binrange=other.binrange;
            this.valuestype=other.valuestype;
            this.bins=(other.bins==null)?null:other.bins.clone();
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
