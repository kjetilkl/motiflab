/*
 
 
 */

package motiflab.engine.data.analysis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import javax.swing.JTable;
import motiflab.engine.data.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.apache.commons.math3.distribution.BinomialDistribution;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.dataformat.DataFormat;
import motiflab.gui.GenericMotifBrowserPanel;
import motiflab.gui.MotifLogo;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.VisualizationSettings;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;

/**
 *
 * @author kjetikl
 */
public class MotifOccurrenceAnalysis extends Analysis {
    private final static String typedescription="Analysis: count motif occurrences";
    private final static String analysisName="count motif occurrences";
    private final static String description="Counts the number of occurrences of each motif in each sequence and the total number";
    private static final String SORT_BY_MOTIF="Motif ID";
    private static final String SORT_BY_TOTAL_OCCURRENCES="Total occurrences";
    private static final String SORT_BY_SEQUENCE_OCCURRENCES="Sequence occurrences";
    private static final String SORT_BY_P_VALUE="p-value";
    private static final String OUTPUT_ALL="All motifs";
    private static final String OUTPUT_PRESENT="Only present motifs";    
    private static final String OUTPUT_SIGNIFICANT="Only significant motifs";    
    private HashMap<String,double[]> counts=null; // key is motif name. Value is double[]{sequence support, total count, maximum number of occurrences possible for this motif in the sequence collection, expected frequency, p-value}
    private int sequenceCollectionSize=0;
    private String backgroundFrequenciesName=null;
    private double significanceThreshold=0.05;
    private int multiplehypothesis=1; // number of motifs counted in bonferroni correction
    private String bonferroniStrategy=ALL_MOTIFS;
    private String motifCollectionName=null;
    private String motifTrackName=null;
    private String sequenceCollectionName=null;
    private String withinRegionsTrackName=null;    
    
    private static final String NONE="None";
    private static final String ALL_MOTIFS="All motifs";
    private static final String PRESENT_MOTIFS="Present motifs";

    private static final int COLOR=0;    
    private static final int ID=1;
    private static final int NAME=2;
    private static final int MOTIF_CLASS=3;    
    private static final int SEQUENCE_SUPPORT=4;
    private static final int TOTAL=5;
    private static final int LOGO_OR_EXPECTED=6;
    private static final int PVALUE=7;
    private static final int LOGO=8;
    
    public MotifOccurrenceAnalysis() {
        this.name="MotifOccurrenceAnalysis_temp";
        addParameter("Motif track",RegionDataset.class, null,new Class[]{RegionDataset.class},"A region track containing motif sites",true,false);
        addParameter("Motifs",MotifCollection.class, null,new Class[]{MotifCollection.class},"The motifs to consider in this analysis",true,false);
        addParameter("Sequences",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"If specified, the analysis will be limited to sequences in this collection",false,false);
        addParameter("Within regions",RegionDataset.class, null,new Class[]{RegionDataset.class},"Limits the analysis to motifs found within the selected regions and not the full sequences",false,false); 
        addParameter("Background frequencies",MotifNumericMap.class,null, new Class[]{MotifNumericMap.class},"A numeric map containing expected frequencies for each motif. If provided, this will be used to estimate overrepresentation p-values for each motif.",false,false);
        addParameter("Significance threshold",Double.class, new Double(0.05),new Double[]{0.0,1.0},"The initial p-value threshold to use when estimating significance",true,false);
        addParameter("Bonferroni correction",String.class, ALL_MOTIFS,new String[]{NONE,PRESENT_MOTIFS,ALL_MOTIFS},"If selected, the initial p-value threshold will be adjusted according to the given number of motifs",true,false);
        addParameter("Motif counts",MotifNumericMap.class,null, new Class[]{MotifNumericMap.class},"If provided, the total number of occurrences for each motif will be taken directly from this map rather than being derived from the motif track.",false,false, true);
        addParameter("Background counts",MotifNumericMap.class,null, new Class[]{MotifNumericMap.class},"If provided, the total number of expected occurrences for each motif will be taken directly from this map rather than being derived from the background frequencies.",false,false, true);
        addParameter("Sequence counts",MotifNumericMap.class,null, new Class[]{MotifNumericMap.class},"If provided, the sequence support for each motif will be taken directly from this map rather than being derived from the motif track.",false,false, true);
    }

    @Override
    public String getAnalysisName() {
        return analysisName;
    }

    @Override
    public String getDescription() {return description;}
    
    
    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Motif track","Motifs"};}
    
    @Override
    public boolean canUseAsSourceProxy(Data data) {
        if (data instanceof RegionDataset) return ((RegionDataset)data).isMotifTrack(); // only allow Motif Tracks as input
        else return (data instanceof MotifCollection);
    }       
    
