/*
 
 
 */

package org.motiflab.engine.data.analysis;

import org.motiflab.engine.Graph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.TaskRunner;
import static org.motiflab.engine.data.analysis.Analysis.EXCEL;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.gui.VisualizationSettings;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.MarkerStyle;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;

/**
 *
 * @author kjetikl
 */
public class ROCAnalysis extends Analysis {
    private static String typedescription="Analysis: ROC analysis";
    private static String analysisName="ROC analysis";
    private final static String description="Evaluates the ability of a particular (numeric) region property to discriminate between regions classified as either positive or negative";
    private String regionDatasetName=null;
    private String scoreProperty="score";
    private String classProperty="";
    private String positiveClassValue="true";
    //private String negativeClassValue="false";
    private String featureProperty="type";
    private String sequenceCollectionName=null;
    private int sequenceCollectionSize=0;
    private double[] ROCvalues; // stores the values for ROC at different levels (from 0 to 100 inclusive (i.e. 101 values))
    private double[] PrecisionRecallvalues; // stores the values for the precision-recall graph at different levels (from 0 to 100 inclusive (i.e. 101 values))
    private long[][] TPFPTNFN; // counts for TP, FP, TN and FN at different levels (from 0 to 100 inclusive (i.e. 101 values))
    private double minScoreValue=0;  // this is used to keep track of the range of 'score' property values in "specific mode"
    private double maxScoreValue=0;  // this is used to keep track of the range of 'score' property values in "specific mode"
    private double bestSnSpsum=0; // the best sum of Sn and Sp which is possible to achieve
    private double bestAccuracy=0; // the best accuracy possible to achieve
    private double[] positiveBins=null; // used for the histograms
    private double[] negativeBins=null; // used for the histograms
    private HashMap<String,Long> positiveCount=null; // number of positive regions (by group)
    private HashMap<String,Long> negativeCount=null; // number of positive regions (by group)
    private double AUC=0; // Area Under the Curve of the best feature
    private HashMap<String,Double> AUCmap=null;
    private HashMap<String,double[]> ROCmap=null;
    private boolean aboveOrEqual=true;
    private static final int resolution=100; // number if threshold samples to use when drawing statistics graphs

    private ArrayList<double[]> optimalThresholdBestSnSpsum=null; // each double[] contains min and max in threshold range
    private ArrayList<double[]> optimalThresholdBestAccuracy=null; // (or lower and upper threshold bound)

    private final String[] variables = new String[]{"AUC","bestSnSpSum","bestAccuracy","bestSnSpSum-threshold(min)","bestSnSpSum-threshold(max)","bestAccuracy-threshold(min)","bestAccuracy-threshold(max)"};  // Returns best AUC score if multiple tracks are evaluated


    public ROCAnalysis() {
        this.name="EvaluatePriorsAnalysis_temp";
        addParameter("Target track",RegionDataset.class, null,new Class[]{RegionDataset.class},"A track containing target regions with properties upon which to base the ROC curves",true,false);
        addParameter("Score property",String.class, "score",null,"The property that will be used to sort the regions in descending order",false,false);
        addParameter("Feature property",String.class, "",null,"If defined, regions will be devided into groups based on the value of this property and a ROC curve will be derived for each group",false,false);
        addParameter("Class property",String.class, "",null,"A boolean or text property that classifies the region as belonging to either the positive or negative class",true,false);
        addParameter("Positive class",String.class, "true",null,"If the 'Class property' is NOT a boolean property, a region will be regarded as a positive instance if the value of the Class property equals the value entered here",true,false);
        addParameter("Sequences",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"If specified, the analysis will be limited to the sequences in this collection",false,false);
        addParameter("Threshold",String.class, "Above or equal",new String[]{"Above or equal","Strictly above"},"Decides whether to classify all positions with a score 'above or equal' to a threshold as positive instances or just those with scores 'strictly above' the threshold",false,false);
    }

    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Target track","Priors track"};} 
    

    @Override
    public Parameter[] getOutputParameters(String dataformat) {
        if (dataformat.equals(HTML)) return new Parameter[] {
              new Parameter("Graph scale",Integer.class,100,new Integer[]{10,2000},"Scale of graphics plot (in percent)",false,false),
              new Parameter("Include legend",String.class,"",null,"<html>This argument can be used to include a legend box as part of the figure itself.<br>If used, this argument should be either a single positive value or three values separated by comma.<br>The first value will be the %-scale the legend box should be displayed at.<br>The optional second and third values are interpreted as a coordinate at which to display the legend box.<br>Positive coordinates are offset down and to the right relative to an origin in the upper left corner.<br>Negative values are offset left and upwards relative to an origin in the lower right corner.</html>",false,false)
        };
        else return new Parameter[0];
    }

//    @Override
//    public String[] getOutputParameterFilter(String parameter) {
//        if (parameter.equals("Graph scale") || parameter.equals("Include legend")) return new String[]{"HTML"};
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
        if (AUCmap!=null && !AUCmap.isEmpty()) {
            String[] dynamic=new String[variables.length+AUCmap.size()];
            System.arraycopy(variables, 0, dynamic, 0, variables.length);
            int i=variables.length;
            for (String key:AUCmap.keySet()) {
               dynamic[i]=key;
               i++;
            }
            return dynamic;
        }
        else return variables;
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename.equals("AUC")) return new NumericVariable("result", AUC); // Returns best AUC score is multiple tracks are evaluates
        else if (variablename.equals("bestSnSpSum")) {return new NumericVariable("result", bestSnSpsum);} //
        else if (variablename.equals("bestAccuracy")) return new NumericVariable("result", bestAccuracy); //
        else if (variablename.equals("bestSnSpSum-threshold(min)")) {
            double result=0;
            if (optimalThresholdBestSnSpsum!=null && optimalThresholdBestSnSpsum.size()==1) {
                result=optimalThresholdBestSnSpsum.get(0)[0];// smallest number
            } else if (optimalThresholdBestSnSpsum!=null && optimalThresholdBestSnSpsum.size()>1){
                result=optimalThresholdBestSnSpsum.get(0)[0]; // initialize
                for (double[]range:optimalThresholdBestSnSpsum) {if (range[0]<result)result=range[0];}
            }            
            return new NumericVariable("result", result);
        } 
        else if (variablename.equals("bestSnSpSum-threshold(max)")) {
            double result=0;
            if (optimalThresholdBestSnSpsum!=null && optimalThresholdBestSnSpsum.size()==1) {
                result=optimalThresholdBestSnSpsum.get(0)[1];// largest number
            } else if (optimalThresholdBestSnSpsum!=null && optimalThresholdBestSnSpsum.size()>1){
                result=optimalThresholdBestSnSpsum.get(0)[1]; // initialize
                for (double[]range:optimalThresholdBestSnSpsum) {if (range[1]>result)result=range[1];}
            }            
            return new NumericVariable("result", result);
        } 
        else if (variablename.equals("bestAccuracy-threshold(min)")) {
            double result=0;
            if (optimalThresholdBestAccuracy!=null && optimalThresholdBestAccuracy.size()==1) {
                result=optimalThresholdBestAccuracy.get(0)[0];// smallest number
            } else if (optimalThresholdBestAccuracy!=null && optimalThresholdBestAccuracy.size()>1){
                result=optimalThresholdBestAccuracy.get(0)[0]; // initialize
                for (double[]range:optimalThresholdBestAccuracy) {if (range[0]<result)result=range[0];}
            }            
            return new NumericVariable("result", result);
        } 
        else if (variablename.equals("bestAccuracy-threshold(max)")) {
            double result=0;
            if (optimalThresholdBestAccuracy!=null && optimalThresholdBestAccuracy.size()==1) {
                result=optimalThresholdBestAccuracy.get(0)[1];// largest number
            } else if (optimalThresholdBestAccuracy!=null && optimalThresholdBestAccuracy.size()>1){
                result=optimalThresholdBestAccuracy.get(0)[1]; // initialize
                for (double[]range:optimalThresholdBestAccuracy) {if (range[1]>result)result=range[1];}
            }            
            return new NumericVariable("result", result);
        }         
        else if (AUCmap!=null && AUCmap.containsKey(variablename)) return new NumericVariable("result", AUCmap.get(variablename).doubleValue()); //
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else return NumericVariable.class; // all exported values in this analysis are numerical
    }

    @Override
    @SuppressWarnings("unchecked")
    public ROCAnalysis clone() {
        ROCAnalysis newanalysis=new ROCAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.regionDatasetName=this.regionDatasetName;
        newanalysis.scoreProperty=this.scoreProperty;
        newanalysis.classProperty=this.classProperty;
        newanalysis.positiveClassValue=this.positiveClassValue;
        //newanalysis.negativeClassValue=this.negativeClassValue;
        newanalysis.featureProperty=this.featureProperty;
        newanalysis.sequenceCollectionName=this.sequenceCollectionName;
        newanalysis.sequenceCollectionSize=this.sequenceCollectionSize;
        newanalysis.minScoreValue=this.minScoreValue;
        newanalysis.maxScoreValue=this.maxScoreValue;
        newanalysis.bestSnSpsum=this.bestSnSpsum;
        newanalysis.bestAccuracy=this.bestAccuracy;
        newanalysis.AUC=this.AUC;
        newanalysis.aboveOrEqual=this.aboveOrEqual;
        newanalysis.positiveBins=(this.positiveBins!=null)?this.positiveBins.clone():null;
        newanalysis.negativeBins=(this.negativeBins!=null)?this.negativeBins.clone():null;
        newanalysis.ROCvalues=(this.ROCvalues!=null)?this.ROCvalues.clone():null;
        newanalysis.PrecisionRecallvalues=(this.PrecisionRecallvalues!=null)?this.PrecisionRecallvalues.clone():null;
        newanalysis.TPFPTNFN=(this.TPFPTNFN!=null)?this.TPFPTNFN.clone():null;
        if (ROCmap!=null) {
            newanalysis.ROCmap=new HashMap<String, double[]>(this.ROCmap.size());
            for (String key:ROCmap.keySet()) {
                double[] values=this.ROCmap.get(key);
                newanalysis.ROCmap.put(key,Arrays.copyOf(values, values.length));
            }
        }
        if (AUCmap!=null) {
            newanalysis.AUCmap=new HashMap<String, Double>(this.AUCmap.size());
            for (String key:AUCmap.keySet()) {newanalysis.AUCmap.put(key,new Double(this.AUCmap.get(key)));}
        }
        if (positiveCount!=null) {
            newanalysis.positiveCount=new HashMap<String, Long>(this.positiveCount.size());
            for (String key:positiveCount.keySet()) {newanalysis.positiveCount.put(key,new Long(this.positiveCount.get(key)));}
        }
        if (negativeCount!=null) {
            newanalysis.negativeCount=new HashMap<String, Long>(this.negativeCount.size());
            for (String key:negativeCount.keySet()) {newanalysis.negativeCount.put(key,new Long(this.negativeCount.get(key)));}
        }      
        if (optimalThresholdBestSnSpsum!=null) {
            newanalysis.optimalThresholdBestSnSpsum=new ArrayList<double[]>();
            for (double[] val:this.optimalThresholdBestSnSpsum) {newanalysis.optimalThresholdBestSnSpsum.add(new double[]{val[0],val[1]});}
        }
        if (optimalThresholdBestAccuracy!=null) {
            newanalysis.optimalThresholdBestAccuracy=new ArrayList<double[]>();
            for (double[] val:this.optimalThresholdBestAccuracy) {newanalysis.optimalThresholdBestAccuracy.add(new double[]{val[0],val[1]});}
        }
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        ROCAnalysis other=(ROCAnalysis)source;
        this.regionDatasetName=other.regionDatasetName;
        this.scoreProperty=other.scoreProperty;
        this.classProperty=other.classProperty;
        this.positiveClassValue=other.positiveClassValue;
        //this.negativeClassValue=other.negativeClassValue;
        this.featureProperty=other.featureProperty;
        this.sequenceCollectionName=other.sequenceCollectionName;
        this.sequenceCollectionSize=other.sequenceCollectionSize;
        this.positiveCount=other.positiveCount;
        this.negativeCount=other.negativeCount;
        this.minScoreValue=other.minScoreValue;
        this.maxScoreValue=other.maxScoreValue;
        this.ROCvalues=other.ROCvalues;
        this.PrecisionRecallvalues=other.PrecisionRecallvalues;
        this.TPFPTNFN=other.TPFPTNFN;
        this.bestSnSpsum=other.bestSnSpsum;
        this.bestAccuracy=other.bestAccuracy;
        this.AUC=other.AUC;
        this.aboveOrEqual=other.aboveOrEqual;
        this.optimalThresholdBestSnSpsum=other.optimalThresholdBestSnSpsum;
        this.optimalThresholdBestAccuracy=other.optimalThresholdBestAccuracy;
        this.AUCmap=other.AUCmap;
        this.ROCmap=other.ROCmap;
        this.positiveBins=other.positiveBins; 
        this.negativeBins=other.negativeBins;
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
        String legendScaleString="";
        double[] includeLegend=null;             
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             scalepercent=(Integer)settings.getResolvedParameter("Graph scale",defaults,engine);
             legendScaleString=(String)settings.getResolvedParameter("Include legend",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        if (legendScaleString==null) legendScaleString=""; else legendScaleString=legendScaleString.trim();
        if (!legendScaleString.isEmpty()) {
            String[] parts=legendScaleString.split("\\s*,\\s*");
            if (!(parts.length==1 || parts.length==3)) throw new ExecutionError("The \"Include legend\" parameter should consist of either 1 single value or 3 comma-separated values");
            includeLegend=new double[3];
            for (int i=0;i<parts.length;i++) {
                try {
                    includeLegend[i]=Double.parseDouble(parts[i]);
                } catch (Exception e) {throw new ExecutionError("Unable to parse expected numerical value for \"Include legend\" parameter: "+parts[i]);}
            }
            includeLegend[0]=includeLegend[0]/100.0; // this was a percentage number
        }
        double scale=(scalepercent==100)?1.0:(((double)scalepercent)/100.0);     
        if (ROCmap==null) return formatHTMLsingleFeature(outputobject, engine, settings, task, format, scale);
        else return formatHTMLmultipleFeatures(outputobject, engine, settings, task, format, scale, includeLegend);
    }



    private OutputData formatHTMLsingleFeature(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format, double scale) throws ExecutionError, InterruptedException {
        DecimalFormat decimalformatter=new DecimalFormat("0.00000");
        File rocImageFile=outputobject.createDependentFile(engine,"png");
        try {
            saveROCGraphAsImage(rocImageFile, scale);
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        File precisionRecallImageFile=outputobject.createDependentFile(engine,"png");
        try {
            savePrecisionRecallGraphAsImage(precisionRecallImageFile, scale);
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        File sensitivityImageFile=outputobject.createDependentFile(engine,"png");
        try {
            saveSensitivityGraphAsImage(sensitivityImageFile, scale);
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        File normalizedHistogramImageFile=outputobject.createDependentFile(engine,"png");
        try {
            saveHistogramGraphAsImage(normalizedHistogramImageFile, true, true, false, scale);
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        File histogramImageFile=outputobject.createDependentFile(engine,"png");
        try {
            saveHistogramGraphAsImage(histogramImageFile, false, false, true, scale);
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        File otherStatsImageFile=outputobject.createDependentFile(engine,"png");
        try {
            saveOtherStatsGraphAsImage(otherStatsImageFile, scale);
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        long truePositive=positiveCount.get(null);
        long trueNegative=negativeCount.get(null);
        engine.createHTMLheader("ROC Analysis", null, null, false, true, true, outputobject);
        outputobject.append("<div align=\"center\">\n",HTML);
        outputobject.append("<h2 class=\"headline\">Evaluation of property \""+scoreProperty+"\" for discriminating between positive and negative regions in \""+regionDatasetName+"\"</h2>\n",HTML);
        outputobject.append("Analysis based on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),HTML);
        if (sequenceCollectionName!=null) outputobject.append(" from collection <span class=\"dataitem\">"+sequenceCollectionName+"</span>\n",HTML);
        outputobject.append("<table style=\"border-width: 0px;\">\n",HTML);
        outputobject.append("<tr><td valign=\"top\" style=\"border-width: 0px;\">\n",HTML);
        outputobject.append("<table style=\"border-width: 0px;\"></tr>\n",HTML); // nested table
        outputobject.append("<tr>",HTML);
        outputobject.append("<td style=\"border-width: 0px;\" valign=\"top\" align=\"center\"><img src=\"file:///"+rocImageFile.getAbsolutePath()+"\" /></td>\n",HTML);
        outputobject.append("<td style=\"border-width: 0px;\" valign=\"top\" align=\"center\"><img src=\"file:///"+precisionRecallImageFile.getAbsolutePath()+"\" /></td>\n",HTML);
        outputobject.append("</tr>\n</table>\n",HTML);
        outputobject.append("These graphs summarize the ability of the region property \"<span class=\"dataitem\">"+scoreProperty+"</span>\" to discriminate between <font color=\"#FF0000\">"+truePositive+" positive</font> and <font color=\"#0000FF\">"+trueNegative+" negative</font> regions in <span class=\"dataitem\">"+regionDatasetName+"</span>.\n",HTML);
        outputobject.append("True positive regions are those whose value of the \"<span class=\"dataitem\">"+classProperty+"</span>\" property equals \""+positiveClassValue+"\". All other regions are considered to be true negatives.<br><br>",HTML);
        outputobject.append("The evaluation assumes that all regions with a value of <span class=\"dataitem\">"+scoreProperty+"</span> ",HTML);
        if (aboveOrEqual) outputobject.append("greater than or equal to",HTML);
        else outputobject.append("strictly greater than",HTML);
        outputobject.append(" some varying threshold are <i>predicted</i> as positive, while regions scoring ",HTML);
        if (aboveOrEqual) outputobject.append("below",HTML);
        else outputobject.append("equal to or below",HTML);        
        outputobject.append(" the threshold are <i>predicted</i> as negative.<br><br>",HTML);
        outputobject.append("The performance of this property, as evaluated by the <i>area under the curve</i> (AUC) of the ROC-graph above, is "+decimalformatter.format(AUC)+", which is "+getEvaluationOfAUC(AUC)+".<br><br>",HTML);
        outputobject.append("The graphs on the right show how different performance statistics vary according to different threshold levels.<br>\n",HTML);
        if (optimalThresholdBestSnSpsum.size()==1) {
            double[] range=optimalThresholdBestSnSpsum.get(0);
            outputobject.append("<br>The best trade-off between sensitivity and specificity is obtained with a ",HTML);
            if (range[0]==range[1]) outputobject.append("threshold="+decimalformatter.format(range[0])+". This threshold is marked with a green line.",HTML);
            else outputobject.append("threshold in the range ["+decimalformatter.format(range[0])+","+decimalformatter.format(range[1])+"]. This range is marked with a green rectangle.",HTML);
        } else {
           outputobject.append("<br>The distribution of <span class=\"dataitem\">"+scoreProperty+"</span> values allows for multiple different thresholds which can achieve optimal trade-off between sensitivity and specificity:<br><br>",HTML);
           for (double[] range:optimalThresholdBestSnSpsum) {
               if (range[0]==range[1]) outputobject.append("&nbsp;&nbsp;&nbsp;&nbsp;&bull;&nbsp;&nbsp;"+decimalformatter.format(range[0])+"<br>",HTML);
               else outputobject.append("&nbsp;&nbsp;&nbsp;&nbsp;&bull;&nbsp;&nbsp;"+decimalformatter.format(range[0])+" - "+decimalformatter.format(range[1])+"<br>",HTML);
           }
           outputobject.append("<br>The thresholds are marked with vertical green lines (or rectangles) in the graphs on the right.",HTML);
        }
        if (optimalThresholdBestAccuracy.size()==1) {
            double[] range=optimalThresholdBestAccuracy.get(0);
            outputobject.append("<br><br>The highest obtainable <i>accuracy</i> (% of all correctly predicted regions) is "+decimalformatter.format(bestAccuracy)+" with a ",HTML);
            if (range[0]==range[1]) outputobject.append("threshold="+decimalformatter.format(range[0])+". This threshold is marked with a gray line.",HTML);
            else outputobject.append("threshold in the range ["+decimalformatter.format(range[0])+","+decimalformatter.format(range[1])+"]. This range is marked with a gray rectangle.",HTML);
        } else {
           outputobject.append("<br><br>The highest obtainable <i>accuracy</i> (% of all correctly predicted region s) is "+decimalformatter.format(bestAccuracy)+".",HTML);
           outputobject.append("The distribution of  \""+scoreProperty+"\" values allows for multiple different thresholds which can achieve optimal accuracy:<br><br>",HTML);
           for (double[] range:optimalThresholdBestAccuracy) {
               if (range[0]==range[1]) outputobject.append("&nbsp;&nbsp;&nbsp;&nbsp;&bull;&nbsp;&nbsp;"+decimalformatter.format(range[0])+"<br>",HTML);
               else outputobject.append("&nbsp;&nbsp;&nbsp;&nbsp;&bull;&nbsp;&nbsp;"+decimalformatter.format(range[0])+" - "+decimalformatter.format(range[1])+"<br>",HTML);
           }
           outputobject.append("<br>The thresholds are marked with vertical gray lines (or rectangles) in the graphs on the right.",HTML);
        }
//        outputobject.append("<br><br>Note that the optimal accuracy-threshold can differ from the threshold for best sensitivity/specificity if the ratio between the number of positive versus negative regions is skewed. In this dataset the positive:negative ratio is ",HTML);
//        if (inside<outside) outputobject.append("1:"+decimalformatter.format((double)outside/(double)inside)+".",HTML);
//        else outputobject.append(decimalformatter.format((double)inside/(double)outside)+":1.",HTML);
        outputobject.append("<br>",HTML);
        outputobject.append("</td>",HTML);
        outputobject.append("<td style=\"border-width: 0px;\" valign=\"top\" align=\"center\">",HTML);
        outputobject.append("<img src=\"file:///"+sensitivityImageFile.getAbsolutePath()+"\" /><br>",HTML);
        outputobject.append("<font color=\"#00FF00\">Sensitivity</font>&nbsp;&nbsp;&nbsp;",HTML);
        outputobject.append("<font color=\"#FF0000\">Specificity</font>&nbsp;&nbsp;&nbsp;",HTML);
        outputobject.append("<font color=\"#E0E000\">False Positive Rate (1-Sp)</font><br>",HTML);
        outputobject.append("<font color=\"#0000FF\">Average Sn+Sp</font>&nbsp;&nbsp;&nbsp;",HTML);
        outputobject.append("<font color=\"#A0A0A0\">Distance between Sn and FPR</font>&nbsp;&nbsp;&nbsp;",HTML);
        outputobject.append("<font color=\"#FF00FF\">F-measure</font><br><br>",HTML);
        outputobject.append("<img src=\"file:///"+normalizedHistogramImageFile.getAbsolutePath()+"\" /><br>",HTML);
        outputobject.append("Normalized distributions of \""+scoreProperty+"\" values for ",HTML);
        outputobject.append("<font color=\"#FF0000\">positive</font> versus ",HTML);
        outputobject.append("<font color=\"#0000FF\">negative</font> regions<br><br>",HTML);
        outputobject.append("<img src=\"file:///"+histogramImageFile.getAbsolutePath()+"\" /><br>",HTML);
        outputobject.append("Raw distributions of \""+scoreProperty+"\" values for ",HTML);
        outputobject.append("<font color=\"#FF0000\">positive</font> versus ",HTML);
        outputobject.append("<font color=\"#0000FF\">negative</font> regions<br><br>",HTML);
        outputobject.append("<img src=\"file:///"+otherStatsImageFile.getAbsolutePath()+"\" /><br>",HTML);
        outputobject.append("<font color=\"#0000FF\">Positive Predictive Value</font>&nbsp;&nbsp;&nbsp;",HTML);
        outputobject.append("<font color=\"#FF00FF\">Negative Predictive Value</font><br>",HTML);
        outputobject.append("<font color=\"#FF9900\">False Discovery Rate</font>&nbsp;&nbsp;&nbsp;",HTML);
        outputobject.append("<font color=\"#A0A0A0\">False Negative Rate</font>&nbsp;&nbsp;&nbsp;",HTML);
        outputobject.append("<font color=\"#FF99E0\">False Omission Rate</font><br>",HTML);
        outputobject.append("<font color=\"#00FFFF\">Performance</font>&nbsp;&nbsp;&nbsp;",HTML);
        outputobject.append("<font color=\"#000000\">Accuracy</font><br>",HTML);
        outputobject.append("</td>",HTML);
        outputobject.append("</table>",HTML);
        outputobject.append("</div>",HTML);
        outputobject.append("</body></html>",HTML);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }



    private OutputData formatHTMLmultipleFeatures(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format, double scale, double[] legendscale) throws ExecutionError, InterruptedException {
        DecimalFormat decimalformatter=new DecimalFormat("0.#####");
        ArrayList<String> names=new ArrayList<String>(ROCmap.size());
        for (String s:ROCmap.keySet()) names.add(s);
        Collections.sort(names, new AUCSortComparator());
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();        
        if (featureProperty.equals("type")) { // filter based on visibility if the featureProperty is type
            for (Iterator<String> iterator = names.iterator(); iterator.hasNext();) {
                String type = iterator.next();
                if (!vizSettings.isRegionTypeVisible(type)) iterator.remove();
            }            
        }        
        File rocImageFile=outputobject.createDependentFile(engine,"png");
        try {
            saveMultipleROCGraphAsImage(rocImageFile, engine, scale, legendscale);
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        engine.createHTMLheader("ROC Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<div align=\"center\">\n",HTML);       
        outputobject.append("<h2 class=\"headline\">ROC curves showing the ability of property \""+scoreProperty+"\" to discriminate between positive and negative regions in "+regionDatasetName+"</h2>",HTML);
        outputobject.append("Regions where grouped by the property \""+featureProperty+"\". ",HTML);
        outputobject.append("True positive regions are those whose value of the \"<span class=\"dataitem\">"+classProperty+"</span>\" property equals \""+positiveClassValue+"\". All other regions are considered to be true negatives.<br><br>",HTML);
        outputobject.append("The evaluation assumes that all regions with a value of <span class=\"dataitem\">"+scoreProperty+"</span> ",HTML);
        if (aboveOrEqual) outputobject.append("greater than or equal to",HTML);
        else outputobject.append("strictly greater than",HTML);
        outputobject.append(" some varying threshold are <i>predicted</i> as positive while regions scoring ",HTML);
        if (aboveOrEqual) outputobject.append("below",HTML);
        else outputobject.append("equal to or below",HTML);        
        outputobject.append(" the threshold are <i>predicted</i> as negative.<br><br>",HTML);        
        outputobject.append("Analysis based on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),HTML);
        if (sequenceCollectionName!=null) outputobject.append(" from collection <span class=\"dataitem\">"+sequenceCollectionName+"</span>",HTML);
        outputobject.append("<br>",HTML);
        outputobject.append("<table>",HTML);
        outputobject.append("<tr><td style=\"border-width: 0px;\" align=\"center\"><img src=\"file:///"+rocImageFile.getAbsolutePath()+"\" /><br><h2>ROC<h2></td><td style=\"border-width: 0px;\" width=\"30\"></td>",HTML);
        outputobject.append("<td style=\"border-width: 0px;\">\n<table class=\"sortable\">\n",HTML);
        outputobject.append("<tr><th colspan=2>"+featureProperty+"</th><th class=\"sorttable_numeric\">AUC</th></tr>\n",HTML);
        for (int i=0;i<names.size();i++) {
            String featurename=names.get(i);
            double AUCvalue=AUCmap.get(featurename);
            Color useColor=Color.BLACK;
            if (featureProperty.equals("type")) useColor=vizSettings.getFeatureColor(featurename);
            else useColor=getPaletteColor(i);
            String colorString=VisualizationSettings.convertColorToHTMLrepresentation(useColor);             
            outputobject.append("<tr><td><div style=\"width:10px;height:10px;border:1px solid #000;background-color:"+colorString+";\"></div></td><td style=\"color:"+colorString+"\">"+featurename+"&nbsp;&nbsp;&nbsp;</td><td class=\"num\" style=\"color:"+colorString+"\">"+decimalformatter.format(AUCvalue)+"</td></tr>\n",HTML);
        }
        outputobject.append("</table>\n",HTML);
        outputobject.append("</td></tr>\n</table>",HTML);
        outputobject.append("</div>\n",HTML);
        outputobject.append("</body>\n</html>\n",HTML);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }

   @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        if (ROCmap==null) return formatExcelsingleFeature(outputobject, engine, settings, task, format);
        else return formatExcelmultipleFeatures(outputobject, engine, settings, task, format);
    }
    
    private OutputData formatExcelsingleFeature(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        XSSFWorkbook workbook=null;
        try {
            InputStream stream = CompareRegionDatasetsAnalysis.class.getResourceAsStream("resources/AnalysisTemplate_ROC.xlsx");
            workbook = new XSSFWorkbook(stream);
            stream.close();
            XSSFSheet sheet = workbook.getSheetAt(0);   // just use the first sheet 
          
            int toprow=100; // the data starts at row 101 (1-indexed) in the Excel template
            
            if (format!=null) format.setProgress(10); 
            
            // Set up data for ROC-curve
            for (int i=0;i<=resolution;i++) {
                // double xValue = ((double)i)/100.0;
                double rocValue = ROCvalues[i];
                Row row = sheet.getRow(toprow+i);
                int column = 1; // this is determined by the Excel template file    
                row.getCell(column).setCellValue(rocValue);
            }        
            if (format!=null) format.setProgress(30);  
            
            // set up data for Precision-Recall curve
            for (int i=0;i<=resolution;i++) {
                double recall = ((double)i)/100.0;
                double precision = PrecisionRecallvalues[i];
                Row row = sheet.getRow(toprow+i);
                int column = 3; // this is determined by the Excel template file    
                row.getCell(column++).setCellValue(recall);
                row.getCell(column++).setCellValue(precision);                
            }             
            if (format!=null) format.setProgress(50); 
            
            // set up data for all the statistics curves    
            long truePositive=positiveCount.get(null);
            long trueNegative=negativeCount.get(null);             
            double[] sensitivity=getStatistic("sensitivity");
            double[] specificity=getStatistic("specificity");
            double[] ppv=getStatistic("PPV");
            double[] npv=getStatistic("NPV");
            double[] fpr=getStatistic("false positive rate");
            double[] fnr=getStatistic("false negative rate");
            double[] fdr=getStatistic("false discovery rate");
            double[] fomr=getStatistic("false omission rate");
            double[] acc=getStatistic("accuracy");
            double[] perf=getStatistic("performance");
            double[] discr=getStatistic("discrimination");
            double[] avgsnsp=getStatistic("averageSnSp");        
            double[] fmeasure=getStatistic("F-measure");           
            double step=(maxScoreValue-minScoreValue)/(double)resolution;

            for (int i=0;i<=resolution;i++) {
                Row row = sheet.getRow(i+toprow); // 
                double threshold=minScoreValue+(i*step);
                int column=6; // this is determined by the Excel template file
                row.getCell(column++).setCellValue(threshold);
                row.getCell(column++).setCellValue(sensitivity[i]);
                row.getCell(column++).setCellValue(specificity[i]);
                row.getCell(column++).setCellValue(fpr[i]);
                row.getCell(column++).setCellValue(avgsnsp[i]);
                row.getCell(column++).setCellValue(discr[i]); 
                row.getCell(column++).setCellValue(fmeasure[i]); 
                row.getCell(column++).setCellValue(ppv[i]);
                row.getCell(column++).setCellValue(npv[i]); 
                row.getCell(column++).setCellValue(fdr[i]); 
                row.getCell(column++).setCellValue(fnr[i]);
                row.getCell(column++).setCellValue(fomr[i]); 
                row.getCell(column++).setCellValue(perf[i]);
                row.getCell(column++).setCellValue(acc[i]);
            } 
            if (format!=null) format.setProgress(70);            
            // histograms
            int numberOfBins=100; // this is hardcoded in the analysis below
            double binSize = (maxScoreValue-minScoreValue)/((double)numberOfBins);           
            for (int i=0;i<numberOfBins;i++) {
                Row row = sheet.getRow(i+toprow); //
                double binstart=minScoreValue+(binSize*i);
                int column=21; // this is determined by the Excel template file 
                row.getCell(column++).setCellValue(binstart);            
                row.getCell(column++).setCellValue(positiveBins[i]);    
                row.getCell(column++).setCellValue(negativeBins[i]);  
                row.getCell(column++).setCellValue((double)positiveBins[i]/(double)truePositive);  
                row.getCell(column++).setCellValue((double)negativeBins[i]/(double)trueNegative);              
            }        
            if (format!=null) format.setProgress(90);              
            // Add all the text
            String title = "Evaluation of property \""+scoreProperty+"\" for descriminating between positive and negative regions in \""+regionDatasetName+"\"";
            String subtitle = "Analysis based on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":"");
            if (sequenceCollectionName!=null) subtitle+=" from collection \""+sequenceCollectionName+"\"";            
            sheet.getRow(0).getCell(0).setCellValue(title);
            sheet.getRow(2).getCell(1).setCellValue(subtitle);
                                               
            int rowIndex=22;
            DecimalFormat decimalformatter=new DecimalFormat("0.#####");            
            String line1 = "These graphs summarize the ability of the region propery \""+scoreProperty+"\" to discriminate between "+truePositive+" positive";
            String line2 = "and "+trueNegative+" negative regions in \""+regionDatasetName+"\". True positive regions are those whose value of the \""+classProperty+"\" property equals \""+positiveClassValue+"\".";
            String line3= "All other regions are considered to be true negatives.";            
            String line4 = "The evaluation assumes that all bases with a value of \""+scoreProperty+"\" ";
            line4 += ((aboveOrEqual)?"greater than or equal to":"strictly greater than")+" some threshold";
            String line5= "are predicted as positive regions, while bases scoring "+((aboveOrEqual)?"below":"equal to or below");
            line5+=" the threshold are predicted as negative.";
            String line6 = "The performance of this property, as evaluated by the Area Under The Curve of the ROC-graph, is "+decimalformatter.format(AUC)+", which is "+getEvaluationOfAUC(AUC)+".";            
            String line7 = "The graphs on the right show how different performance statistics vary according to different threshold levels on the X-axis.";
            
            setCellValue(sheet, rowIndex++, 0, line1);
            setCellValue(sheet, rowIndex++, 0, line2);
            setCellValue(sheet, rowIndex++, 0, line3);
            setCellValue(sheet, rowIndex++, 0, line4);
            setCellValue(sheet, rowIndex++, 0, line5);
            setCellValue(sheet, rowIndex++, 0, line6);            
            setCellValue(sheet, rowIndex++, 0, "");  
            setCellValue(sheet, rowIndex++, 0, line7);         
            
            if (optimalThresholdBestSnSpsum.size()==1) {
                double[] range=optimalThresholdBestSnSpsum.get(0);
                String lineOptimal="The best trade-off between sensitivity and specificity is obtained with the ";
                if (range[0]==range[1]) lineOptimal+="threshold "+decimalformatter.format(range[0])+".";
                else lineOptimal+="threshold in the range ["+decimalformatter.format(range[0])+","+decimalformatter.format(range[1])+"].";
                setCellValue(sheet, rowIndex++, 0, lineOptimal);
            } else {
                String lineOptimal="The distribution of \""+scoreProperty+"\" allows for multiple different thresholds which can achieve optimal trade-off between sensitivity and specificity:";
                setCellValue(sheet, rowIndex++, 0, lineOptimal);
                for (double[] range:optimalThresholdBestSnSpsum) {
                   if (range[0]==range[1]) setCellValue(sheet, rowIndex++, 1, decimalformatter.format(range[0]));
                   else setCellValue(sheet, rowIndex++, 1, decimalformatter.format(range[0])+" - "+decimalformatter.format(range[1]));;
                }
            }           
            if (optimalThresholdBestAccuracy.size()==1) {
                double[] range=optimalThresholdBestAccuracy.get(0);
                String lineOptimal="The highest obtainable accuracy (% of all correctly predicted bases) is "+decimalformatter.format(bestAccuracy);
                if (range[0]==range[1]) lineOptimal+=" with the threshold "+decimalformatter.format(range[0])+".";
                else lineOptimal+=" with a threshold in the range ["+decimalformatter.format(range[0])+","+decimalformatter.format(range[1])+"].";
                setCellValue(sheet, rowIndex++, 0, lineOptimal);
            } else {
                String lineOptimal="The highest obtainable accuracy (% of all correctly predicted bases) is "+decimalformatter.format(bestAccuracy)+".";
                lineOptimal+="The distribution of  \""+scoreProperty+"\" allows for multiple different thresholds which can achieve optimal accuracy:";
                setCellValue(sheet, rowIndex++, 0, lineOptimal);
                for (double[] range:optimalThresholdBestAccuracy) {
                   if (range[0]==range[1]) setCellValue(sheet, rowIndex++, 1, decimalformatter.format(range[0]));
                   else setCellValue(sheet, rowIndex++, 1, decimalformatter.format(range[0])+" - "+decimalformatter.format(range[1]));;
                }
            }   
//            String line7 = "Note that the optimal accuracy-threshold can differ from the threshold for best sensitivity/specificity if the ratio";
//            String line8 = "between the number of positive versus negative regions is skewed. ";
//            line8+= "In this dataset the inside-to-outside ratio is ";
//            if (inside<outside) line8+="1:"+decimalformatter.format((double)outside/(double)inside)+".";
//            else line8+=decimalformatter.format((double)inside/(double)outside)+":1.";  
//            setCellValue(sheet, rowIndex++, 0, line7);
//            setCellValue(sheet, rowIndex++, 0, line8);             
            
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
    
    private Cell setCellValue(XSSFSheet sheet, int row, int column, String value) {
        Row r = sheet.getRow(row);
        if (r==null) r=sheet.createRow(row);
        Cell cell = r.getCell(column);
        if (cell==null) cell=r.createCell(column);
        cell.setCellValue(value);
        return cell;
    }
    
    private Cell setCellValue(XSSFSheet sheet, int row, int column, double value) {
        Row r = sheet.getRow(row);
        if (r==null) r=sheet.createRow(row);
        Cell cell = r.getCell(column);
        if (cell==null) cell=r.createCell(column);
        cell.setCellValue(value);
        return cell;
    }    

    private OutputData formatExcelmultipleFeatures(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        XSSFWorkbook workbook=null;
        try {
            InputStream stream = CompareRegionDatasetsAnalysis.class.getResourceAsStream("resources/AnalysisTemplate_ROC_multiple.xlsx");
            workbook = new XSSFWorkbook(stream);
            stream.close();
            XSSFSheet sheet = workbook.getSheetAt(0);   // 
            XSSFSheet datasheet = workbook.getSheetAt(1);   // 
            // update the data range of the histogram
            XSSFDrawing drawing = ((XSSFSheet)sheet).createDrawingPatriarch();           
            XSSFChart rocChart = drawing.getCharts().get(0);
            // Access the chart data
            XDDFChartData data = rocChart.getChartSeries().get(0);
            CellStyle tableheaderstyle = getExcelTableHeaderStyle(workbook);
                     
            if (format!=null) format.setProgress(10);   
            ArrayList<String> names=new ArrayList<String>(ROCmap.size());
            for (String s:ROCmap.keySet()) names.add(s);
            Collections.sort(names, new AUCSortComparator());
            VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
            if (featureProperty.equals("type")) { // filter based on visibility if the featureProperty is type
                for (Iterator<String> iterator = names.iterator(); iterator.hasNext();) {
                    String type = iterator.next();
                    if (!vizSettings.isRegionTypeVisible(type)) iterator.remove();
                }            
            }   
            int toprow=1; //
            int numdatarows=101;
            XDDFDataSource<Double> xValues = XDDFDataSourcesFactory.fromNumericCellRange(datasheet, new CellRangeAddress(toprow, toprow+numdatarows-1, 0, 0)); // X values
            
            int row = 8; // determined by the Excel template file
            int column = 8; // determined by the Excel template file
            int datacolumn = 2;
            Color bgcolor = new Color(255,255,220);
            CellStyle valueCellStyle=createExcelStyle(workbook, BorderStyle.THIN, null, bgcolor, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);
            for (int i=0;i<names.size();i++) {
                String featurename=names.get(i);
                Color useColor=Color.BLACK;
                if (featureProperty.equals("type")) useColor=vizSettings.getFeatureColor(featurename);
                else useColor=getPaletteColor(i);                
                double AUCvalue=AUCmap.get(featurename);
                CellStyle style=createExcelStyle(workbook, BorderStyle.THIN, useColor, bgcolor, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);                
                setCellValue(sheet, row, column, featurename).setCellStyle(style);
                setCellValue(sheet, row, column+1, AUCvalue).setCellStyle(valueCellStyle);
                double[] values=ROCmap.get(featurename);
                // add data to the second sheet
                for (int j=0;j<=resolution;j++) {
                    setCellValue(datasheet,toprow+j,datacolumn,values[j]);
                } 
                XDDFNumericalDataSource<Double> newSeriesData = XDDFDataSourcesFactory.fromNumericCellRange(datasheet, new CellRangeAddress(toprow, toprow+numdatarows-1, datacolumn, datacolumn));                
                XDDFLineChartData.Series newSeries = (XDDFLineChartData.Series) data.addSeries(xValues, newSeriesData);
                newSeries.setTitle(featurename, null);   
                XDDFSolidFillProperties fillProperties = getExcelFillColor(useColor);
                XDDFLineProperties lineProperties = new XDDFLineProperties();
                lineProperties.setFillProperties(fillProperties);
                lineProperties.setWidth(2.0);
                newSeries.setLineProperties(lineProperties);
                newSeries.setMarkerStyle(MarkerStyle.NONE);
                newSeries.setSmooth(true);           

                setCellValue(datasheet,toprow-1,datacolumn,featurename).setCellStyle(tableheaderstyle); // header (not really used)
                datacolumn++;
                row++;
            }       
            rocChart.plot(data);            
            if (format!=null) format.setProgress(90);              
            // Add all the text            
            String title = "ROC analysis";
            String line1 = "These ROC curves show the ability of region property \""+scoreProperty+"\" to discriminate between positive and negative regions in \""+regionDatasetName+"\" (grouped by \""+featureProperty+"\")";
            String line2 = "True positive regions are those whose value of the \""+classProperty+"\" property equals \""+positiveClassValue+"\". All other regions are considered to be true negatives.";
            String line3 = "Regions that score "+((aboveOrEqual)?"above or equal to":"stricly above")+" a varying threshold of the property \""+scoreProperty+"\" are PREDICTED as positive instances.";
            String line4 = "Analysis based on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":"");
            if (sequenceCollectionName!=null) line3+=" from collection \""+sequenceCollectionName+"\"";            
            sheet.getRow(0).getCell(0).setCellValue(title);
            sheet.getRow(2).getCell(1).setCellValue(line1);
            sheet.getRow(3).getCell(1).setCellValue(line2); 
            sheet.getRow(4).getCell(1).setCellValue(line3);
            sheet.getRow(5).getCell(1).setCellValue(line4);   
            
            sheet.getRow(7).getCell(8).setCellValue(featureProperty); // Replace the generic "feature" header in the template with the name of the actual feature
            
        } catch (Exception e) {
            // e.printStackTrace(System.err);
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



    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        if (ROCmap==null) return formatRAWsingleFeature(outputobject, engine, settings, task, format);
        else return formatRAWmultipleFeatures(outputobject, engine, settings, task, format);
    }

    private OutputData formatRAWsingleFeature(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        DecimalFormat decimalformatter=new DecimalFormat("0.#####");
        outputobject.append("#Evaluation of the ability of region property \""+scoreProperty+"\" to discriminate between positive and negative regions in \""+regionDatasetName+"\".\n",RAWDATA);
        outputobject.append("#True positive regions are those whose value of the \""+classProperty+"\" property equals \""+positiveClassValue+"\". All other regions are considered to be true negatives.\n",RAWDATA);        
        outputobject.append("#Regions that score "+((aboveOrEqual)?"above or equal to":"stricly above")+" a varying threshold of the property \""+scoreProperty+"\" are PREDICTED as positive instances.\n",RAWDATA);
        outputobject.append("#Analysis based on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),RAWDATA);
        if (sequenceCollectionName!=null) outputobject.append(" from collection '"+sequenceCollectionName+"'",RAWDATA);        
        outputobject.append("\n",RAWDATA);
        outputobject.append("AUC="+AUC+"\n",RAWDATA);
        if (optimalThresholdBestSnSpsum.size()==1) {
            double[] range=optimalThresholdBestSnSpsum.get(0);
            if (range[0]==range[1]) outputobject.append("Best Sensitivity vs Specificity threshold="+decimalformatter.format(range[0])+"\n",RAWDATA);
            else outputobject.append("Best Sensitivity vs Specificity threshold=["+decimalformatter.format(range[0])+","+decimalformatter.format(range[1])+"\n",RAWDATA);
        } else {
           for (double[] range:optimalThresholdBestSnSpsum) {
               if (range[0]==range[1]) outputobject.append("Best Sensitivity vs Specificity threshold="+decimalformatter.format(range[0])+"\n",RAWDATA);
               else outputobject.append("Best Sensitivity vs Specificity threshold=["+decimalformatter.format(range[0])+" - "+decimalformatter.format(range[1])+"\n",RAWDATA);
           }
        }
        outputobject.append("Best accuracy="+decimalformatter.format(bestAccuracy)+"\n",RAWDATA);        
        if (optimalThresholdBestAccuracy.size()==1) {
            double[] range=optimalThresholdBestAccuracy.get(0);            
            if (range[0]==range[1]) outputobject.append("Best accuracy threshold="+decimalformatter.format(range[0])+"\n",RAWDATA);
            else outputobject.append("Best accuracy threshold=["+decimalformatter.format(range[0])+","+decimalformatter.format(range[1])+"]\n",RAWDATA);
        } else {
            for (double[] range:optimalThresholdBestAccuracy) {
               if (range[0]==range[1]) outputobject.append("Best accuracy threshold="+decimalformatter.format(range[0])+"\n",HTML);
               else outputobject.append("Best accuracy threshold=["+decimalformatter.format(range[0])+" - "+decimalformatter.format(range[1])+"\n",HTML);
           }
        }
        outputobject.append("\n\n",RAWDATA);
        if (format!=null) format.setProgress(20);   
        outputobject.append("#X-value\tROC\n",RAWDATA);   
        for (int i=0;i<=resolution;i++) {
            double xValue = ((double)i)/100.0;
            double rocValue = ROCvalues[i];
            outputobject.append(xValue+"\t"+rocValue+"\n", RAWDATA);
        }        
        
        outputobject.append("\n\n",RAWDATA);
        
        if (format!=null) format.setProgress(40);   
        outputobject.append("#Recall\tPrecision\n",RAWDATA);
        for (int i=0;i<=resolution;i++) {
            double recall = ((double)i)/100.0;
            double precision = PrecisionRecallvalues[i];
            outputobject.append(recall+"\t"+precision+"\n", RAWDATA);
        }     
        
        outputobject.append("\n\n",RAWDATA);
        
        if (format!=null) format.setProgress(60);
        outputobject.append("\n\n",RAWDATA);
        outputobject.append("#Threshold\tSn\tSp\tFPR\tAvg Sn&Sp\tDist Sn-FDR\tF-measure\tPPV\tNPV\tFDR\tFNR\tFOR\tPerformace\tAccuracy\n",RAWDATA);
        double[] sensitivity=getStatistic("sensitivity");
        double[] specificity=getStatistic("specificity");
        double[] ppv=getStatistic("PPV");
        double[] npv=getStatistic("NPV");
        double[] fpr=getStatistic("false positive rate");
        double[] fnr=getStatistic("false negative rate");
        double[] fdr=getStatistic("false discovery rate");
        double[] fomr=getStatistic("false omission rate");
        double[] acc=getStatistic("accuracy");
        double[] perf=getStatistic("performance");
        double[] discr=getStatistic("discrimination");
        double[] avgsnsp=getStatistic("averageSnSp");        
        double[] fmeasure=getStatistic("F-measure");           
        double step=(maxScoreValue-minScoreValue)/(double)resolution;
        for (int i=0;i<=resolution;i++) {
            double threshold=minScoreValue+(i*step);
            outputobject.append(""+threshold,RAWDATA);
            outputobject.append("\t"+sensitivity[i],RAWDATA);
            outputobject.append("\t"+specificity[i],RAWDATA); 
            outputobject.append("\t"+fpr[i],RAWDATA);
            outputobject.append("\t"+avgsnsp[i],RAWDATA);
            outputobject.append("\t"+discr[i],RAWDATA); 
            outputobject.append("\t"+fmeasure[i],RAWDATA); 
            outputobject.append("\t"+ppv[i],RAWDATA);
            outputobject.append("\t"+npv[i],RAWDATA); 
            outputobject.append("\t"+fdr[i],RAWDATA); 
            outputobject.append("\t"+fnr[i],RAWDATA);
            outputobject.append("\t"+fomr[i],RAWDATA); 
            outputobject.append("\t"+perf[i],RAWDATA); 
            outputobject.append("\t"+acc[i],RAWDATA);    
            outputobject.append("\n",RAWDATA);   
        }        
         if (format!=null) format.setProgress(80);       
        // histograms
        int numberOfBins=100; // this is hardcoded in the analysis below
        double binSize = (maxScoreValue-minScoreValue)/((double)numberOfBins); 
        outputobject.append("\n\n#Histogram\n",RAWDATA);
        outputobject.append("#Threshold\tPositive\tNegative\tPositive normalized\tNegative normalized\n",RAWDATA);              
        for (int i=0;i<numberOfBins;i++) {
            double binstart=minScoreValue+(binSize*i);
            outputobject.append(""+binstart,RAWDATA);            
            outputobject.append("\t"+positiveBins[i],RAWDATA);    
            outputobject.append("\t"+negativeBins[i],RAWDATA);  
            outputobject.append("\t"+((double)positiveBins[i]/(double)positiveCount.get(null)),RAWDATA);  
            outputobject.append("\t"+((double)negativeBins[i]/(double)negativeCount.get(null)),RAWDATA);   
            outputobject.append("\n",RAWDATA);             
        }             
        if (format!=null) format.setProgress(100);
        return outputobject;
    }

    private OutputData formatRAWmultipleFeatures(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        ArrayList<String> names=new ArrayList<String>(ROCmap.size());
        for (String s:ROCmap.keySet()) names.add(s);
        Collections.sort(names, new AUCSortComparator());
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();       
        outputobject.append("#Evaluation of the ability of region property \""+scoreProperty+"\" to discriminate between positive and negative regions in "+regionDatasetName+", as measured by the Area Under the Curve (AUC) of the ROC-curves.\n",RAWDATA);
        outputobject.append("#Regions where grouped by the property \""+featureProperty+"\".\n",RAWDATA);
        outputobject.append("#True positive regions are those whose value of the \""+classProperty+"\" property equals \""+positiveClassValue+"\". All other regions are considered to be true negatives.\n",RAWDATA);        
        outputobject.append("#Regions that score "+((aboveOrEqual)?"above or equal to":"stricly above")+" a varying threshold of the property \""+scoreProperty+"\" are PREDICTED as positive instances.\n",RAWDATA);       
        outputobject.append("#Analysis based on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),RAWDATA);
        if (sequenceCollectionName!=null) outputobject.append(" from collection '"+sequenceCollectionName+"'",RAWDATA);
        outputobject.append("\n",RAWDATA);
        for (int i=0;i<names.size();i++) {
            String featurename=names.get(i);
            if (vizSettings!=null && !vizSettings.isTrackVisible(featurename)) continue;
            double AUCvalue=AUCmap.get(featurename);
            outputobject.append(featurename+"="+AUCvalue+"\n",RAWDATA);
        }
        if (format!=null) format.setProgress(100);
        return outputobject;
    }


    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        int bins=100;
        RegionDataset regionTrack=(RegionDataset)task.getParameter("Target track");
        regionDatasetName=regionTrack.getName();
        featureProperty=(String)task.getParameter("Feature property");
        if (featureProperty!=null && featureProperty.trim().isEmpty()) featureProperty=null;
        scoreProperty=(String)task.getParameter("Score property");      
        classProperty=(String)task.getParameter("Class property");
        if (classProperty==null || classProperty.trim().isEmpty()) throw new ExecutionError("Missing required parameter 'Class property'");
        positiveClassValue=(String)task.getParameter("Positive class");
        //negativeClassValue=(String)task.getParameter("Negative class");
        //if (positiveClassValue.equals(negativeClassValue)) throw new ExecutionError("The value for the positive class should be different from the negative class");
        
        SequenceCollection sequenceCollection=(SequenceCollection)task.getParameter("Sequences");
        if (sequenceCollection==null) sequenceCollection=task.getEngine().getDefaultSequenceCollection();
        if (sequenceCollection.getName().equals(task.getEngine().getDefaultSequenceCollectionName())) sequenceCollectionName=null;
        else sequenceCollectionName=sequenceCollection.getName();
        String aboveOrEqualString=(String)task.getParameter("Threshold");
        if (aboveOrEqualString!=null && aboveOrEqualString.equalsIgnoreCase("Strictly above")) aboveOrEqual=false;
        else aboveOrEqual=true;
        positiveCount=new HashMap<>();
        negativeCount=new HashMap<>();
        positiveCount.put(null,new Long(0));
        negativeCount.put(null,new Long(0));
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(task.getEngine());
        sequenceCollectionSize=sequences.size();
        if (featureProperty!=null) { // Group regions according to given feature and create a ROC curve for each group
            AUCmap=new HashMap<String, Double>();
            ROCmap=new HashMap<String, double[]>(); 
            calculateROCsForMultipleGroups(sequences, regionTrack, task);
            return; // <== NB: The "multiple groups" analysis ends here!!!!
        }
        
        // === NB: If (valueTrack==null) the method will already have returned at this point!!
        
        // From this point on we should analyse a single group (comprising all regions) and calculate lots of different statistics
        double[] minmaxRange=getMinMaxCountFromTrack(regionTrack,sequenceCollection,scoreProperty);
        minScoreValue=minmaxRange[0];
        maxScoreValue=minmaxRange[1];
        double regioncount=minmaxRange[2];
        positiveBins=new double[bins];
        negativeBins=new double[bins];
        optimalThresholdBestSnSpsum=new ArrayList<double[]>();
        optimalThresholdBestAccuracy=new ArrayList<double[]>();
        ArrayList<double[]> values=new ArrayList<double[]>(); // first double[0]=> score property value. double[1]=> 1 if positive or 0 if negative

        TaskRunner taskRunner=task.getEngine().getTaskRunner();
        task.setProgress(0L,sequences.size());
        long[] counters=new long[]{0,0,sequences.size()}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences (multiplied by tracks)        
        
        ArrayList<ProcessSequenceTaskNoGroups> processTasks=new ArrayList<ProcessSequenceTaskNoGroups>(sequences.size());
        for (int i=0;i<sequences.size();i++) {
            String sequenceName=sequences.get(i).getName();         
            processTasks.add(new ProcessSequenceTaskNoGroups(regionTrack, sequenceName, values, bins, task, counters));
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
        if (countOK!=sequences.size()) {
            throw new ExecutionError("Some mysterious error occurred while performing analysis: "+getAnalysisName());
        }
        
        Object[] stats=getROCvalues(values,null);
        ROCvalues=(double[])stats[0];
        AUC=(Double)stats[1];
        task.setProgress(80); //
        task.setStatusMessage("Executing analysis: "+getAnalysisName()+"  (Calculating statistics...)");
        TPFPTNFN=new long[resolution+1][];
        double threshold=minScoreValue;
        double step=(maxScoreValue-minScoreValue)/(double)resolution;
        // DEBUG: for (double[] v:values) { System.err.println(v[0]+"\t"+v[1]);}
        // count TP, FP, TN and FN for each level. Also find best (Sn+Sp) and Accuracy
        for (int i=0;i<=resolution;i++) {
            threshold=minScoreValue+(i*step);
            TPFPTNFN[i]=countTPFPTNFN(values, threshold, aboveOrEqual);
            double sensitivity=(double)TPFPTNFN[i][0]/(double)(TPFPTNFN[i][0]+TPFPTNFN[i][3]);
            double specificity=(double)TPFPTNFN[i][2]/(double)(TPFPTNFN[i][1]+TPFPTNFN[i][2]);
            double besttotal=sensitivity+specificity; // is this good or should I use (Sn+Sp)/2;
            double bestaccuracy=(double)(TPFPTNFN[i][0]+TPFPTNFN[i][2])/(double)(TPFPTNFN[i][0]+TPFPTNFN[i][1]+TPFPTNFN[i][2]+TPFPTNFN[i][3]);
            if (besttotal>bestSnSpsum) bestSnSpsum=besttotal;
            if (bestaccuracy>bestAccuracy) bestAccuracy=bestaccuracy;
            // System.err.println("["+i+"]{"+threshold+"} TP="+TPFPTNFN[i][0]+", FP="+TPFPTNFN[i][1]+", TN="+TPFPTNFN[i][2]+", FN="+TPFPTNFN[i][3]+"    besttotal="+besttotal+"    bestAccuracy="+bestaccuracy);
            int percentage=(int)(i/(double)resolution*100.0);
            task.setStatusMessage("Executing analysis: "+getAnalysisName()+"  (Calculating statistics: "+percentage+"%)");
            task.setProgress(80+(int)(percentage/10.0));
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();            
        }
        task.setProgress(90); //
        task.setStatusMessage("Executing analysis: "+getAnalysisName()+"  (Calculating Precision-recall)");
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        // calculate Precision-Recall values. with recall (sensitivity) on the X-axis (index) and Precision (PPV) on the Y-axis (value)
        PrecisionRecallvalues=new double[resolution+1];
        double[] precision=getStatistic("PPV");
        double[] recall=getStatistic("sensitivity");
        double lastvalid=0;    
        for (int i=resolution;i>=0;i--) {  
            ArrayList<Integer> thresholds=getThresholdsForRecallRange((double)i/(double)resolution,(double)(i+1)/(double)resolution,recall);
            if (thresholds.isEmpty()) {
                PrecisionRecallvalues[i]=lastvalid; // for missing values, interpolate with last valid value
            } else  {
                double bestPrecision=getHighestPrecisionForThreshold(thresholds,precision);
                PrecisionRecallvalues[i]=bestPrecision;
                lastvalid=bestPrecision;
            }
        }
        
        task.setProgress(95); //
        task.setStatusMessage("Executing analysis: "+getAnalysisName()+"  (Finding optimal thresholds)");
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        //System.err.println("BestSum="+bestSnSpsum+"   bestAccuracy="+bestAccuracy);
        //set threshold ranges for best values
        boolean insideBest=false;
        int leftEnd=0;
        for (int i=0;i<=resolution;i++) {
            double sensitivity=(double)TPFPTNFN[i][0]/(double)(TPFPTNFN[i][0]+TPFPTNFN[i][3]);
            double specificity=(double)TPFPTNFN[i][2]/(double)(TPFPTNFN[i][1]+TPFPTNFN[i][2]);
            double sum=sensitivity+specificity;
            if (sum==bestSnSpsum) {
                if (!insideBest) {leftEnd=i;insideBest=true;}
            } else {
                if (insideBest) {
                    double minThresholdBestSnSpsum=minScoreValue+step*leftEnd;
                    double maxThresholdBestSnSpsum=minScoreValue+step*(i-1);
                    optimalThresholdBestSnSpsum.add(new double[]{minThresholdBestSnSpsum,maxThresholdBestSnSpsum});
                    insideBest=false;
                }
            }
        }
        task.setProgress(98); //
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        if (insideBest) {
            double minThresholdBestSnSpsum=minScoreValue+step*leftEnd;
            double maxThresholdBestSnSpsum=maxScoreValue;
            optimalThresholdBestSnSpsum.add(new double[]{minThresholdBestSnSpsum,maxThresholdBestSnSpsum});
        }
        insideBest=false;
        leftEnd=0;
        for (int i=0;i<=resolution;i++) {
            double accuracy=(double)(TPFPTNFN[i][0]+TPFPTNFN[i][2])/(double)(TPFPTNFN[i][0]+TPFPTNFN[i][1]+TPFPTNFN[i][2]+TPFPTNFN[i][3]);
            if (accuracy==bestAccuracy) {
                if (!insideBest) {leftEnd=i;insideBest=true;}
            } else {
                if (insideBest) {
                    double minThresholdBestAccuracy=minScoreValue+step*leftEnd;
                    double maxThresholdBestAccuracy=minScoreValue+step*(i-1);
                    optimalThresholdBestAccuracy.add(new double[]{minThresholdBestAccuracy,maxThresholdBestAccuracy});
                    insideBest=false;
                }
            }
        }
        if (insideBest) {
            double minThresholdBestAccuracy=minScoreValue+step*leftEnd;
            double maxThresholdBestAccuracy=maxScoreValue;
            optimalThresholdBestAccuracy.add(new double[]{minThresholdBestAccuracy,maxThresholdBestAccuracy});
        }
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setProgress(100); //
    }
    
    
    
    private double[] getMinMaxCountFromTrack(RegionDataset track, SequenceCollection collection, String scoreproperty) {
        double[] results=new double[]{Double.MAX_VALUE,-Double.MAX_VALUE,0}; // first is minimum score value, second is max score value, third is region count (only eligible regions)
        ArrayList<FeatureSequenceData> sequences=(collection!=null)?track.getSequencesFromCollection(collection):track.getAllSequences();
        for (FeatureSequenceData sequence:sequences) {
            for (Region region:((RegionSequenceData)sequence).getOriginalRegions()) {
                Object propvalue=region.getProperty(scoreproperty);
                if (propvalue instanceof Number) {
                    double value=((Number)propvalue).doubleValue();
                    if (value<results[0]) results[0]=value;
                    if (value>results[1]) results[1]=value;
                    results[2]+=1; // increment region count
                }
            }
        }
        return results;
    }
    
    

    /**
     * 
     * @param fromRecall lower range of recall (inclusive)
     * @param toRecall   upper range of recall (exclusive)
     * @return 
     */
    private ArrayList<Integer> getThresholdsForRecallRange(double fromRecall, double toRecall, double[] recall) {
        ArrayList<Integer> result=new ArrayList<Integer>();
        for (int i=0;i<recall.length;i++) {
            if (recall[i]>=fromRecall && recall[i]<toRecall) result.add(i);
        }
        return result;
    }

    private double getHighestPrecisionForThreshold(ArrayList<Integer> bins, double[] precision) {
        double max=-Double.MAX_VALUE;
        for (int i:bins) {
            double value=precision[i];
            if (value>max) max=value;
        }
        return max;
    }   
    
    private void calculateROCsForMultipleGroups(ArrayList<Sequence> sequences, RegionDataset regionTrack, OperationTask task) throws Exception {
        int totalsize=0; // this is used for progress...
        // HashSet<String> types=regionTrack.getAllRegionTypes();
        HashSet<String> types = new HashSet<>();
        for (FeatureSequenceData seq: regionTrack.getAllSequences()) {
            RegionSequenceData regionSeq = (RegionSequenceData)seq;
            for (Region region:regionSeq.getAllRegions()) {
                Object value = region.getProperty(featureProperty);
                if (value==null) types.add("undefined");
                else types.add(value.toString());
                // if (types.size()>1000)) break; // set a hard limit on the number of groups?
            }
        }       
        int sequencesSize=sequences.size(); // number of sequences to process
        int steps=types.size()*sequencesSize;
        
        int progress=0;
        TaskRunner taskRunner=task.getEngine().getTaskRunner();
        task.setProgress(0L,steps);
        long[] counters=new long[]{0,0,steps}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences (multiplied by tracks)

        for (String featureName:types) {
            ArrayList<double[]> values=new ArrayList<double[]>(totalsize); // the arrays are pairs: double[0]=> score property value. double[1]=> 1 if positive or 0 if negative
            
            ArrayList<ProcessSequenceTaskMultipleGroups> processTasks=new ArrayList<ProcessSequenceTaskMultipleGroups>(sequencesSize);
            for (int i=0;i<sequencesSize;i++) {
                String sequenceName=sequences.get(i).getName();         
                processTasks.add(new ProcessSequenceTaskMultipleGroups(regionTrack, featureName, sequenceName, values, task, counters));
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
            if (countOK!=sequencesSize) {
                throw new ExecutionError("Some mysterious error occurred while performing analysis: "+getAnalysisName());
            }                       
            Object[] stats=getROCvalues(values,featureName);
            ROCvalues=(double[])stats[0];
            double featureAUC=(Double)stats[1];
            ROCmap.put(featureName, ROCvalues);
            AUCmap.put(featureName, featureAUC);
            if (featureAUC>AUC) AUC=featureAUC; // same best
            progress++;
        } // end for each feature dataset
    }    
    
    /** Used when processing a sequence in multi-group mode (but only a single group is considered here, specified by targetFeature) */
    private int[] processSequenceMultipleGroups(RegionSequenceData regionTrackSequence, String scoreProperty, String featureProperty, String targetFeature, String classProperty, String positiveValue, ArrayList<double[]> values) {
        int positive_this=0; int negative_this=0;
        
        for (Region region:regionTrackSequence.getOriginalRegions()) {
            Object featurePropertyValue=region.getProperty(featureProperty);
            if (featurePropertyValue==null || !featurePropertyValue.toString().equals(targetFeature)) continue; // skip region that is not in the target feature group      
            double value=0;
            Object scoreValue=region.getProperty(scoreProperty);
            if (scoreValue instanceof Number) {value=((Number)scoreValue).doubleValue();}
            else continue; // skip regions that do not have a numeric value for the score property rather than just using a default
            boolean isPositive=false;
            Object classValue=region.getProperty(classProperty);
            if (classValue!=null) {
                if (classValue instanceof Boolean) isPositive=((Boolean)classValue);
                else isPositive=classValue.toString().equals(positiveValue);
            }  
            if (isPositive) positive_this++; else negative_this++;
            synchronized(values) {
                values.add(new double[]{value,(isPositive)?1f:0f});
            }
        }        
        return new int[]{positive_this,negative_this}; // return the number of positive and negative regions in this sequence for this particular feature        
    }
    
    /** Used when processing a sequence where all the regions are included (but just a subgroup) */
    private Object[] processSequenceNoGroups(RegionSequenceData regionTrackSequence, String scoreProperty, String classProperty, String positiveValue, ArrayList<double[]> values, int bins) {
        int positive_this=0; int negative_this=0;        
        double binrange=(maxScoreValue-minScoreValue)/(double)bins;
        double[] positiveBins_this=new double[bins];
        double[] negativeBins_this=new double[bins];        
        
        for (Region region:regionTrackSequence.getOriginalRegions()) {
            double value=0;
            Object scoreValue=region.getProperty(scoreProperty);
            if (scoreValue instanceof Number) {value=((Number)scoreValue).doubleValue();}
            else continue; // skip regions that do not have a numeric value for the score property rather than just using a default
            boolean isPositive=false;
            Object classValue=region.getProperty(classProperty);
            if (classValue!=null) {
                if (classValue instanceof Boolean) isPositive=((Boolean)classValue);
                else isPositive=classValue.toString().equals(positiveValue);
            }              
            int bin=(int)((value-minScoreValue)/binrange);
            if (bin>=bins) bin=bins-1;
            if (isPositive) {
                positive_this++;
                positiveBins_this[bin]++;
            } else {
                negative_this++;
                negativeBins_this[bin]++;
            }
            synchronized(values) {
                values.add(new double[]{value,(isPositive)?1f:0f});
            }
        }        
        return new Object[]{positive_this, negative_this, positiveBins_this, negativeBins_this};        
    }    
    
    
    protected class ProcessSequenceTaskNoGroups implements Callable<String> {
        final String sequenceName;
        final RegionDataset targetDataset;
        final long[] counters; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final OperationTask task;          
        final ArrayList<double[]> values;
        final int bins;
        
        public ProcessSequenceTaskNoGroups(RegionDataset targetDataset, String sequenceName, ArrayList<double[]> values, int bins, OperationTask task, long[] counters) {
           this.sequenceName=sequenceName;
           this.targetDataset=targetDataset;
           this.values=values;
           this.bins=bins;
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
            RegionSequenceData targetSequence=(RegionSequenceData)targetDataset.getSequenceByName(sequenceName);
            
            Object[] results=processSequenceNoGroups(targetSequence, scoreProperty, classProperty, positiveClassValue, values, bins);           
                    
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                int positive_this=(Integer)results[0];
                int negative_this=(Integer)results[1];
                double[] positiveBins_this=(double[])results[2];
                double[] negativeBins_this=(double[])results[3];
                positiveCount.put(null,positiveCount.get(null)+positive_this); // use NULL key for single group case
                negativeCount.put(null,negativeCount.get(null)+negative_this); // use NULL key for single group case
                for (int i=0;i<bins;i++) {
                    positiveBins[i]+=positiveBins_this[i];
                    negativeBins[i]+=negativeBins_this[i];
                }
                task.setStatusMessage("Executing analysis: "+getAnalysisName()+"  (Gathering data for sequence "+counters[1]+"/"+counters[2]+")");
                int progress=(int)(((double)counters[1]/(double)counters[2])*80); // take this progress up to 80%
                task.setProgress(progress);                              
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();    
            return sequenceName;
        }   
    }     
    
    
    protected class ProcessSequenceTaskMultipleGroups implements Callable<String> {
        final String sequenceName;
        final String featureName;
        final RegionDataset targetDataset;
        final long[] counters; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final OperationTask task;  
        final ArrayList<double[]> values;
        
        public ProcessSequenceTaskMultipleGroups(RegionDataset targetDataset, String featureName, String sequenceName, ArrayList<double[]> values, OperationTask task, long[] counters) {
           this.sequenceName=sequenceName;
           this.featureName=featureName;
           this.targetDataset=targetDataset;
           this.values=values;
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
            RegionSequenceData targetSequence=(RegionSequenceData)targetDataset.getSequenceByName(sequenceName);
            
            int[] results=processSequenceMultipleGroups(targetSequence, scoreProperty, featureProperty, featureName, classProperty, positiveClassValue, values);            
            
            synchronized(counters) { // finished one of the sequences (one feature!)
                counters[1]++; // number of sequences completed
                if (!positiveCount.containsKey(featureName)) {
                   positiveCount.put(featureName,new Long(results[0])); // these are probably group-specific :-S
                   negativeCount.put(featureName,new Long(results[1])); // these are probably group-specific :-S                 
                } else {
                   positiveCount.put(featureName,positiveCount.get(featureName)+results[0]); 
                   negativeCount.put(featureName,negativeCount.get(featureName)+results[1]);                
                }

                task.setStatusMessage("Executing analysis: "+getAnalysisName()+"  ("+counters[1]+"/"+counters[2]+")");
                //int full=(int)(counters[2]*1.05); // add a bit of margin to the end
                task.setProgress(counters[1],counters[2]);                              
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();    
            return sequenceName;
        }   
    }       
    
    // This is used for both multiple group and single group analysis
    private Object[] getROCvalues(ArrayList<double[]> values, String group) {
        Collections.sort(values, new SortComparator()); // sort values in descending order or priors score
        double[] ROCvalues=new double[101]; // note: this hides a field, but that is OK
        int positiveSeen=0; int negativeSeen=0; int result=0; // set this to 1 if AUC should be 1 and -1 if AUC should be 0
        long positivecount=0;
        long negativecount=0;
        if (positiveCount.containsKey(group)) positivecount=positiveCount.get(group);
        if (negativeCount.containsKey(group)) negativecount=negativeCount.get(group);
        int i=0;
        int totalsize=values.size();
        int lastindex=0;
        while (i<totalsize) {
            double[] entry=values.get(i);
            int[] nextEntries=lookahead(values, i); // check if the entries coming after this one have same priors score
            if (nextEntries==null) { // this is the only example with this priors value
                if (entry[1]>0) positiveSeen++; else negativeSeen++;
                double percentagePositiveSeen=(double)positiveSeen/(double)positivecount;
                double percentageNegativeSeen=(double)negativeSeen/(double)negativecount;
                if (positiveSeen==positivecount && negativeSeen==0) result=1;
                else if (negativeSeen==negativecount && positiveSeen==0) result=-1;
                int index=(int)Math.round(percentageNegativeSeen*100.0); // negative rate
                for (int j=lastindex+1;j<=index;j++) ROCvalues[j]=percentagePositiveSeen; // positive rate corresponding to this negative rate
                lastindex=index;                  
                i++;
            } else { // multiple entries with the same priors value coming after eachother
                //System.err.println("["+featureName+"] Next "+nextEntries[0]+" positive ("+(nextEntries[0]/(double)inside)+") and "+nextEntries[1]+" ("+(nextEntries[1]/(double)outside)+") negative examples have same score="+entry[0]+" seen="+insideSeen+"/"+outsideSeen);
                double oldpercentagePositiveSeen=(double)positiveSeen/(double)positivecount;
                double oldpercentageNegativeSeen=(double)negativeSeen/(double)negativecount;
                positiveSeen+=nextEntries[0]; // this applies to the end of the run
                negativeSeen+=nextEntries[1]; // this applies to the end of the run
                double percentagePositiveSeen=(double)positiveSeen/(double)positivecount;
                double percentageNegativeSeen=(double)negativeSeen/(double)negativecount;
                if (positiveSeen==positivecount && negativeSeen==0) result=1; // all positive seen before all negative
                else if (negativeSeen==negativecount && positiveSeen==0) result=-1; // all negative seen before all positive
                int startIndex=(int)Math.round(oldpercentageNegativeSeen*100.0);
                int endIndex=(int)Math.round(percentageNegativeSeen*100.0);
                if (startIndex==endIndex) ROCvalues[endIndex]=percentagePositiveSeen;
                else {
                    double differenceStep=(percentagePositiveSeen-oldpercentagePositiveSeen)/(double)(endIndex-startIndex);
                    double value=oldpercentagePositiveSeen;
                    for (int j=startIndex;j<=endIndex;j++) {
                       //System.err.println("["+featureName+"]  ROC["+j+"]="+value);
                       ROCvalues[j]=value;
                       value+=differenceStep;
                    }
                }
                i+=(nextEntries[0]+nextEntries[1]);
                lastindex=endIndex;
            }
        }
        double featureAUC=0;
        if (result==1) featureAUC=1;
        else if (result==0) {
            for (i=1;i<ROCvalues.length;i++) {
                featureAUC+=(ROCvalues[i-1]+ROCvalues[i])*0.005f; // this formula sums 'trapezoids'. I.e. a linear interpolation between Y points, where the distance between X's is 0.01
            }
        } // note that featureAUC defaults to 0 if result==-1
        return new Object[]{ROCvalues,featureAUC};
    }

    /** If values coming after 'index' in the the list have same prior score as the 'index' one, the number of such positive and negative instances will be returned as a list [positive, negative] (including current index)
     *  If the subsequent values have different prior scores NULL is returned
     */
    private int[] lookahead(ArrayList<double[]> values, int index) {
        int[] counts=new int[]{0,0};
        int count=1;
        double score=values.get(index)[0];
        while ((index+count)<values.size()) {
            double nextscore=values.get(index+count)[0];
            if (nextscore!=score) break;
            if (values.get(index+count)[1]>0) counts[0]++; else counts[1]++;
            count++;
        }
        if (counts[0]+counts[1]==0) return null; // no similar
        if (values.get(index)[1]>0) counts[0]++; else counts[1]++; // include current also in the count
        return counts;
    }

   /** Creates the ROC graph for a single feature */
    private void saveROCGraphAsImage(File file, double scale) throws IOException {
        int graphheight=250; // height of graph in pixels (just the actual axis-system)
        int graphwidth=250;  // height of graph in pixels (just the actual axis-system)
        int translateX=50;   // the X coordinate for the top of the graph
        int translateY=10;   // the Y coordinate for the top of the graph
        int width=graphwidth+translateX+10; //
        int height=translateY+graphheight+50;
        BufferedImage image=new BufferedImage((int)Math.round(width*scale),(int)Math.round(height*scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.scale(scale, scale);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Graph graph=new Graph(g, 0f, 1f, 0f, 1f, graphwidth, graphheight, translateX, translateY);
        graph.labelpaddingX=0;
        graph.drawAxes(Graph.BOX, Graph.DOTTED, true);
        g.setColor(new Color(180,180,180));
        g.drawLine(graph.getXforValue(0), graph.getYforValue(0), graph.getXforValue(1), graph.getYforValue(1));
        // draw ROC
        double[] xpoints=new double[103]; // 0 and 100 are represented twice ( 0,0,1,2,3,4,...,98,99,100,100) to add additional anchors (0,0) and (1,1) at each end
        for (int i=0;i<=100;i++) xpoints[i+1]=i*0.01;
        xpoints[0]=0;
        xpoints[102]=1;
        g.setColor(Color.RED);
        Stroke defaultStroke=g.getStroke();
        BasicStroke fatStroke = new BasicStroke(2f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        g.setStroke(fatStroke);
        double[] ypoints=new double[103];
        for (int i=0;i<=100;i++) ypoints[i+1]=ROCvalues[i];
        ypoints[0]=0; // anchor points
        ypoints[102]=1;  // anchor points
        graph.drawCurve(xpoints, ypoints);
        g.setStroke(defaultStroke);
        Font standard=g.getFont();
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        graph.drawAlignedString("ROC", translateX+graphwidth/2, translateY+graphheight+36, 0.5f, 0);
        g.setFont(standard);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        // write the image to file
        OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
        ImageIO.write(image, "png", output);
        output.close(); 
        g.dispose();
    }

   /** Creates the Precision-Recall graph for a single feature */
    private void savePrecisionRecallGraphAsImage(File file, double scale) throws IOException {
        int graphheight=250; // height of graph in pixels (just the actual axis-system)
        int graphwidth=250;  // height of graph in pixels (just the actual axis-system)
        int translateX=50;   // the X coordinate for the top of the graph
        int translateY=10;   // the Y coordinate for the top of the graph
        int width=graphwidth+translateX+10; //
        int height=translateY+graphheight+50;
        BufferedImage image=new BufferedImage((int)Math.round(width*scale),(int)Math.round(height*scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.scale(scale, scale);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Graph graph=new Graph(g, 0f, 1f, 0f, 1f, graphwidth, graphheight, translateX, translateY);
        graph.labelpaddingX=0;
        graph.drawAxes(Graph.BOX, Graph.DOTTED, true);
         // draw Precision-recall curve
        int countValid=0;
        for (int i=0;i<=resolution;i++) if (PrecisionRecallvalues[i]>-Double.MAX_VALUE) countValid++;       
        double[] xpoints=new double[countValid];
        double[] ypoints=new double[countValid];
        int index=0;
        for (int i=0;i<=resolution;i++) {
            double PRvalue=PrecisionRecallvalues[i];
            if (PRvalue==-Double.MAX_VALUE) continue; 
            xpoints[index]=(double)i/(double)resolution;
            ypoints[index]=PRvalue;
            index++;
        }
        
        g.setColor(Color.BLUE);
        Stroke defaultStroke=g.getStroke();
        BasicStroke fatStroke = new BasicStroke(2f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        g.setStroke(fatStroke);
        graph.drawCurve(xpoints, ypoints);
        g.setStroke(defaultStroke);
        Font standard=g.getFont();
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        graph.drawAlignedString("Recall", translateX+graphwidth/2, translateY+graphheight+36, 0.5f, 0);
        graph.drawVerticalString("Precision", 14, translateY+graphheight-100, true);
        g.setFont(standard);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        // write the image to file
        OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
        ImageIO.write(image, "png", output);
        output.close(); 
        g.dispose();
    }






   /** Creates the ROC graph for multiple features */
    private void saveMultipleROCGraphAsImage(File file, MotifLabEngine engine, double scale, double[] legendscale) throws IOException {
        DecimalFormat decimalformatter=new DecimalFormat("0.000");
        int graphheight=400; // height of graph in pixels (just the actual axis-system)
        int graphwidth=400;  // height of graph in pixels (just the actual axis-system)
        int translateX=55;   // the X coordinate for the top of the graph
        int translateY=10;   // the Y coordinate for the top of the graph
        int width=graphwidth+translateX+10; //
        int height=translateY+graphheight+50;
        BufferedImage image=new BufferedImage((int)Math.round(width*scale),(int)Math.round(height*scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.scale(scale, scale);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Graph graph=new Graph(g, 0f, 1f, 0f, 1f, graphwidth, graphheight, translateX, translateY);
        graph.drawAxes(Graph.BOX, Graph.DOTTED, true);
        g.setColor(new Color(180,180,180));
        g.drawLine(graph.getXforValue(0), graph.getYforValue(0), graph.getXforValue(1), graph.getYforValue(1));
        // draw ROC
        double[] xpoints=new double[103];
        for (int i=0;i<=100;i++) xpoints[i+1]=i*0.01;
        xpoints[0]=0;
        xpoints[102]=1;    
        Stroke defaultStroke=g.getStroke();
        BasicStroke fatStroke = new BasicStroke(1.8f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

        //BasicStroke fatStroke = new BasicStroke(2f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        g.setStroke(fatStroke);
        ArrayList<String> names=new ArrayList<String>(ROCmap.size());
        for (String s:ROCmap.keySet()) names.add(s);
        Collections.sort(names, new AUCSortComparator());
        VisualizationSettings settings=engine.getClient().getVisualizationSettings();        
        if (featureProperty.equals("type")) { // filter based on visibility if the featureProperty is type
            for (Iterator<String> iterator = names.iterator(); iterator.hasNext();) {
                String type = iterator.next();
                if (!settings.isRegionTypeVisible(type)) iterator.remove();
            }            
        }
        Color useColor=Color.BLACK;
        String[] sortedNames=new String[names.size()];
        Color[] sortedColors=new Color[names.size()];
        int index=0;
        for (int i=0;i<names.size();i++) {
            String featurename=names.get(i);
            if (featureProperty.equals("type")) useColor=settings.getFeatureColor(featurename);
            else useColor=getPaletteColor(i);
            sortedNames[index]=featurename+" = "+decimalformatter.format(AUCmap.get(featurename));
            sortedColors[index]=useColor;
            double[] ROC=ROCmap.get(featurename);
            double[] ypoints=new double[103];
            for (int j=0;j<=100;j++) ypoints[j+1]=ROC[j];
            ypoints[0]=0; // anchor points
            ypoints[102]=1;  // anchor points
            g.setColor(useColor);
            graph.drawCurve(xpoints, ypoints);
            index++;
        }
        g.setColor(Color.BLACK);
        g.setStroke(defaultStroke);
        Font d=g.getFont();
        Font usefont=new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        g.setFont(usefont);
        graph.drawAlignedString("1 - Specificity    ( FP-rate )", translateX+graphwidth/2, translateY+graphheight+42, 0.5f, 0);
        graph.drawVerticalString("Sensitivity    ( TP-rate )", 8, translateY+graphheight-144,true);
        if (legendscale!=null && legendscale[0]>0) {
            Dimension legendDim=Graph.getLegendDimension(sortedNames, usefont);
            legendDim.width=(int)Math.round(legendDim.width*legendscale[0]);
            legendDim.height=(int)Math.round(legendDim.height*legendscale[0]);
            int legendX=(legendscale[1]>=0)?(int)(graph.getXforValue(0)+legendscale[1]):(int)(graph.getXforValue(1.0)+legendscale[1]-legendDim.width);
            int legendY=(legendscale[2]>=0)?(int)(graph.getYforValue(1.0)+legendscale[2]):(int)(graph.getYforValue(0.0)+legendscale[2]-legendDim.height);  
            g.translate(legendX, legendY);         
            g.scale(legendscale[0], legendscale[0]);
            graph.drawLegendBox(sortedNames, sortedColors, 0, 0, true);
        }        
        g.setFont(d);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        // write the image to file
        OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
        ImageIO.write(image, "png", output);
        output.close(); 
        g.dispose();
    }


    private void saveSensitivityGraphAsImage(File file, double scale) throws IOException {
        int graphheight=250; // height of graph in pixels (just the actual axis-system)
        int graphwidth=380; // height of graph in pixels (just the actual axis-system)
        int translateX=50; // the X coordinate for the top of the graph
        int translateY=10; // the Y coordinate for the top of the graph
        int width=graphwidth+translateX+10; //
        int height=translateY+graphheight+30;
        BufferedImage image=new BufferedImage((int)Math.round(width*scale),(int)Math.round(height*scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.scale(scale, scale);
        Stroke defaultStroke=g.getStroke();
        BasicStroke fatStroke = new BasicStroke(2f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        // draw axes and ticks
        g.setColor(Color.BLACK);
        Graph graph=new Graph(g, minScoreValue, maxScoreValue, 0f, 1f, graphwidth, graphheight, translateX, translateY);
        graph.drawAxes(Graph.BOX, Graph.DOTTED, true);
      
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // create an array of x-points
        double[] Xpoints=new double[resolution+1];
        double step=(maxScoreValue-minScoreValue)/(double)resolution;
        for (int i=0;i<=resolution;i++) {
            Xpoints[i]=minScoreValue+(i*step);
        }
        double[] sensitivity=getStatistic("sensitivity");
        double[] specificity=getStatistic("specificity");
        g.setStroke(fatStroke);
        g.setColor(Color.GREEN);
        graph.drawCurve(Xpoints, sensitivity);
        g.setColor(Color.RED);
        graph.drawCurve(Xpoints, specificity);
        g.setColor(Color.YELLOW);
        graph.drawCurve(Xpoints, getStatistic("false positive rate"));
        g.setColor(Color.MAGENTA);
        graph.drawCurve(Xpoints, getStatistic("F-measure"));
        g.setStroke(new BasicStroke(1f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f,3f}, 0f));
        g.setColor(Color.BLUE);
        graph.drawCurve(Xpoints, getStatistic("averageSnSp"));
        g.setColor(Color.LIGHT_GRAY);
        graph.drawCurve(Xpoints, getStatistic("discrimination"));
        g.setStroke(defaultStroke);
        g.setColor(new Color(0,255,0,100));
        for (double[] range:optimalThresholdBestSnSpsum) {
            double minThresholdBestSnSpsum=range[0];
            double maxThresholdBestSnSpsum=range[1];
            int x1=graph.getXforValue(minThresholdBestSnSpsum);
            int x2=graph.getXforValue(maxThresholdBestSnSpsum);
            int y1=graph.getYforValue(1);
            int y2=graph.getYforValue(0);
            int bheight=y2-y1+1;
            int bwidth=x2-x1+1;
            g.fillRect(x1, y1, bwidth, bheight);
        }
    
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        // write the image to file
        OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
        ImageIO.write(image, "png", output);
        output.close(); 
        g.dispose();
    }


    private void saveHistogramGraphAsImage(File file, boolean normalize, boolean drawSnSpThreshold, boolean drawAccuracyThreshold, double scale) throws IOException {
        int graphheight=150; // height of graph in pixels (just the actual axis-system)
        int graphwidth=380; // height of graph in pixels (just the actual axis-system)
        int translateX=50; // the X coordinate for the top of the graph
        int translateY=10; // the Y coordinate for the top of the graph
        int width=graphwidth+translateX+10; //
        int height=translateY+graphheight+30;
        BufferedImage image=new BufferedImage((int)Math.round(width*scale),(int)Math.round(height*scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.scale(scale, scale);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        double[] usepositiveBins=positiveBins;
        double[] usenegativeBins=negativeBins;
        long positivecount=1;
        long negativecount=1;
        if (positiveCount.containsKey(null)) positivecount=positiveCount.get(null);
        if (negativeCount.containsKey(null)) negativecount=negativeCount.get(null);       
        if (normalize) {
        // normalize count bins
            usepositiveBins=new double[positiveBins.length];
            usenegativeBins=new double[negativeBins.length];
            for (int i=0;i<positiveBins.length;i++) {
                usepositiveBins[i]=(double)positiveBins[i]/(double)positivecount;
                usenegativeBins[i]=(double)negativeBins[i]/(double)negativecount;
            }
        }
        // find max in histogram (max bin count)
        double max=0;
        for (int i=0;i<usepositiveBins.length;i++) {
            if(usepositiveBins[i]>max) max=usepositiveBins[i];
            if(usenegativeBins[i]>max) max=usenegativeBins[i];
        }
        // draw axes and ticks
        g.setColor(Color.BLACK);
        Graph graph=new Graph(g, minScoreValue, maxScoreValue, 0f, max, graphwidth, graphheight, translateX, translateY);
        Stroke defaultStroke=g.getStroke();
        g.setStroke(new BasicStroke(1f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f}, 0f));
        g.setColor(Graph.lightGray);
        graph.drawHorizontalGridLines();
        graph.drawVerticalGridLines();
        g.setStroke(defaultStroke);
        g.setColor(Color.BLUE);
        graph.drawHistogram(usenegativeBins);
        g.setColor(Color.RED);
        graph.drawHistogram(usepositiveBins);
        g.setColor(Color.BLACK);
        g.drawRect(translateX, translateY, graphwidth, graphheight); // draw bounding box
        graph.drawXaxisWithTicks(graphheight+translateY, false,false);
        // graph.drawYaxisWithTicks(translateX, false,true);
        //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (drawSnSpThreshold) {
            g.setColor(new Color(0,255,0,100));
            for (double[] range:optimalThresholdBestSnSpsum) {
                double minThresholdBestSnSpsum=range[0];
                double maxThresholdBestSnSpsum=range[1];
                int x1=graph.getXforValue(minThresholdBestSnSpsum);
                int x2=graph.getXforValue(maxThresholdBestSnSpsum);
                int bwidth=x2-x1+1;
                g.fillRect(x1, translateY, bwidth, graphheight);

            }
        }
        if (drawAccuracyThreshold) {
            g.setColor(new Color(100,100,100,100));
            for (double[] range:optimalThresholdBestAccuracy) {
                double minThresholdBestAccuracy=range[0];
                double maxThresholdBestAccuracy=range[1];
                int x1=graph.getXforValue(minThresholdBestAccuracy);
                int x2=graph.getXforValue(maxThresholdBestAccuracy);
                int bwidth=x2-x1+1;
                g.fillRect(x1, translateY, bwidth, graphheight);
            }
        }
        g.setColor(Color.BLACK);
        //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        // write the image to file
        OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
        ImageIO.write(image, "png", output);
        output.close(); 
        g.dispose();

    }

    private void saveOtherStatsGraphAsImage(File file, double scale) throws IOException {
        int graphheight=250; // height of graph in pixels (just the actual axis-system)
        int graphwidth=380; // height of graph in pixels (just the actual axis-system)
        int translateX=50; // the X coordinate for the top of the graph
        int translateY=10; // the Y coordinate for the top of the graph
        int width=graphwidth+translateX+10; //
        int height=translateY+graphheight+30;
        BufferedImage image=new BufferedImage((int)Math.round(width*scale),(int)Math.round(height*scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.scale(scale, scale);
        Stroke defaultStroke=g.getStroke();
        BasicStroke fatStroke = new BasicStroke(2f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        // draw axes and ticks
        g.setColor(Color.BLACK);
        Graph graph=new Graph(g, minScoreValue, maxScoreValue, 0f, 1f, graphwidth, graphheight, translateX, translateY);
        graph.drawAxes(Graph.BOX, Graph.DOTTED, true);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // create an array of x-points
        double[] Xpoints=new double[resolution+1];
        double step=(maxScoreValue-minScoreValue)/(double)resolution;
        for (int i=0;i<=resolution;i++) {
            Xpoints[i]=minScoreValue+(i*step);
        }
        g.setStroke(fatStroke);
        // draw Sensitivity in Green
        g.setColor(Color.BLUE);
        graph.drawCurve(Xpoints, getStatistic("PPV"));
        g.setColor(Color.MAGENTA);
        graph.drawCurve(Xpoints, getStatistic("NPV"));
        g.setColor(Color.LIGHT_GRAY);
        graph.drawCurve(Xpoints, getStatistic("false negative rate"));
        g.setColor(Color.ORANGE);
        graph.drawCurve(Xpoints, getStatistic("false discovery rate"));
        g.setColor(Color.PINK);
        graph.drawCurve(Xpoints, getStatistic("false ommision rate"));
        g.setColor(Color.CYAN);
        graph.drawCurve(Xpoints, getStatistic("performance"));
        g.setColor(Color.BLACK);
        double[] accuracy=getStatistic("accuracy");
        graph.drawCurve(Xpoints, accuracy);
        g.setStroke(defaultStroke);
        g.setColor(new Color(100,100,100,100));
        for (double[] range:optimalThresholdBestAccuracy) {
            double minThresholdBestAccuracy=range[0];
            double maxThresholdBestAccuracy=range[1];
            int x1=graph.getXforValue(minThresholdBestAccuracy);
            int x2=graph.getXforValue(maxThresholdBestAccuracy);
            int y1=graph.getYforValue(1);
            int y2=graph.getYforValue(0);
            int bheight=y2-y1+1;
            int bwidth=x2-x1+1;
            g.fillRect(x1, y1, bwidth, bheight);
        }
        g.setColor(Color.BLACK);
        //graph.drawAlignedString("Threshold", (int)(translateX+graphwidth/2), translateY+graphheight+30, 0.5, 0.5);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        // write the image to file
        OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
        ImageIO.write(image, "png", output);
        output.close(); 
        g.dispose();
    }


    /** Returns the statistic with the given name for the precompiled list of TP, FP, TN and FN for different threshold levels */
    public double[] getStatistic(String statistic) {
        double[] result=new double[TPFPTNFN.length];
        for (int i=0;i<TPFPTNFN.length;i++) {
            double TP=TPFPTNFN[i][0];
            double FP=TPFPTNFN[i][1];
            double TN=TPFPTNFN[i][2];
            double FN=TPFPTNFN[i][3];
            // If no positive predictions or no negative predictions are made, division by zero will occurr for some statistics, resulting in NaN values.
            // This happens at edge-cases, where all bases are predicted as being either positive or negative. (threshold is at minimum or maximum value)            
                 if (statistic.equalsIgnoreCase("sensitivity")) result[i]=TP/(TP+FN);
            else if (statistic.equalsIgnoreCase("specificity")) result[i]=TN/(FP+TN);
            else if (statistic.equalsIgnoreCase("PPV")) result[i]=TP/(TP+FP);
            else if (statistic.equalsIgnoreCase("NPV")) result[i]=TN/(TN+FN);
            else if (statistic.equalsIgnoreCase("false positive rate")) result[i]=1-TN/(FP+TN); // 1-specificity
            else if (statistic.equalsIgnoreCase("false negative rate")) result[i]=1-TP/(TP+FN); // 1-sensitivity
            else if (statistic.equalsIgnoreCase("false discovery rate")) result[i]=1-TP/(TP+FP);
            else if (statistic.equalsIgnoreCase("false ommision rate")) result[i]=1-TN/(TN+FN);
            else if (statistic.equalsIgnoreCase("accuracy")) result[i]=(TP+TN)/(TP+FP+TN+FN);
            else if (statistic.equalsIgnoreCase("performance")) result[i]=TP/(TP+FP+FN);
//          else if (statistic.equalsIgnoreCase("correlation")) result[i]=0; // not used yet
            else if (statistic.equalsIgnoreCase("discrimination")) result[i]=Math.abs(TP/(TP+FN)-(1-TN/(FP+TN))); // distance between sensitivity and FPR
            else if (statistic.equalsIgnoreCase("averageSnSp")) result[i]=((TP/(TP+FN))+(TN/(FP+TN)))/2; // average of Sn and Sp
            else if (statistic.equalsIgnoreCase("F-measure")) result[i]=(2*TP)/(2*TP+FN+FP); // harmonic mean of precision (PPV) and recall (sensitivity)
            if (Double.isNaN(result[i])) result[i]=0; // to avoid NaN values                 
        }
        return result;
    }




    private class SortComparator implements Comparator<double[]> {
        @Override
        public int compare(double[] sample1, double[] sample2) { // the first value [0] is the score value which is used for sorting. The second [1] value is the class (positive(1)/negative(0)) which will not be used here
            if (sample1[0]<sample2[0]) return 1;
            else if (sample1[0]>sample2[0]) return -1;
            else return 0;
        }
    }

    private class AUCSortComparator implements Comparator<String> {
        @Override
        public int compare(String feature1, String feature2) {
            Double value1=AUCmap.get(feature1);
            Double value2=AUCmap.get(feature2);
            return value1.compareTo(value2)*(-1);
        }
    }

    /**
     * Returns the number of TP, FP, TN and FN when using the given threshold to separate POSITIVE predictions (>=threshold) from NEGATIVE predictions
     * @param values
     * @param threshold
     * @param aboveorequal If true, values 'above or equal' to the threshold are considered POSITIVE, else only those strictly above are considered positive
     * @return
     */
    private long[] countTPFPTNFN(ArrayList<double[]> values, double threshold, boolean aboveorequal) {
        long[] results=new long[]{0,0,0,0};
        for (double[] v:values) {
            boolean positive=(aboveorequal)?(v[0]>=threshold):(v[0]>threshold);
            if (positive) results[(v[1]==1)?0:1]++; // predicted POSITIVE: increase TP if actually true else FP
            else results[(v[1]==0)?2:3]++; // predicted NEGATIVE: increase TN if actually true else FN
        }
        return results;
    }


    private Color getPaletteColor(int i) {
          switch(i%10) {
              case 0: return Color.RED;
              case 1: return Color.GREEN;
              case 2: return Color.BLUE;
              case 3: return Color.YELLOW;
              case 4: return Color.BLACK;
              case 5: return Color.MAGENTA;
              case 6: return Color.CYAN;
              case 7: return Color.ORANGE;
              case 8: return Color.LIGHT_GRAY;
              case 9: return Color.PINK;
              default:return Color.BLACK;
          }

    }


    private String getEvaluationOfAUC(double auc) {
             if (auc==1) return "perfect";
        else if (auc>0.95) return "near perfect";
        else if (auc>0.9) return "excellent";
        else if (auc>0.85) return "very good";
        else if (auc>0.80) return "good";
        else if (auc>0.75) return "OK";
        else if (auc>0.70) return "OK";
        else if (auc>0.65) return "fair";
        else if (auc>0.60) return "not very good";
        else if (auc>0.55) return "bad";
        else if (auc<=0.55 && auc>=0.45) return "close to random guessing";
        else  return "very bad (it would have been better if the prior was inverted!)";
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