    @Override
    public Parameter[] getOutputParameters() {
        return new Parameter[] {
             new Parameter("Include",MotifCollection.class,null,new Class[]{MotifCollection.class},"Only include data from this collection",false,false),
             new Parameter("Sort by",String.class,SORT_BY_SEQUENCE_OCCURRENCES, new String[]{SORT_BY_MOTIF,SORT_BY_SEQUENCE_OCCURRENCES,SORT_BY_TOTAL_OCCURRENCES, SORT_BY_P_VALUE},"Sorting order for the results table",false,false),
             new Parameter("Logos",String.class,MOTIF_LOGO_NO, new String[]{MOTIF_LOGO_NO,MOTIF_LOGO_NEW,MOTIF_LOGO_SHARED,MOTIF_LOGO_TEXT},"Include sequence logos in the table",false,false),
             new Parameter("Color boxes",Boolean.class,Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a box with the assigned color for the motif will be output as the first column",false,false),       
             new Parameter("Legend",Boolean.class,Boolean.TRUE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a header with a title and analysis details will be included at the top of the Excel sheet.",false,false)       
        };
    }
    
    @Override
    public String[] getOutputParameterFilter(String parameter) {
        if (parameter.equals("Color boxes")) return new String[]{HTML};
        if (parameter.equals("Legend")) return new String[]{EXCEL};
        if (parameter.equals("Include") || parameter.equals("Sort by") || parameter.equals("Logos")) return new String[]{HTML,RAWDATA,EXCEL};        
        return null;
    }      

    @Override
    public String[] getResultVariables() {
        if (backgroundFrequenciesName!=null) return new String[]{"support","total","expected","p-value","corrected threshold","overrepresented","present"};
        else return new String[]{"support","total","present"};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename==null || variablename.isEmpty()) throw new ExecutionError("'"+getName()+"' does not have a result for ''");
        else if (variablename.equals("support")) {
            MotifNumericMap map=new MotifNumericMap("temp",0);
            for (String motifname:counts.keySet()) {
                double[] stats=counts.get(motifname);
                map.setValue(motifname, stats[0]);
            }
            return map;
        }
        else if (variablename.equals("total")) {
            MotifNumericMap map=new MotifNumericMap("temp",0);
            for (String motifname:counts.keySet()) {
                double[] stats=counts.get(motifname);
                map.setValue(motifname, stats[1]);
            }
            return map;
        }      
        else if (backgroundFrequenciesName!=null && variablename.equals("expected")) {
            MotifNumericMap map=new MotifNumericMap("temp",0);
            for (String motifname:counts.keySet()) {
                double[] stats=counts.get(motifname);
                double expectedCount=(double)(stats[3]*stats[2]); // expected frequency * number of possibilities
                map.setValue(motifname, expectedCount);
            }
            return map;
        }
        else if (backgroundFrequenciesName!=null && variablename.equals("p-value")) {
            MotifNumericMap map=new MotifNumericMap("temp",0);
            for (String motifname:counts.keySet()) {
                double[] stats=counts.get(motifname);
                map.setValue(motifname, stats[4]);
            }
            return map;
        }
        else if (backgroundFrequenciesName!=null && variablename.equals("overrepresented")) {
            MotifCollection collection=new MotifCollection("temp");
            double correctedThreshold=significanceThreshold / (double) multiplehypothesis;            
            for (String motifname:counts.keySet()) {
                    double[] c=counts.get(motifname);
                    double pvalueOverrep=c[4];
                    if (pvalueOverrep<=correctedThreshold) {
                        Data motif=engine.getDataItem(motifname);
                        if (motif instanceof Motif) collection.addMotif((Motif)motif);
                    }
            }
            return collection;
        }
        else if (variablename.equals("present")) {
            MotifCollection collection=new MotifCollection("temp");         
            for (String motifname:counts.keySet()) {
                    double[] c=counts.get(motifname);
                    double occurrences=c[1];
                    if (occurrences>0) {
                        Data motif=engine.getDataItem(motifname);
                        if (motif instanceof Motif) collection.addMotif((Motif)motif);
                    }
            }
            return collection;
        }          
        else if (backgroundFrequenciesName!=null && variablename.equals("corrected threshold")) return new NumericVariable("result", significanceThreshold/(double)multiplehypothesis);
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else if (variablename.equals("corrected threshold")) return NumericVariable.class;
       else if (variablename.equals("overrepresented") || variablename.equals("present")) return MotifCollection.class;
       else return MotifNumericMap.class;
    }

    @Override
    public String[] getColumnsExportedForCollation() {
        if (backgroundFrequenciesName!=null) return new String[]{"support","total","expected","p-value"};
        else return new String[]{"support","total"};
    }
    
    @Override
    public Class getCollateType() {
        return Motif.class;
    }

    @Override
    public HashMap<String,Object> getColumnData(String column) throws ExecutionError {
        Class coltype=getColumnType(column);
        if (coltype==null) throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
        HashMap<String,Object> columnData=new HashMap<String, Object>();
        for (String motifname:counts.keySet()) {
            double[] stat=counts.get(motifname);
            if (column.equalsIgnoreCase("support")) columnData.put(motifname, new Integer((int)stat[0]));
            else if (column.equalsIgnoreCase("total")) columnData.put(motifname, new Integer((int)stat[1]));
            else if (column.equalsIgnoreCase("expected") && backgroundFrequenciesName!=null) columnData.put(motifname, new Double(stat[2]*stat[3]));
            else if (column.equalsIgnoreCase("p-value") && backgroundFrequenciesName!=null) {
                double totalcount=stat[1];
                double maxcount=stat[2];
                double expectedFrequency=stat[3];                
                double observedFrequency=totalcount/maxcount;
                double correctedThreshold=significanceThreshold/(double)multiplehypothesis;
                double pvalueOverrep=stat[4];
                String significanceClass=null;
                if (observedFrequency>expectedFrequency && expectedFrequency==0) {significanceClass="verysignificant";}
                else if (pvalueOverrep<=correctedThreshold) {significanceClass="significant";}                
                columnData.put(motifname, new AnnotatedValue(new Double(pvalueOverrep),significanceClass));               
            }
        }
        return columnData;
    }
    
    @Override
    public Class getColumnType(String column) {
             if (column.equalsIgnoreCase("support")) return Integer.class;
        else if (column.equalsIgnoreCase("total")) return Integer.class;
        else if (column.equalsIgnoreCase("expected") && backgroundFrequenciesName!=null) return Double.class;
        else if (column.equalsIgnoreCase("p-value") && backgroundFrequenciesName!=null) return Double.class;
        else return null;
    }      
    
    
    @Override
    @SuppressWarnings("unchecked")
    public MotifOccurrenceAnalysis clone() {
        MotifOccurrenceAnalysis newanalysis=new MotifOccurrenceAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.counts=(HashMap<String,double[]>)this.counts.clone();
        newanalysis.backgroundFrequenciesName=this.backgroundFrequenciesName;
        newanalysis.sequenceCollectionSize=this.sequenceCollectionSize;
        newanalysis.bonferroniStrategy=this.bonferroniStrategy;
        newanalysis.significanceThreshold=this.significanceThreshold;
        newanalysis.multiplehypothesis=this.multiplehypothesis;
        newanalysis.motifCollectionName=this.motifCollectionName;
        newanalysis.motifTrackName=this.motifTrackName;
        newanalysis.sequenceCollectionName=this.sequenceCollectionName;
        newanalysis.withinRegionsTrackName=this.withinRegionsTrackName;        
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        this.counts=(HashMap<String,double[]>)((MotifOccurrenceAnalysis)source).counts;
        this.sequenceCollectionSize=((MotifOccurrenceAnalysis)source).sequenceCollectionSize;
        this.backgroundFrequenciesName=((MotifOccurrenceAnalysis)source).backgroundFrequenciesName;
        this.bonferroniStrategy=((MotifOccurrenceAnalysis)source).bonferroniStrategy;
        this.significanceThreshold=((MotifOccurrenceAnalysis)source).significanceThreshold;
        this.multiplehypothesis=((MotifOccurrenceAnalysis)source).multiplehypothesis;
        this.motifCollectionName=((MotifOccurrenceAnalysis)source).motifCollectionName;
        this.motifTrackName=((MotifOccurrenceAnalysis)source).motifTrackName;
        this.sequenceCollectionName=((MotifOccurrenceAnalysis)source).sequenceCollectionName;
        this.withinRegionsTrackName=((MotifOccurrenceAnalysis)source).withinRegionsTrackName;      
    }

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

    /** Constructs a sorted list with the motif name, total occurrence count and sequence count
     *  This method is contains code common for both formatHTML and formatRaw
     */
    private ArrayList<Object[]> assembleList(String sortorder, MotifCollection include, MotifLabEngine engine) {
        ArrayList<Object[]> resultList=new ArrayList<Object[]>(counts.size());
        Set<String> keys=counts.keySet();
        Iterator<String> iterator=keys.iterator();
        int i=0;
        double correctedThreshold=significanceThreshold/(double)multiplehypothesis;        
        while (iterator.hasNext()) {
            i++;
            String motifkey=iterator.next();
            if (include!=null && !include.contains(motifkey)) continue;
            double[] values=counts.get(motifkey);
            int sequencesupport=(int)values[0];            
            int totalcount=(int)values[1];
            int maxcount=(int)values[2];            
            double pvalueOverrep=(double)values[4]; // assign to totalcount first
            double expectedFrequency=(double)values[3];          
            resultList.add(new Object[]{motifkey,new Integer(sequencesupport), new Integer(totalcount), new Integer(maxcount), new Double(pvalueOverrep), new Double(expectedFrequency)});
        }
        Collections.sort(resultList, new SortOrderComparator(sortorder));
        return resultList;
    }

    @Override
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder=SORT_BY_MOTIF;
        MotifCollection include=null;
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        Color [] basecolors=vizSettings.getBaseColors();
        MotifLogo sequencelogo=new MotifLogo(basecolors,sequencelogoSize);
        String showSequenceLogosString=MOTIF_LOGO_NO;
        boolean showColorBoxes=false;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             include=(MotifCollection)settings.getResolvedParameter("Include",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
             showColorBoxes=(Boolean)settings.getResolvedParameter("Color boxes",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos=(showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_NEW) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_SHARED) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_TEXT));
        ArrayList<Object[]> resultList=assembleList(sortorder,include,engine);
        engine.createHTMLheader("Motif Occurrence Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<h1 class=\"headline\">Motif Occurrence Analysis</h1>\n",HTML);
        outputobject.append("<div class=\"summary\">\n",HTML);
        outputobject.append("Analysis performed with motifs from <span class=\"dataitem\">",HTML);
        outputobject.append(motifCollectionName,HTML);
        outputobject.append("</span> and sites from <span class=\"dataitem\">",HTML);
        outputobject.append(motifTrackName,HTML);
        outputobject.append("</span>",HTML);
        if (withinRegionsTrackName!=null) {            
            outputobject.append(" within <span class=\"dataitem\">",HTML);
            outputobject.append(withinRegionsTrackName,HTML);
            outputobject.append("</span> regions",HTML);
        }
        outputobject.append(" on "+sequenceCollectionSize,HTML);
        outputobject.append(" sequence"+((sequenceCollectionSize!=1)?"s":""),HTML);
        if (sequenceCollectionName!=null) {
            outputobject.append(" from collection <span class=\"dataitem\">",HTML);
            outputobject.append(sequenceCollectionName,HTML);
            outputobject.append("</span>",HTML);
        }
        outputobject.append(".",HTML);
        if (backgroundFrequenciesName!=null) {
            outputobject.append(" Expected motif frequencies from <span class=\"dataitem\">",HTML);
            outputobject.append(backgroundFrequenciesName,HTML);
            outputobject.append("</span>.",HTML);
            outputobject.append("<br>Statistical significance evaluated using a binomial test with p-value threshold="+significanceThreshold,HTML);
            if (bonferroniStrategy.equals(ALL_MOTIFS)) outputobject.append(" (Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering all "+multiplehypothesis+" motifs tested)",HTML);
            if (bonferroniStrategy.equals(PRESENT_MOTIFS)) outputobject.append(" (Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering "+multiplehypothesis+" present motifs)",HTML);
        }      
        outputobject.append("\n</div>\n",HTML);
        outputobject.append("<br>\n",HTML);
        outputobject.append("<table class=\"sortable\">\n",HTML);
        outputobject.append("<tr>",HTML);
        if (showColorBoxes) outputobject.append("<th>&nbsp;</th>",HTML);               
        outputobject.append("<th>ID</th>",HTML);
        outputobject.append("<th>Name</th>",HTML);
        outputobject.append("<th class=\"sorttable_ip\">Class</th>",HTML);
        outputobject.append("<th>Total</th>",HTML);
        outputobject.append("<th>Sequences</th>",HTML);
        outputobject.append("<th class=\"sorttable_numeric\">%</th>",HTML);
        if (backgroundFrequenciesName!=null) outputobject.append("<th class=\"sorttable_numeric\">Expected</th><th class=\"sorttable_numeric\">p-value</th>",HTML);
        if (showSequenceLogos) outputobject.append("<th class=\"sorttable_nosort\">Logo</th>",HTML);
        outputobject.append("</tr>\n",HTML);
        double correctedThreshold=significanceThreshold/(double)multiplehypothesis;
        DecimalFormat decimalformatter=DataFormat.getDecimalFormatter(3);
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[0];
            int seqcount=(Integer)entry[1];
            int totalcount=(Integer)entry[2];
            int maxcount=(Integer)entry[3];
            double pvalueOverrep=(Double)entry[4];
            double expectedFrequency=(Double)entry[5];
            Motif motif=null;
            if (engine.dataExists(motifname, Motif.class)) motif=(Motif)engine.getDataItem(motifname);
            String motifpresentationname=(motif!=null)?motif.getShortName():motifname;
            String motifclass=(motif!=null)?motif.getClassification():null;
            String motifclassname=null;
            if (motifclass==null) motifclass="unknown";
            else motifclassname=MotifClassification.getNameForClass(motifclass);
            if (motifclassname!=null) {
               motifclassname=escapeHTML(motifclassname);
               motifclassname=motifclassname.replace("\"", "&#34;");// escape quotes
            } 
            outputobject.append("<tr>",HTML);
            if (showColorBoxes) {
                Color color=Color.WHITE;               
                if (motif!=null) color=vizSettings.getFeatureColor(motifname);             
                String colorString=VisualizationSettings.convertColorToHTMLrepresentation(color);
                outputobject.append("<td><div style=\"width:10px;height:10px;border:1px solid #000;background-color:"+colorString+";\"></div></td>",HTML);
            }             
            outputobject.append("<td>"+escapeHTML(motifname)+"</td>",HTML);
            outputobject.append("<td>"+escapeHTML(motifpresentationname)+"</td>",HTML);
            outputobject.append("<td"+((motifclassname!=null)?(" title=\""+motifclassname+"\""):"")+">"+escapeHTML(motifclass)+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+totalcount+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+seqcount+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+(int)((double)seqcount*100f/(double)sequenceCollectionSize)+"%</td>",HTML);
            if (motif!=null && backgroundFrequenciesName!=null) {
                String observedVSexpectedColumnClass="";
                double observedFrequency=(double)totalcount/(double)maxcount;
                double expectedCount=(double)(expectedFrequency*maxcount);
                if (observedFrequency>expectedFrequency && expectedFrequency==0) observedVSexpectedColumnClass="verysignificant";
                else if (pvalueOverrep<=correctedThreshold) observedVSexpectedColumnClass="significant"; 
                else observedVSexpectedColumnClass="num"; // normal number. Just to get alignment right 
                outputobject.append("<td class=\"num\">",HTML);
                outputobject.append(decimalformatter.format(expectedCount),HTML);
                outputobject.append("</td>",HTML);
                outputobject.append("<td class=\"",HTML);
                outputobject.append(observedVSexpectedColumnClass,HTML);
                outputobject.append("\">",HTML);
                outputobject.append(pvalueOverrep+"</td>",HTML);
            }
            if (showSequenceLogos) {
              if (motif instanceof Motif) {
                 sequencelogo.setMotif((Motif)motif);
                 outputobject.append("<td title=\"",HTML);
                 outputobject.append(sequencelogo.getMotifInfoTooltip(),HTML);
                 outputobject.append("\">",HTML);
                 outputobject.append(getMotifLogoTag((Motif)motif, outputobject, sequencelogo, showSequenceLogosString, engine),HTML);
                 outputobject.append("</td>",HTML);
              } else outputobject.append("<td>?</td>",HTML);
            }
            outputobject.append("</tr>\n",HTML);
            if (i%30==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                Thread.yield();
            }
            task.setStatusMessage("Executing operation: output ("+(i+1)+"/"+resultList.size()+")");
            format.setProgress(i, resultList.size()); //
        }
        outputobject.append("</table>\n</body>\n</html>\n",HTML);
        format.setProgress(100);
        return outputobject;
    }

    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder=SORT_BY_MOTIF;
        MotifCollection include=null;
        String showSequenceLogosString=MOTIF_LOGO_NO;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             include=(MotifCollection)settings.getResolvedParameter("Include",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos=(showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_NEW) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_SHARED) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_TEXT));      
        ArrayList<Object[]> resultList=assembleList(sortorder,include,engine);
        outputobject.append("# Motif occurrence analysis with motifs from '"+motifCollectionName+"' and sites from '"+motifTrackName+"'",RAWDATA);
        if (withinRegionsTrackName!=null) {            
            outputobject.append(" within '",RAWDATA);
            outputobject.append(withinRegionsTrackName,RAWDATA);
            outputobject.append("' regions",RAWDATA);
        }        
        outputobject.append(" on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),RAWDATA);
        if (sequenceCollectionName!=null) outputobject.append(" from collection '"+sequenceCollectionName+"'",RAWDATA);
        if (backgroundFrequenciesName!=null) {
            outputobject.append("\n# Expected motif frequencies from '"+backgroundFrequenciesName+"'",RAWDATA);
            outputobject.append("\n# Statistical significance evaluated using a binomial test with p-value threshold="+significanceThreshold,RAWDATA);
            if (bonferroniStrategy.equals(ALL_MOTIFS)) outputobject.append("\n# (Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering all "+multiplehypothesis+" motifs tested)",RAWDATA);
            if (bonferroniStrategy.equals(PRESENT_MOTIFS)) outputobject.append("\n# (Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering "+multiplehypothesis+" present motifs)",RAWDATA);
        } 

        outputobject.append("\n\n#Motif ID, total occurrences, number of sequences containing motif, total number of sequences, percentage of sequences containing motif",RAWDATA);
        if (backgroundFrequenciesName!=null) {
            outputobject.append(", expected number of motif occurrences, overrepresentation p-value",RAWDATA);
        }
        if (showSequenceLogos) outputobject.append(", motif consensus",RAWDATA);
        outputobject.append("\n",RAWDATA);
        DecimalFormat decimalformatter=DataFormat.getDecimalFormatter(3);
        double correctedThreshold=significanceThreshold/(double)multiplehypothesis;        
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[0];
            int seqcount=(Integer)entry[1];
            int totalcount=(Integer)entry[2];
            int maxcount=(Integer)entry[3];
            double pvalueOverrep=(Double)entry[4];
            double expectedFrequency=(Double)entry[5];
            Motif motif=null;
            if (engine.dataExists(motifname, Motif.class)) motif=(Motif)engine.getDataItem(motifname);            
            outputobject.append(motifname+"\t"+totalcount+"\t"+seqcount+"\t"+sequenceCollectionSize+"\t"+(int)((double)seqcount*100/(double)sequenceCollectionSize)+"%",RAWDATA);
            String stars=null; 
            if (backgroundFrequenciesName!=null) {
                double observedFrequency=(double)totalcount/(double)maxcount;                
                double expectedCount=(double)(expectedFrequency*maxcount);
                outputobject.append("\t",RAWDATA);
                if (motif!=null) outputobject.append(decimalformatter.format(expectedCount),RAWDATA);
                else outputobject.append("N/A",RAWDATA);
                outputobject.append("\t",RAWDATA);
                if (motif!=null) outputobject.append(""+pvalueOverrep,RAWDATA);
                else outputobject.append("N/A",RAWDATA);
                if (observedFrequency>expectedFrequency && expectedFrequency==0) stars="**";
                else if (pvalueOverrep<=correctedThreshold) stars="*";           
            }    
            if (showSequenceLogos) {
                if (motif!=null) outputobject.append("\t"+motif.getConsensusMotif(),RAWDATA);
                else outputobject.append("\t?",RAWDATA);
            }
            if (stars!=null) { // stars that mark significant motifs
                outputobject.append("\t"+stars,RAWDATA);                 
            }
            outputobject.append("\n",RAWDATA);
            if (i%100==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                task.setStatusMessage("Executing operation: output ("+i+"/"+resultList.size()+")");
                format.setProgress(i, resultList.size()); // 
                Thread.yield();
            }
        }
        format.setProgress(100);
        return outputobject;
    }

    
    @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder=SORT_BY_MOTIF;
        MotifCollection include=null;
        String showSequenceLogosString=MOTIF_LOGO_NO;
        boolean includeLegend=true;
        int logoheight=19;
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        Color [] basecolors=vizSettings.getBaseColors();
        boolean border=(Boolean)vizSettings.getSettingAsType("motif.border", Boolean.TRUE);        
        MotifLogo sequencelogo=new MotifLogo(basecolors,sequencelogoSize);        
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             include=(MotifCollection)settings.getResolvedParameter("Include",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);             
             includeLegend=(Boolean)settings.getResolvedParameter("Legend",defaults,engine);
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showLogosAsImages=(showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_NEW) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_SHARED));           
        boolean showSequenceLogos=(showLogosAsImages || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_TEXT));      
        ArrayList<Object[]> resultList=assembleList(sortorder,include,engine);
        int rownum=0;
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(outputobject.getName());
        CreationHelper helper = (showLogosAsImages)?workbook.getCreationHelper():null;
        Drawing drawing = (showLogosAsImages)?sheet.createDrawingPatriarch():null;       
        
        CellStyle title=createExcelStyle(workbook, HSSFCellStyle.BORDER_NONE, (short)0, HSSFCellStyle.ALIGN_LEFT, false);      
        addFontToExcelCellStyle(workbook, title, null, (short)(workbook.getFontAt((short)0).getFontHeightInPoints()*2.5), true, false);
        CellStyle tableheader=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.LIGHT_YELLOW.index, HSSFCellStyle.ALIGN_CENTER, true);      
        CellStyle significant=createExcelStyle(workbook, HSSFCellStyle.BORDER_NONE, HSSFColor.CORAL.index, HSSFCellStyle.ALIGN_RIGHT, false);      
        CellStyle verysignificant=createExcelStyle(workbook, HSSFCellStyle.BORDER_NONE, HSSFColor.RED.index, HSSFCellStyle.ALIGN_RIGHT, false);      
        
        // Make room for the header which will be added later

        Row row = null;
        int headerrows=(backgroundFrequenciesName!=null)?6:5;
        if (includeLegend) {
            for (int j=0;j<headerrows;j++) {
               row = sheet.createRow(j); 
            }
            rownum=headerrows-1; // -1 because it will be incremented later on...
        }        
        int col=0;
        int logocolumn=0;
        row = sheet.createRow(rownum);
        outputStringValuesInCells(row, new String[]{"Motif ID","Name","Class","Total","Sequences"}, 0, tableheader);      
        col+=5;
        if (backgroundFrequenciesName!=null) {
            outputStringValuesInCells(row, new String[]{"Expected","p-value"}, col, tableheader);
            col+=2;
        }
        if (showSequenceLogos) {
            logocolumn=col;
            outputStringValuesInCells(row, new String[]{"Logo"}, logocolumn, tableheader);
            sheet.setColumnWidth(logocolumn, 10000);
        }
        double correctedThreshold=significanceThreshold/(double)multiplehypothesis;    
        int maxlogowidth=0; // the number of bases in the longest motif     
        if (showLogosAsImages) sheet.setColumnWidth(logocolumn, 10000);
        for (int i=0;i<resultList.size();i++) {
            rownum++;
            row = sheet.createRow(rownum);
            col=0;
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[0];
            String shortname="";
            String motifclass="";
            int seqcount=(Integer)entry[1];
            int totalcount=(Integer)entry[2];
            int maxcount=(Integer)entry[3];
            double pvalueOverrep=(Double)entry[4];
            double expectedFrequency=(Double)entry[5];
            Motif motif=null;
            if (engine.dataExists(motifname, Motif.class)) {
                motif=(Motif)engine.getDataItem(motifname);
                motifclass=motif.getClassification();
                if (motifclass==null) motifclass="unknown";  
                shortname=motif.getShortName();
            }            
            outputStringValuesInCells(row, new String[]{motifname,shortname,motifclass}, col);
            col+=3;
            outputNumericValuesInCells(row, new double[]{totalcount,seqcount}, col);
            col+=2;
            if (backgroundFrequenciesName!=null && motif!=null) {
                double observedFrequency=(double)totalcount/(double)maxcount;                
                double expectedCount=(double)(expectedFrequency*maxcount);
                outputNumericValuesInCells(row, new double[]{expectedCount}, col++);                            
                if (observedFrequency>expectedFrequency && expectedFrequency==0) outputNumericValuesInCells(row, new double[]{pvalueOverrep}, col++, verysignificant);
                else if (pvalueOverrep<=correctedThreshold) outputNumericValuesInCells(row, new double[]{pvalueOverrep}, col++, significant);  
                else outputNumericValuesInCells(row, new double[]{pvalueOverrep}, col++);
            }    
            if (showSequenceLogos && motif!=null) {
                if (showLogosAsImages) {
                    try {
                        row.setHeightInPoints((short)(sheet.getDefaultRowHeightInPoints()*1.2));                        
                        sequencelogo.setMotif(motif);
                        int width=motif.getLength();
                        if (width>maxlogowidth) maxlogowidth=width;
                        byte[] image=getMotifLogoImageAsByteArray(sequencelogo, logoheight, border, "png");
                        int imageIndex=workbook.addPicture(image, HSSFWorkbook.PICTURE_TYPE_PNG);
                        ClientAnchor anchor = helper.createClientAnchor();
                        anchor.setCol1(logocolumn);
                        anchor.setRow1(rownum);
                        anchor.setAnchorType(ClientAnchor.MOVE_DONT_RESIZE);
                        Picture pict=drawing.createPicture(anchor, imageIndex);	
                        pict.resize();
                    } catch (Exception e) {e.printStackTrace(System.err);}
                }
                else outputStringValuesInCells(row, new String[]{motif.getConsensusMotif()}, logocolumn);
            }
            if (i%10==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                task.setStatusMessage("Executing operation: output ("+i+"/"+resultList.size()+")");
                format.setProgress(i, resultList.size()); // 
                if (i%100==0) Thread.yield();
            }
        }
        format.setProgress(95);  
        for (short i=0;i<col;i++) {
            sheet.autoSizeColumn(i);               
        }
        if (!showLogosAsImages) sheet.autoSizeColumn((short)logocolumn);   
             
        // Add the header on top of the page
        if (includeLegend) {        
            sheet.createFreezePane(0,headerrows,0,headerrows);
            row=sheet.getRow(0);
            outputStringValueInCell(row, 0, "Motif Occurrence Analysis", title);
            StringBuilder firstLine=new StringBuilder();
            firstLine.append("Analysis performed with motifs from \"");
            firstLine.append(motifCollectionName);
            firstLine.append("\" and sites from \"");
            firstLine.append(motifTrackName);
            firstLine.append("\"");
            if (withinRegionsTrackName!=null) {            
                firstLine.append(" within \">");
                firstLine.append(withinRegionsTrackName);
                firstLine.append("\"");
            }
            firstLine.append(" on "+sequenceCollectionSize);
            firstLine.append(" sequence"+((sequenceCollectionSize!=1)?"s":""));
            if (sequenceCollectionName!=null) {
                firstLine.append(" from collection \"");
                firstLine.append(sequenceCollectionName);
                firstLine.append("\"");
            }
            firstLine.append(".");
            if (backgroundFrequenciesName!=null) {
                firstLine.append(" Expected motif frequencies from \"");
                firstLine.append(backgroundFrequenciesName);
                firstLine.append("\".");      
            }
            row=sheet.getRow(2);
            outputStringValueInCell(row, 0, firstLine.toString(), null);          
            if (backgroundFrequenciesName!=null) {
                StringBuilder secondLine=new StringBuilder();
                secondLine.append("Statistical significance evaluated using a binomial test with p-value threshold=");
                secondLine.append(significanceThreshold);
                if (bonferroniStrategy.equals(ALL_MOTIFS)) secondLine.append(" (Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering all "+multiplehypothesis+" motifs tested)");
                if (bonferroniStrategy.equals(PRESENT_MOTIFS)) secondLine.append(" (Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering "+multiplehypothesis+" present motifs)");
                row=sheet.getRow(3);
                outputStringValueInCell(row, 0, secondLine.toString(), null);  
            }                                
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
        RegionDataset source=(RegionDataset)task.getParameter("Motif track");
        if (!source.isMotifTrack()) throw new ExecutionError("Motif occurrence analysis can only be performed on motif tracks");
        RegionDataset withinRegions=(RegionDataset)task.getParameter("Within regions");        
        motifTrackName=source.getName();
        if (withinRegions==source) throw new ExecutionError("'Within regions' parameter should not be the same as the 'Motif track' parameter");
        if (withinRegions!=null) withinRegions=withinRegions.flatten(); // this is necessary to avoid overlapping regions in the original dataset
        if (withinRegions!=null) withinRegionsTrackName=withinRegions.getName();        
        MotifCollection motifcollection=(MotifCollection)task.getParameter("Motifs");
        motifCollectionName=motifcollection.getName();
        counts=new HashMap<String,double[]>(motifcollection.size());
        for (String motifname:motifcollection.getAllMotifNames()) {
            counts.put(motifname, new double[]{0.0,0.0,0.0,0.0,0.0});
        }          
        MotifNumericMap backgroundFrequencies=(MotifNumericMap)task.getParameter("Background frequencies");
        MotifNumericMap expectedCountsMap=(MotifNumericMap)task.getParameter("Background counts");        
        if (expectedCountsMap!=null) { // use explicit background counts (and derive background frequency)
           backgroundFrequenciesName=expectedCountsMap.getName();  
        }
        else
        if (backgroundFrequencies!=null) {
            for (String key:backgroundFrequencies.getAllKeys(task.getEngine())) {
                double value=backgroundFrequencies.getValue(key);
                if (Double.isNaN(value) || value<0 || value>1) throw new ExecutionError("Motif background frequency map '"+backgroundFrequencies.getName()+"' contains value outside legal range [0-1]\n"+key+" => "+value);
            }  
            backgroundFrequenciesName=backgroundFrequencies.getName();
        }
        bonferroniStrategy=(String)task.getParameter("Bonferroni correction");
        Double sigDouble=(Double)task.getParameter("Significance threshold");
        if (sigDouble!=null) significanceThreshold=sigDouble.doubleValue();
        SequenceCollection collection=(SequenceCollection)task.getParameter("Sequences");
        if (collection==null) collection=task.getEngine().getDefaultSequenceCollection();
        else sequenceCollectionName=collection.getName();
        if (sequenceCollectionName!=null && sequenceCollectionName.equals(task.getEngine().getDefaultSequenceCollectionName())) sequenceCollectionName=null;
        sequenceCollectionSize=collection.size();
        MotifLabEngine engine=task.getEngine();
        int[] maxoccurrences=new int[40];
        for (int i=0;i<maxoccurrences.length;i++) maxoccurrences[i]=maxOccurrences(i, collection, withinRegions, engine); // LUT for max occurrences
        
        MotifNumericMap explicitCountsMap=(MotifNumericMap)task.getParameter("Motif counts");
        MotifNumericMap explicitSequenceCountsMap=(MotifNumericMap)task.getParameter("Sequence counts");        
        if (explicitCountsMap!=null) {
            for (String motifname:counts.keySet()) {
                  double value=explicitCountsMap.getValue(motifname);
                  double[] motifcounts=counts.get(motifname);
                  motifcounts[1]=value; // set total occurrences (this affects the 'counts' variable!)    
                  if (explicitSequenceCountsMap!=null) {
                      double support=explicitSequenceCountsMap.getValue(motifname);
                      motifcounts[0]=support;
                  }
            }
        } else {
            // count the motif occurrences in the sequences
            int s=0;
            for (String sequenceName:collection.getAllSequenceNames()) {
                s++;
                RegionSequenceData seq=(RegionSequenceData)source.getSequenceByName(sequenceName);
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                task.setStatusMessage("Executing analysis: "+getAnalysisName()+" ("+s+"/"+sequenceCollectionSize+")");
                task.setProgress(s, sequenceCollectionSize); //
                Thread.yield();
                RegionSequenceData withinRegionsSeq=(withinRegions==null)?null:(RegionSequenceData)withinRegions.getSequenceByName(sequenceName);

                HashSet<String> present=new HashSet<String>();
                for (Region r:seq.getOriginalRegions()) {
                  if (withinRegionsSeq!=null && !isRegionInsideOtherRegions(r, withinRegionsSeq)) continue; // target region is not inside one of the 'within region' regions                        
                  String type=r.getType();
                  if (counts.containsKey(type)) { // only count valid motifs (from selected collection)
                      if (!present.contains(type)) present.add(type);
                      double[] motifcounts=counts.get(type);
                      motifcounts[1]++; // increase total occurrences (this affects the 'counts' variable!)
                  }
                }
                // increase sequence support for motifs present in this sequences
                for (String motif:present) {
                    double[] motifcounts=counts.get(motif);
                    motifcounts[0]++; // increase sequence occurrences
                }
            } // end for each sequence (count motifs)
        }
        // evaluate significance
        int presentMotifs=0;
        for (String motif:counts.keySet()) {
            double[] motifcounts=counts.get(motif);
            int motiflength=0;
            if (engine.dataExists(motif, Motif.class)) motiflength=((Motif)engine.getDataItem(motif)).getLength();
            motifcounts[2]=(motiflength>=maxoccurrences.length)?maxOccurrences(motiflength, collection, withinRegions, engine):maxoccurrences[motiflength]; // This is the maximum number of occurrences for a motif with the same size in these sequences (double stranded). Use LUT if size is smaller than 40
            if (motifcounts[1]>0) presentMotifs++;            
            if (expectedCountsMap!=null) {
                double expectedFrequency=expectedCountsMap.getValue(motif); // these are total counts!
                expectedFrequency=expectedFrequency/motifcounts[2]; // dividing by max occurrences to get frequencies
                BinomialDistribution binomialTarget = new BinomialDistribution((int)motifcounts[2],expectedFrequency);
                double pvalueOverrep=-1;
                try {pvalueOverrep=(1-binomialTarget.cumulativeProbability((int)motifcounts[1]))+binomialTarget.probability((int)motifcounts[1]);} catch (Exception e) {engine.logMessage("WARNING (p-value error):"+e.getMessage());}
                if (pvalueOverrep>1) pvalueOverrep=1.0; // this can happen apparently :(                
                motifcounts[3]=expectedFrequency;
                motifcounts[4]=pvalueOverrep;                
            } 
            else
            if (backgroundFrequencies!=null) {
                double expectedFrequency=backgroundFrequencies.getValue(motif);
                BinomialDistribution binomialTarget = new BinomialDistribution((int)motifcounts[2],expectedFrequency);
                double pvalueOverrep=-1;
                try {pvalueOverrep=(1-binomialTarget.cumulativeProbability((int)motifcounts[1]))+binomialTarget.probability((int)motifcounts[1]);} catch (Exception e) {engine.logMessage("WARNING (p-value error):"+e.getMessage());}
                if (pvalueOverrep>1) pvalueOverrep=1.0; // this can happen apparently :(                
                motifcounts[3]=expectedFrequency;
                motifcounts[4]=pvalueOverrep;
            }            
        }

             if (bonferroniStrategy.equals(ALL_MOTIFS)) multiplehypothesis=motifcollection.size();
        else if (bonferroniStrategy.equals(PRESENT_MOTIFS)) {multiplehypothesis=presentMotifs;}
    }

     /** Returns TRUE if the selected region is fully within any one of the regions in the provided Sequence */
    private boolean isRegionInsideOtherRegions(Region region, RegionSequenceData sequence) {
        for (Region other:sequence.getOriginalRegions()) {
            if (region.getRelativeStart()>=other.getRelativeStart() && region.getRelativeEnd()<=other.getRelativeEnd()) return true;
        }
        return false;
    }    
    
    /** Returns a the maximal number of times a motif with the given length could match in the sequence collection (both strands)*/
    private int maxOccurrences(int motiflength, SequenceCollection collection, MotifLabEngine engine) throws ExecutionError {
        int total=0;
        for (int i=0;i<collection.size();i++) {
            String seqname=collection.getSequenceNameByIndex(i);
            Data data=engine.getDataItem(seqname);
            if (data==null || !(data instanceof Sequence)) throw new ExecutionError(seqname+" is not a sequence");
            Sequence sequence=(Sequence)data;
            total+=(sequence.getSize()-motiflength+1)*2;
        }
        return total;
    }

    /** Returns a the maximal number of times a motif with the given length could match in the sequence collection (both strands) within the regions of the given Region dataset*/
    private int maxOccurrences(int motiflength, SequenceCollection collection, RegionDataset withinRegionsDataset, MotifLabEngine engine) throws ExecutionError {
        if (withinRegionsDataset==null) return maxOccurrences(motiflength,collection,engine);
        int total=0;
        for (int i=0;i<collection.size();i++) {
            String seqname=collection.getSequenceNameByIndex(i);
            RegionSequenceData sequence=(RegionSequenceData)withinRegionsDataset.getSequenceByName(seqname);
            if (sequence==null) throw new ExecutionError(seqname+" is not a sequence");
            for (Region segment:sequence.getOriginalRegions()) {
               total+=(segment.getLength()<motiflength)?0:(segment.getLength()-motiflength+1)*2;
            }
        }
        return total;
    }
    
    private class SortOrderComparator implements Comparator<Object[]> {
            String sortorder=null;
            public SortOrderComparator(String order) {
                sortorder=order;
            }
            @Override
            public int compare(Object[] motif1, Object[] motif2) { //
                 if (sortorder.equals(SORT_BY_SEQUENCE_OCCURRENCES)) {
                     Integer value1=(Integer)motif1[1];
                     Integer value2=(Integer)motif2[1];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;                     
                     int res=value2.compareTo(value1); // sorts descending!
                     if (res!=0) return res;
                     else return ((Integer)motif2[2]).compareTo(((Integer)motif1[2])); // if equal, sorts by total count descending!
                } else if (sortorder.equals(SORT_BY_TOTAL_OCCURRENCES)) {
                     Integer value1=(Integer)motif1[2];
                     Integer value2=(Integer)motif2[2];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;                      
                     int res=value2.compareTo(value1); // sorts descending!
                     if (res!=0) return res;
                     else return ((Integer)motif2[1]).compareTo(((Integer)motif1[1])); // if equal, sorts by total count descending!
                } else if (sortorder.equals(SORT_BY_P_VALUE)) {
                     Double value1=(Double)motif1[4];
                     Double value2=(Double)motif2[4];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;
                     if (Double.isNaN(value1)) return 1;
                     if (Double.isNaN(value2)) return -1;
                     int res=value1.compareTo(value2);  // sorts ascending!
                     if (res!=0) return res;
                     else { // same p-value
                         Double exp1=(Double)motif1[5];
                         Double exp2=(Double)motif2[5]; 
                         if (exp1==null && exp2==null) return 0;
                         if (exp1==null) return 1;
                         if (exp2==null) return -1;
                         if (Double.isNaN(exp1)) return 1;
                         if (Double.isNaN(exp2)) return -1;                         
                         if (exp1==exp2) return ((Integer)motif2[1]).compareTo(((Integer)motif1[1]));
                         else return exp1.compareTo(exp2);
                     } // if equal, sorts by total count descending!
                } else { // sort by motif ID
                    String motifname1=(String)motif1[0];
                    String motifname2=(String)motif2[0];
                    return motifname1.compareTo(motifname2); // sorts ascending!
                }
            }
    }


    @Override
    protected JPanel getDisplayPanel(MotifLabGUI gui, boolean modal) {
        JPanel displayPanel=new JPanel(new BorderLayout());
        JPanel headerPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        StringBuilder headerString=new StringBuilder("<html>Motif occurrence analysis with motifs from <b>");
        headerString.append(motifCollectionName);
        headerString.append("</b> and sites from <b>");
        headerString.append(motifTrackName);
        headerString.append("</b>");        
        if (withinRegionsTrackName!=null) {            
            headerString.append(" within <b>");
            headerString.append(withinRegionsTrackName);
            headerString.append("</b> regions");
        }        
        headerString.append("<br>on ");
        headerString.append(sequenceCollectionSize);
        headerString.append(" sequence"+((sequenceCollectionSize!=1)?"s":""));
        if (sequenceCollectionName!=null) {
            headerString.append(" from collection <b>");
            headerString.append(sequenceCollectionName);
            headerString.append("</b>");
        }
        headerString.append(".");
        if (backgroundFrequenciesName!=null) {
            headerString.append(" Expected motif frequencies from <b>");
            headerString.append(backgroundFrequenciesName);
            headerString.append("</b>.");
            headerString.append("<br>Statistical significance evaluated using a binomial test with p-value threshold="+significanceThreshold);
            if (bonferroniStrategy.equals(ALL_MOTIFS)) headerString.append("<br />(Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering all "+multiplehypothesis+" motifs tested)");
            if (bonferroniStrategy.equals(PRESENT_MOTIFS)) headerString.append("<br />(Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering "+multiplehypothesis+" present motifs)");
        }

        headerString.append("</html>");
        headerPanel.add(new JLabel(headerString.toString()));
        MotifOccurrenceTableModel tablemodel=new MotifOccurrenceTableModel(gui);
        GenericMotifBrowserPanel panel=new GenericMotifBrowserPanel(gui, tablemodel, modal);
        CellRenderer_Support renderer=new CellRenderer_Support();
        JTable table=panel.getTable();
        table.getColumn("Sequences").setCellRenderer(renderer);
        table.getColumn("Class").setCellRenderer(new CellRenderer_Classification());
        if (tablemodel.compareToExpected) table.getColumn("p-value").setCellRenderer(new CellRenderer_ExpectedVSobserved(gui.getVisualizationSettings()));
        table.getColumn("ID").setPreferredWidth(60);
        table.getColumn("Sequences").setPreferredWidth(80);
        table.getColumn("Class").setPreferredWidth(50);
        table.getColumn("Total").setPreferredWidth(60);
        if (tablemodel.compareToExpected) table.getColumn("Expected").setPreferredWidth(60);
        if (tablemodel.compareToExpected) table.getColumn("p-value").setPreferredWidth(80);
        if (tablemodel.compareToExpected) panel.setPreferredSize(new java.awt.Dimension(700,500));
        else panel.setPreferredSize(new java.awt.Dimension(600,500));
        if (tablemodel.compareToExpected) {
            table.getRowSorter().toggleSortOrder(PVALUE);
        } else {
            table.getRowSorter().toggleSortOrder(TOTAL);
            table.getRowSorter().toggleSortOrder(TOTAL);
        }
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8,5,0,5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)
          ));
        displayPanel.add(headerPanel,BorderLayout.NORTH);
        displayPanel.add(panel,BorderLayout.CENTER);
        return displayPanel;
    }

private class MotifOccurrenceTableModel extends AbstractTableModel {
    private String[] columnNames=null;
    private String[] motifnames=null;
    private MotifLabEngine engine;
    public boolean compareToExpected=false;
    private VisualizationSettings settings;

    public MotifOccurrenceTableModel(MotifLabGUI gui) {
        this.engine=gui.getEngine();
        this.settings=gui.getVisualizationSettings();
        motifnames=new String[counts.size()];
        int i=0;
        for (String name:counts.keySet()) {
           motifnames[i]=name;
           i++;
        }
        if (backgroundFrequenciesName==null) {
            columnNames=new String[]{" ","ID","Name","Class","Sequences","Total","Logo"};
        } else {
            columnNames=new String[]{" ","ID","Name","Class","Sequences","Total","Expected","p-value","Logo"};
            compareToExpected=true;
        }
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case COLOR:return Color.class;            
            case ID:return String.class;
            case NAME:return String.class;
            case MOTIF_CLASS:return String.class;                
            case SEQUENCE_SUPPORT:return Integer.class;
            case TOTAL:return Integer.class;
            case LOGO_OR_EXPECTED:return (compareToExpected)?Double.class:Motif.class;
            case PVALUE:return Double.class;
            case LOGO:return Motif.class;
            default:return Object.class;
        }
    }

    public final Motif getMotif(String id) {
        Data data=engine.getDataItem(id);
        if (data instanceof Motif) return (Motif)data;
        else return null;
    }
    
    public final double getExpected(String id) {
        double[] stat=counts.get(id);
        return stat[2]*stat[3]; // expected frequency * number of tries (maximum number of occurrences)
    }    

    public String getMotifName(String id) {
        Data data=engine.getDataItem(id);
        if (data instanceof Motif) return ((Motif)data).getShortName();
        else return "unknown";
    }
    
    public String getMotifClass(String id) {
        Data data=engine.getDataItem(id);
        if (data instanceof Motif) {
            String motifclass=((Motif)data).getClassification();
            if (motifclass!=null) return motifclass; else return "unknown";
        }
        else return "unknown";
    }


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLOR:return settings.getFeatureColor(motifnames[rowIndex]);            
            case ID:return motifnames[rowIndex];
            case NAME:return getMotifName(motifnames[rowIndex]);    
            case MOTIF_CLASS:return getMotifClass(motifnames[rowIndex]);                
            case SEQUENCE_SUPPORT:return getIntValueAt(rowIndex,0);
            case TOTAL:return getIntValueAt(rowIndex,1);
            case LOGO_OR_EXPECTED:return (compareToExpected)?getExpected(motifnames[rowIndex]):getMotif(motifnames[rowIndex]);
            case PVALUE:return counts.get(motifnames[rowIndex])[4];
            case LOGO:return getMotif(motifnames[rowIndex]);
            default:return Object.class;
        }
    }

    private int getIntValueAt(int row,int col) { // this method is an ad-hoc solution to a casting problem that sometimes occur (perhaps from old sessions)
        Object countsrow=counts.get(motifnames[row]);
        if (countsrow instanceof double[]) {
            double[] rowcounts=(double[])countsrow;
            double value=rowcounts[col];
            return (int)value;
        } else if (countsrow instanceof int[]) {
            int[] rowcounts=(int[])countsrow;
            return rowcounts[col];         
        } else return 0; // this should not happen
    }
    
    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	return false;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return motifnames.length;
    }

}
private class CellRenderer_Classification extends DefaultTableCellRenderer {
    public CellRenderer_Classification() {
       super();
    }
    @Override
    public void setValue(Object value) {
       setText((String)value);
       setToolTipText(MotifClassification.getFullLevelsStringAsHTML((String)value));          
    }
}// end class CellRenderer_RightAlign



private class CellRenderer_Support extends DefaultTableCellRenderer {
    public CellRenderer_Support() {
       super();
       this.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
    }
    @Override
    public void setValue(Object value) {
       if (value!=null && value instanceof Integer) {
           int support=((Integer)value).intValue();
           int percentage=(int)(((double)support*100f)/(double)sequenceCollectionSize);
           setText(support+" of "+sequenceCollectionSize+" ("+percentage+"%)");
       } else if (value!=null && value instanceof Double) {
           setText(""+((Double)value).doubleValue());
       }
   }
}// end class CellRenderer_RightAlign


private class CellRenderer_ExpectedVSobserved extends DefaultTableCellRenderer {
    private java.awt.Color VERY_SIGNIFICANT;
    private java.awt.Color VERY_SIGNIFICANT_SELECTED;
    private java.awt.Color SIGNIFICANT;
    private java.awt.Color SIGNIFICANT_SELECTED;        
   
    public CellRenderer_ExpectedVSobserved(VisualizationSettings settings) {
       VERY_SIGNIFICANT=settings.getSystemColor("verysignificant");
       SIGNIFICANT=settings.getSystemColor("significant");
       VERY_SIGNIFICANT_SELECTED=VERY_SIGNIFICANT.darker().darker();
       SIGNIFICANT_SELECTED=SIGNIFICANT.darker().darker();
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c=super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        int observed=(Integer)table.getValueAt(row, TOTAL);
        double expected=(Double)table.getValueAt(row, LOGO_OR_EXPECTED);
        double pvalue=(Double)table.getValueAt(row, PVALUE);
        double correctedThreshold=significanceThreshold/(double)multiplehypothesis;
        boolean isSignificant=(observed>0 && expected>0 && pvalue<=correctedThreshold);
        if (!isSelected) {
            if (observed>expected && expected==0) c.setBackground(VERY_SIGNIFICANT);
            else if (isSignificant) c.setBackground(SIGNIFICANT);
            else c.setBackground(table.getBackground());
        } else {
            if (observed>expected && expected==0) c.setBackground(VERY_SIGNIFICANT_SELECTED);
            else if (isSignificant) c.setBackground(SIGNIFICANT_SELECTED);
            else c.setBackground(table.getSelectionBackground());
        }
        if (c instanceof javax.swing.JLabel) {
            ((javax.swing.JLabel)c).setHorizontalAlignment(javax.swing.JLabel.RIGHT);
        }                
        return c;
    }

}// end class CellRenderer_RightAlign

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
