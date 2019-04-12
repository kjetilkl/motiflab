/*
 
 
 */

package motiflab.engine.data.analysis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import motiflab.engine.data.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.HypergeometricDistribution;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.Graph;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.TaskRunner;
import motiflab.engine.dataformat.DataFormat;
import motiflab.gui.GenericMotifBrowserPanel;
import motiflab.gui.OutputPanel;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.MotifLogo;
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
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;

/**
 *
 * @author kjetikl
 */
public class CompareMotifOccurrencesAnalysis extends Analysis {
    
    private static String typedescription="Analysis: compare motif occurrences";
    private static String analysisName="compare motif occurrences";
    private static String description="Compares the distribution of motifs within two sequence collections to find which motifs are over- and underrepresented";
    private transient HashMap<String,HashMap<String,Integer>> storage=new HashMap<String,HashMap<String,Integer>>(); // temporary storage for motif counts during runAnalysis. First key is sequence name, second is motif name
    private double significanceThreshold=0.05;
    private int multiplehypothesis=1; // number of motifs counted in bonferroni correction
    private String bonferroniStrategy=ALL_MOTIFS;
    private String statisticalTest="Binomial";
    private String motifsTrackName=null;
    private String targetSetName=null;
    private String controlSetName=null;
    private String motifCollectionName=null;
    private String withinRegionsTrackName=null;
    private static double OVERREPRESENTED_IN_TARGET_BY_DEFAULT=5; // these are only present in target
    private static double OVERREPRESENTED_IN_TARGET=4;
    private static double OVERREPRESENTED_IN_CONTROL_BY_DEFAULT=3;  // these are only present in control
    private static double OVERREPRESENTED_IN_CONTROL=2;
    private static double SAME_IN_BOTH_SETS=1;
    private static double NOT_PRESENT=0;
    private static char[] expressionResultSign=new char[]{'-','=','<','<','>','>'};
            
    private static final String NONE="None";
    private static final String ALL_MOTIFS="All motifs";
    private static final String PRESENT_MOTIFS="Present motifs";
    
    private static final String SORT_BY_MOTIF="Motif ID";
    private static final String SORT_BY_EXPRESSION="Expression";
    

    private HashMap<String,Double[]> results=new HashMap<String,Double[]>(); // stores motif name and a result-array for that motif
    // the results are stored in a Double array for easy access and cloning
    // the Double[] contains 5 values:
    // [0] a "flag" value with the result of the analysis expressed using one of the double constants above
    // [1] (an int) the number of motifs found in the target set (or number of sequences with that motif for hypergeometric test)
    // [2] (an int) the number of motifs found in the control set (or number of sequences with that motif for hypergeometric test)
    // [3] a p-value for the testing overrepresentation in target set
    // [4] a p-value for the testing overrepresentation in control set
    
    

    public CompareMotifOccurrencesAnalysis() {
        this.name="CompareMotifOccurrencesAnalysis_temp";
        addParameter("Motif track",RegionDataset.class, null,new Class[]{RegionDataset.class},"A track containing binding sites for motifs",true,false);
        addParameter("Motifs",MotifCollection.class, null,new Class[]{MotifCollection.class},"The motifs to consider in this analysis",true,false);
        addParameter("Within regions",RegionDataset.class, null,new Class[]{RegionDataset.class},"Limits the calculation of motif-frequencies to be within the selected regions and not the full sequences",false,false);
        addParameter("Target set",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"A set of sequences (for instance a set of upregulated sequences)",true,false);
        addParameter("Control set",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"A second set of sequences to compare the first set against (for instance a set of downregulated sequences)",true,false);
        addParameter("Statistical test",String.class, "Binomial",new String[]{"Binomial","Hypergeometric"},"The statistical test used to compute p-values",true,false);
        addParameter("Significance threshold",Double.class, new Double(0.05),new Double[]{0.0,1.0},"The initial p-value threshold to use when estimating significance",true,false);
        addParameter("Bonferroni correction",String.class, ALL_MOTIFS,new String[]{NONE,PRESENT_MOTIFS,ALL_MOTIFS},"If selected, the initial p-value threshold will be adjusted according to the given number of motifs",true,false);        
    }  
    
    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Motif track","Motifs","Target set","Control set"};}   
    
    @Override
    public boolean canUseAsSourceProxy(Data data) {
        if (data instanceof RegionDataset) return ((RegionDataset)data).isMotifTrack(); // only allow Motif Tracks as input
        else return (data instanceof MotifCollection || data instanceof SequenceCollection);
    }    
    
    @Override
    public String getAnalysisName() {
        return analysisName;
    }
     
    @Override
    public String getDescription() {return description;}
    
    @Override
    public Class getCollateType() {
        return Motif.class;
    }

    @Override
    public String[] getColumnsExportedForCollation() {
        return new String[]{"count target","count control","p-value target","p-value control","group"};
    }

    @Override
    public Class getColumnType(String column) {
             if (column.equalsIgnoreCase("count target") || column.equalsIgnoreCase("count control")) return Integer.class;
        else if (column.equalsIgnoreCase("p-value target") || column.equalsIgnoreCase("p-value control")) return Double.class;
        else if (column.equalsIgnoreCase("group")) return String.class;
        else return null;
    }

    @Override
    public HashMap<String,Object> getColumnData(String column) throws ExecutionError {
        Class coltype=getColumnType(column);
        if (coltype==null) throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
        HashMap<String,Object> columnData=new HashMap<String, Object>();
        for (String motifname:results.keySet()) {
            Double[] stat=results.get(motifname);
            String overrepclass="notpresent";
            String group="F";
            Object value=null;
                 if (stat[0]==OVERREPRESENTED_IN_TARGET_BY_DEFAULT) {overrepclass="onlyintarget";group="A";}
            else if (stat[0]==OVERREPRESENTED_IN_CONTROL_BY_DEFAULT) {overrepclass="onlyincontrol";group="E";}
            else if (stat[0]==OVERREPRESENTED_IN_TARGET) {overrepclass="overrepintarget";group="B";}
            else if (stat[0]==OVERREPRESENTED_IN_CONTROL) {overrepclass="overrepincontrol";group="D";}
            else if (stat[0]==SAME_IN_BOTH_SETS) {overrepclass="samerate";group="C";}
                 if (column.equalsIgnoreCase("count target")) value=new Integer(stat[1].intValue());
            else if (column.equalsIgnoreCase("count control")) value=new Integer(stat[2].intValue());
            else if (column.equalsIgnoreCase("p-value target")) value=stat[3];
            else if (column.equalsIgnoreCase("p-value control")) value=stat[4];
            else if (column.equalsIgnoreCase("group")) value=group;
            columnData.put(motifname, new AnnotatedValue(value,overrepclass));
        }
        return columnData;
    }

    private final String[] variables = new String[]{"Overrepresented in target","Overrepresented in control","Present only in target","Present only in control","Same expression","All present motifs","Motifs not present", "count target","count control","p-value target","p-value control","corrected threshold","clusters"};

    @Override
    public String[] getResultVariables() {
        return variables;
    }

    @Override    
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename.equals("corrected threshold")) return new NumericVariable("temp",significanceThreshold/(double)multiplehypothesis);
        else if (variablename.equals("count target")) return createMapFromResult(1);
        else if (variablename.equals("count control"))  return createMapFromResult(2);
        else if (variablename.equals("p-value target"))  return createMapFromResult(3);
        else if (variablename.equals("p-value control"))  return createMapFromResult(4);
        else if (variablename.equals("clusters"))  return createPartition(engine);
        else {
            MotifCollection collection=new MotifCollection("comparetemp");
                 if (variablename.equals("Overrepresented in target"))  {addMotifsToCollection(collection,OVERREPRESENTED_IN_TARGET,true,engine);addMotifsToCollection(collection,OVERREPRESENTED_IN_TARGET_BY_DEFAULT,true,engine);}
            else if (variablename.equals("Overrepresented in control")) {addMotifsToCollection(collection,OVERREPRESENTED_IN_CONTROL,true,engine);addMotifsToCollection(collection,OVERREPRESENTED_IN_CONTROL_BY_DEFAULT,true,engine);}
            else if (variablename.equals("Present only in target")) addMotifsToCollection(collection,OVERREPRESENTED_IN_TARGET_BY_DEFAULT,true,engine);
            else if (variablename.equals("Present only in control")) addMotifsToCollection(collection,OVERREPRESENTED_IN_CONTROL_BY_DEFAULT,true,engine);
            else if (variablename.equals("Same expression")) addMotifsToCollection(collection,SAME_IN_BOTH_SETS,true,engine);
            else if (variablename.equals("Motifs not present")) addMotifsToCollection(collection,NOT_PRESENT,true,engine);
            else if (variablename.equals("All present motifs")) addMotifsToCollection(collection,NOT_PRESENT,false,engine);
            else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
            return collection;
        }
        //throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }
    
    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       if (variablename.equals("corrected threshold")) return NumericVariable.class;
       else if (variablename.equals("clusters")) return MotifPartition.class;
       else if (variablename.equals("count target") || variablename.equals("count control") || variablename.equals("p-value target") || variablename.equals("p-value control")) return MotifNumericMap.class;
       else return MotifCollection.class; // all other exported values in this analysis are MotifCollections
    }      
    
    private MotifNumericMap createMapFromResult(int index) {
            MotifNumericMap map=new MotifNumericMap("temp",0);
            for (String motifname:results.keySet()) {
                Double[] stats=results.get(motifname);
                map.setValue(motifname, stats[index].doubleValue());
            }
            return map;
    }

    private void addMotifsToCollection(MotifCollection collection, double expressionpattern, boolean hasexpression, MotifLabEngine engine) {        
        for (String motifname:results.keySet()) {
            Data motif=engine.getDataItem(motifname);
            if (motif==null || !(motif instanceof Motif)) continue;
            Double[] value=results.get(motifname);
            if ((hasexpression && value[0]==expressionpattern) || (!hasexpression && value[0]!=expressionpattern)) collection.addMotif((Motif)motif);
        }
    }
    
    private MotifPartition createPartition(MotifLabEngine engine) {        
        MotifPartition partition = new MotifPartition("comparetemp");
        for (String motifname:results.keySet()) {
            Data motif=engine.getDataItem(motifname);
            if (motif==null || !(motif instanceof Motif)) continue;
            Double[] value=results.get(motifname);
            String clusterName="error";
                 if (value[0]==OVERREPRESENTED_IN_TARGET_BY_DEFAULT) clusterName="Present_only_in_"+targetSetName;
            else if (value[0]==OVERREPRESENTED_IN_TARGET) clusterName="Overrepresented_in_"+targetSetName;
            else if (value[0]==OVERREPRESENTED_IN_CONTROL) clusterName="Overrepresented_in_"+controlSetName;
            else if (value[0]==OVERREPRESENTED_IN_CONTROL_BY_DEFAULT) clusterName="Present_only_in_"+controlSetName;
            else if (value[0]==SAME_IN_BOTH_SETS) clusterName="Same_rate";
            else if (value[0]==NOT_PRESENT) clusterName="Not_present";
            partition.addMotif((Motif)motif, clusterName);       
        }
        return partition;
    }    
    
    
    /** Stores a count for the given motif in the given sequence */
    public synchronized void storeCount(String sequenceName,String motifID,int count) {
        if (!storage.containsKey(sequenceName)) storage.put(sequenceName, new HashMap<String,Integer>());
        HashMap<String,Integer> seqHash=storage.get(sequenceName);
        seqHash.put(motifID, count);
    }
    
    /** Increases the count for the given motif in the given sequence */
    public synchronized void increaseCount(String sequenceName,String motifID) {
        if (!storage.containsKey(sequenceName)) storage.put(sequenceName, new HashMap<String,Integer>());
        HashMap<String,Integer> seqHash=storage.get(sequenceName);
        if (seqHash.containsKey(motifID)) {
            seqHash.put(motifID, seqHash.get(motifID)+1);
        }
        else seqHash.put(motifID, 1);
    }
    
    /** Returns the count for the given motif in the given sequence */
    public synchronized int getCount(String sequenceName,String motifID) {
        if (!storage.containsKey(sequenceName)) return 0;
        HashMap<String,Integer> seqHash=storage.get(sequenceName);
        if (!seqHash.containsKey(motifID)) return 0;
        else return seqHash.get(motifID);      
    }
       
    
    @Override
    @SuppressWarnings("unchecked")    
    public CompareMotifOccurrencesAnalysis clone() {
        CompareMotifOccurrencesAnalysis newanalysis=new CompareMotifOccurrencesAnalysis();
        super.cloneCommonSettings(newanalysis);    
        newanalysis.results=(HashMap<String,Double[]>)this.results.clone();
        newanalysis.statisticalTest=this.statisticalTest;
        newanalysis.bonferroniStrategy=this.bonferroniStrategy;
        newanalysis.significanceThreshold=this.significanceThreshold;    
        newanalysis.multiplehypothesis=this.multiplehypothesis;
        newanalysis.motifsTrackName=this.motifsTrackName;
        newanalysis.targetSetName=this.targetSetName;
        newanalysis.controlSetName=this.controlSetName;
        newanalysis.withinRegionsTrackName=this.withinRegionsTrackName;
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        this.results=(HashMap<String,Double[]>)((CompareMotifOccurrencesAnalysis)source).results.clone();
        this.bonferroniStrategy=((CompareMotifOccurrencesAnalysis)source).bonferroniStrategy;
        this.statisticalTest=((CompareMotifOccurrencesAnalysis)source).statisticalTest;
        this.significanceThreshold=((CompareMotifOccurrencesAnalysis)source).significanceThreshold;
        this.multiplehypothesis=((CompareMotifOccurrencesAnalysis)source).multiplehypothesis;
        this.motifsTrackName=((CompareMotifOccurrencesAnalysis)source).motifsTrackName;
        this.targetSetName=((CompareMotifOccurrencesAnalysis)source).targetSetName;
        this.controlSetName=((CompareMotifOccurrencesAnalysis)source).controlSetName;
        this.withinRegionsTrackName=((CompareMotifOccurrencesAnalysis)source).withinRegionsTrackName;
    }   
    
    public static String getType() {return typedescription;}
    
    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}




    @Override
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings outputSettings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        Color [] basecolors=vizSettings.getBaseColors();
        MotifLogo sequencelogo=new MotifLogo(basecolors,sequencelogoSize);
        MotifCollection include=null;
        String sortorder=SORT_BY_MOTIF;
        String showSequenceLogosString=MOTIF_LOGO_NO;
        boolean showColorBoxes=false;
        if (outputSettings!=null) {
          try {
                 Parameter[] defaults=getOutputParameters();
                 include=(MotifCollection)outputSettings.getResolvedParameter("Include",defaults,engine);                  
                 sortorder=(String)outputSettings.getResolvedParameter("Sort by",defaults,engine);
                 showSequenceLogosString=(String)outputSettings.getResolvedParameter("Logos",defaults,engine);
                 showColorBoxes=(Boolean)outputSettings.getResolvedParameter("Color boxes",defaults,engine);                 
          } 
          catch (ExecutionError e) {throw e;} 
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        } 
        boolean showSequenceLogos=(showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_NEW) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_SHARED) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_TEXT));
        engine.createHTMLheader("Motif Occurrence Comparison", null, null, true, true, true, outputobject);
        formatSummary(outputobject,false);
        outputobject.append("<br />\n<table class=\"sortable\">\n",HTML);
        outputobject.append("<tr>",HTML);
        if (showColorBoxes) outputobject.append("<th>&nbsp;</th>",HTML);
        outputobject.append("<th>ID</th><th>Name</th><th class=\"sorttable_ip\">Class</th><th>Target</th><th>Control</th><th class=\"sorttable_numeric\"><nobr>p-value target</nobr></th><th class=\"sorttable_numeric\"><nobr>p-value control</nobr></th><th>Group</th>"+((showSequenceLogos)?"<th class=\"sorttable_nosort\">Logo</th>":"")+"</tr>\n",HTML);
        ArrayList<String> motifs=new ArrayList<String>(results.size());
        if (include!=null) {
           for (String entry:include.getValues()) {
               if (results.containsKey(entry)) motifs.add(entry);
           }
        } else motifs.addAll(results.keySet());        
        if (sortorder.equalsIgnoreCase(SORT_BY_EXPRESSION)) Collections.sort(motifs,new SortOrderComparator());
        else Collections.sort(motifs);
        int prog=0;
        for (String motifname:motifs) {
            prog++;
            Data motif=engine.getDataItem(motifname);
            // if (include!=null && !include.contains(motifname)) continue;            
            String motifpresentationname=motifname;
            String motifclass=""; 
            String motifclassname=null;
            if (motif instanceof Motif) {
                motifpresentationname=((Motif)motif).getShortName();
                motifclass=((Motif)motif).getClassification();
                if (motifclass==null) motifclass="unknown";
                else motifclassname=MotifClassification.getNameForClass(motifclass);
                if (motifclassname!=null) {
                    motifclassname=escapeHTML(motifclassname);
                    motifclassname=motifclassname.replace("\"", "&#34;");// escape quotes
                } 
            }               
            Double[] value=results.get(motifname);    
            String overrepclass="";
            String group="";
                 if (value[0]==OVERREPRESENTED_IN_TARGET_BY_DEFAULT) {overrepclass="onlyintarget";group="A";}
            else if (value[0]==OVERREPRESENTED_IN_CONTROL_BY_DEFAULT) {overrepclass="onlyincontrol";group="E";}
            else if (value[0]==OVERREPRESENTED_IN_TARGET) {overrepclass="overrepintarget";group="B";}
            else if (value[0]==OVERREPRESENTED_IN_CONTROL) {overrepclass="overrepincontrol";group="D";}
            else if (value[0]==SAME_IN_BOTH_SETS) {overrepclass="samerate";group="C";}
            else {
                overrepclass="notpresent";
                group="F";
            }
            outputobject.append("<tr>",HTML);
            if (showColorBoxes) {
                Color color=Color.WHITE;               
                if (motif instanceof Motif) color=vizSettings.getFeatureColor(motifname);             
                String colorString=VisualizationSettings.convertColorToHTMLrepresentation(color);
                outputobject.append("<td><div style=\"width:10px;height:10px;border:1px solid #000;background-color:"+colorString+";\"></div></td>",HTML);
            }               
            outputobject.append("<td>"+escapeHTML(motifname)+"</td>",HTML);
            outputobject.append("<td>"+escapeHTML(motifpresentationname)+"</td>",HTML);
            outputobject.append("<td"+((motifclassname!=null)?(" title=\""+motifclassname+"\""):"")+">"+escapeHTML(motifclass)+"</td>",HTML);
            outputobject.append("<td class=\""+overrepclass+"\">"+value[1].intValue()+"</td>",HTML);
            outputobject.append("<td class=\""+overrepclass+"\">"+value[2].intValue()+"</td>",HTML);
            outputobject.append("<td class=\""+overrepclass+"\">"+Graph.formatNumber(value[3],false)+"</td>",HTML);
            outputobject.append("<td class=\""+overrepclass+"\">"+Graph.formatNumber(value[4],false)+"</td>",HTML);
            outputobject.append("<td class=\""+overrepclass+"\">"+group+"</td>",HTML);
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
            if (task!=null) task.setStatusMessage("Executing operation: output ("+prog+"/"+motifs.size()+")");
            format.setProgress(prog,motifs.size());
        }
        outputobject.append("</table>\n</body>\n</html>\n",HTML);
        format.setProgress(100);
        return outputobject;
    }


    public void formatSummary(OutputData outputobject, boolean alignCenter) {
        int[] counts=new int[6]; // count the number of motifs that fall in each of the 6 classes
        for (String motifname:results.keySet()) {
            Double[] value=results.get(motifname);
            counts[value[0].intValue()]++;
        }
        String withinString="";
        if (withinRegionsTrackName!=null) withinString=" within <span class=\"dataitem\">"+withinRegionsTrackName+"</span> regions";
        if (alignCenter) outputobject.append("<center>",HTML);
        outputobject.append("<h1 class=\"headline\">Motif occurrence comparison for \""+targetSetName+"\" vs \""+controlSetName+"\"</h1>\n",HTML);
        outputobject.append("<div class=\"summary\">The analysis was performed on binding sites from <span class=\"dataitem\">"+motifsTrackName+"</span>"+withinString,HTML);
        outputobject.append("<br />Statistical significance evaluated using a "+statisticalTest.toLowerCase()+" test with p-value threshold="+significanceThreshold,HTML);
        if (bonferroniStrategy.equals(ALL_MOTIFS)) outputobject.append("<br />(Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering all "+multiplehypothesis+" motifs tested)",HTML);
        if (bonferroniStrategy.equals(PRESENT_MOTIFS)) outputobject.append("<br />(Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering "+multiplehypothesis+" present motifs)",HTML);
        outputobject.append("<br /><br />\n<table>\n",HTML);
        if (statisticalTest.equalsIgnoreCase("Binomial")) {
            outputobject.append("<tr><td class=\"onlyintarget\">Motifs present only in target</td><td class=\"overrepintarget\">Motifs overrepresented in target</td><td class=\"samerate\">Same rate</td><td class=\"overrepincontrol\">Motifs overrepresented in control</td><td class=\"onlyincontrol\">Motifs present only in control</td><td class=\"notpresent\">Motifs not present</td></tr>\n",HTML);
            outputobject.append("<tr><td class=\"onlyintarget\" style=\"text-align:center\">"+counts[(int)OVERREPRESENTED_IN_TARGET_BY_DEFAULT]+"</td><td class=\"overrepintarget\" style=\"text-align:center\">"+counts[(int)OVERREPRESENTED_IN_TARGET]+"</td><td class=\"samerate\" style=\"text-align:center\" rowspan=\"2\">"+counts[(int)SAME_IN_BOTH_SETS]+"</td><td class=\"overrepincontrol\" style=\"text-align:center\">"+counts[(int)OVERREPRESENTED_IN_CONTROL]+"</td><td class=\"onlyincontrol\" style=\"text-align:center\">"+counts[(int)OVERREPRESENTED_IN_CONTROL_BY_DEFAULT]+"</td><td class=\"notpresent\" style=\"text-align:center\" rowspan=\"2\">"+counts[(int)NOT_PRESENT]+"</td></tr>\n",HTML);
            outputobject.append("<tr><td class=\"intarget\" style=\"text-align:center\" colspan=\"2\">"+(counts[(int)OVERREPRESENTED_IN_TARGET]+counts[(int)OVERREPRESENTED_IN_TARGET_BY_DEFAULT])+"</td><td class=\"incontrol\" style=\"text-align:center\" colspan=\"2\">"+(counts[(int)OVERREPRESENTED_IN_CONTROL]+counts[(int)OVERREPRESENTED_IN_CONTROL_BY_DEFAULT])+"</td></tr>\n",HTML);
        } else if (statisticalTest.equalsIgnoreCase("Hypergeometric")) {
            outputobject.append("<tr><td class=\"overrepintarget\">Motifs overrepresented in target</td><td class=\"samerate\">Same rate</td><td class=\"overrepincontrol\">Motifs overrepresented in control</td><td class=\"notpresent\">Motifs not present</td></tr>\n",HTML);
            outputobject.append("<tr><td class=\"overrepintarget\" style=\"text-align:center\">"+counts[(int)OVERREPRESENTED_IN_TARGET]+"</td><td class=\"samerate\" style=\"text-align:center\">"+counts[(int)SAME_IN_BOTH_SETS]+"</td><td class=\"overrepincontrol\" style=\"text-align:center\">"+counts[(int)OVERREPRESENTED_IN_CONTROL]+"</td><td class=\"notpresent\" style=\"text-align:center\">"+counts[(int)NOT_PRESENT]+"</td></tr>\n",HTML);
        }
        outputobject.append("</table>\n",HTML);
        outputobject.append("</div>\n",HTML);
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
        int rownum=0;
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(outputobject.getName());
        CreationHelper helper = (showLogosAsImages)?workbook.getCreationHelper():null;
        Drawing drawing = (showLogosAsImages)?sheet.createDrawingPatriarch():null;       
        
        CellStyle title=createExcelStyle(workbook, HSSFCellStyle.BORDER_NONE, (short)0, HSSFCellStyle.ALIGN_LEFT, false);      
        addFontToExcelCellStyle(workbook, title, null, (short)(workbook.getFontAt((short)0).getFontHeightInPoints()*2.5), true, false);
        CellStyle tableheader=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.LIGHT_YELLOW.index, HSSFCellStyle.ALIGN_CENTER, true);      
        
        CellStyle groupA_UP=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.RED.index, HSSFCellStyle.ALIGN_RIGHT, false);      
        CellStyle groupB_sigUP=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.CORAL.index, HSSFCellStyle.ALIGN_RIGHT, false);      
        CellStyle groupC_same=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.YELLOW.index, HSSFCellStyle.ALIGN_RIGHT, false);      
        CellStyle groupD_sigDOWN=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.LIGHT_GREEN.index, HSSFCellStyle.ALIGN_RIGHT, false);          
        CellStyle groupE_DOWN=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.BRIGHT_GREEN.index, HSSFCellStyle.ALIGN_RIGHT, false);          
        CellStyle groupF_notpresent=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.WHITE.index, HSSFCellStyle.ALIGN_RIGHT, false);          
        
        CellStyle groupA_UP_LEGEND=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.RED.index, HSSFCellStyle.ALIGN_CENTER, false);      
        CellStyle groupB_sigUP_LEGEND=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.CORAL.index, HSSFCellStyle.ALIGN_CENTER, false);      
        CellStyle groupC_same_LEGEND=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.YELLOW.index, HSSFCellStyle.ALIGN_CENTER, false);      
        CellStyle groupD_sigDOWN_LEGEND=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.LIGHT_GREEN.index, HSSFCellStyle.ALIGN_CENTER, false);          
        CellStyle groupE_DOWN_LEGEND=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.BRIGHT_GREEN.index, HSSFCellStyle.ALIGN_CENTER, false);          
        CellStyle groupF_notpresent_LEGEND=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.WHITE.index, HSSFCellStyle.ALIGN_CENTER, false);          
        
        groupA_UP_LEGEND.setWrapText(true);
        groupB_sigUP_LEGEND.setWrapText(true);
        groupC_same_LEGEND.setWrapText(true);
        groupD_sigDOWN_LEGEND.setWrapText(true);
        groupE_DOWN_LEGEND.setWrapText(true);
        groupF_notpresent_LEGEND.setWrapText(true);
        groupA_UP_LEGEND.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
        groupB_sigUP_LEGEND.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
        groupC_same_LEGEND.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
        groupD_sigDOWN_LEGEND.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
        groupE_DOWN_LEGEND.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
        groupF_notpresent_LEGEND.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);        
        
        // Make room for the header which will be added later
        
        Row row = null;
        int headerrows=(statisticalTest.equalsIgnoreCase("Binomial"))?10:9;
        if (includeLegend) {
            for (int j=0;j<headerrows;j++) {
               row = sheet.createRow(j); 
            }
            rownum=headerrows-1; // -1 because it will be incremented later on...
        }        
        int col=0;
        int logocolumn=0;
        row = sheet.createRow(rownum);
        outputStringValuesInCells(row, new String[]{"Motif ID","Name","Class","Target","Control","P-value target","P-value control","Group"}, 0, tableheader);      
        col+=8;
        if (showSequenceLogos) {
            logocolumn=col;
            outputStringValuesInCells(row, new String[]{"Logo"}, logocolumn, tableheader);
            sheet.setColumnWidth(logocolumn, 10000);            
        }
  
        int maxlogowidth=0; // the number of bases in the longest motif     
        if (showLogosAsImages) sheet.setColumnWidth(logocolumn, 10000);
        ArrayList<String> motifs=new ArrayList<String>(results.size());
        if (include!=null) {
           for (String entry:include.getValues()) {
               if (results.containsKey(entry)) motifs.add(entry);
           }
        } else motifs.addAll(results.keySet());        
        if (sortorder.equalsIgnoreCase(SORT_BY_EXPRESSION)) Collections.sort(motifs,new SortOrderComparator());
        else Collections.sort(motifs);
        int prog=0;
        for (String motifname:motifs) {
            rownum++;
            row = sheet.createRow(rownum);
            col=0;            
            prog++;
            String shortname="";
            String motifclass="";            
            Motif motif=null;
            if (engine.dataExists(motifname, Motif.class)) {
                motif=(Motif)engine.getDataItem(motifname);
                motifclass=motif.getClassification();
                if (motifclass==null) motifclass="unknown";  
                shortname=motif.getShortName();
            }             
            Double[] value=results.get(motifname);   
            String group="F";
            CellStyle style=null;
                 if (value[0]==OVERREPRESENTED_IN_TARGET_BY_DEFAULT) {style=groupA_UP;group="A";}
            else if (value[0]==OVERREPRESENTED_IN_CONTROL_BY_DEFAULT) {style=groupE_DOWN;group="E";}
            else if (value[0]==OVERREPRESENTED_IN_TARGET) {style=groupB_sigUP;group="B";}
            else if (value[0]==OVERREPRESENTED_IN_CONTROL) {style=groupD_sigDOWN;group="D";}
            else if (value[0]==SAME_IN_BOTH_SETS) {style=groupC_same;group="C";}
            else {style=groupF_notpresent;group="F";} // group F                 
            outputStringValuesInCells(row, new String[]{motifname,shortname,motifclass}, col);
            col+=3;
            outputNumericValuesInCells(row, new double[]{value[1],value[2],value[3],value[4]}, col, style);
            col+=4;
            outputStringValueInCell(row, col, group, style);
            col+=1;
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
            if (task!=null) task.setStatusMessage("Executing operation: output ("+prog+"/"+motifs.size()+")");
            format.setProgress(prog,motifs.size());
        }           
        format.setProgress(95);  
        for (short i=0;i<col;i++) {
            sheet.autoSizeColumn(i);               
        }
        if (!showLogosAsImages) sheet.autoSizeColumn((short)logocolumn);   
        
        // Add the header on top of the page
        if (includeLegend) { 
            int[] counts=new int[6]; // count the number of motifs that fall in each of the 6 classes
            for (String motifname:results.keySet()) {
                Double[] value=results.get(motifname);
                counts[value[0].intValue()]++;
            }            
            sheet.createFreezePane(0,headerrows,0,headerrows);
            row=sheet.getRow(0);
            outputStringValueInCell(row, 0, "Motif occurrence analysis for \""+targetSetName+"\" vs \""+controlSetName+"\"", title);
            StringBuilder firstLine=new StringBuilder();
            if (motifCollectionName!=null) {
               firstLine.append("The analysis was performed with motifs from \"");
               firstLine.append(motifCollectionName);
               firstLine.append("\" on binding sites from \"");              
            } else {
               firstLine.append("The analysis was performed on binding sites from \"");              
            }
            firstLine.append(motifsTrackName);
            firstLine.append("\"");
            if (withinRegionsTrackName!=null) {            
                firstLine.append(" within \">");
                firstLine.append(withinRegionsTrackName);
                firstLine.append("\"");
            }
            firstLine.append(".");
            row=sheet.getRow(2);
            outputStringValueInCell(row, 0, firstLine.toString(), null);  
            
            StringBuilder secondLine=new StringBuilder();
            secondLine.append("Statistical significance evaluated using a "+statisticalTest.toLowerCase()+" test with p-value threshold="+significanceThreshold);
            if (bonferroniStrategy.equals(ALL_MOTIFS)) secondLine.append(" (Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering all "+multiplehypothesis+" motifs tested)");
            if (bonferroniStrategy.equals(PRESENT_MOTIFS)) secondLine.append(" (Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering "+multiplehypothesis+" present motifs)");
            secondLine.append(".");
            row=sheet.getRow(3);
            outputStringValueInCell(row, 0, secondLine.toString(), null);  
                       
            if (statisticalTest.equalsIgnoreCase("Binomial")) {
                row=sheet.getRow(5);
                outputStringValueInCell(row, 0, "Motifs present\nonly in target", groupA_UP_LEGEND);
                outputStringValueInCell(row, 1, "Motifs overrepresented\nin target",groupB_sigUP_LEGEND);
                outputStringValueInCell(row, 2, "Same rate",groupC_same_LEGEND);
                outputStringValueInCell(row, 3, "Motifs overrepresented\nin control",groupD_sigDOWN_LEGEND);                
                outputStringValueInCell(row, 4, "Motifs present\nonly in control",groupE_DOWN_LEGEND);
                outputStringValueInCell(row, 5, "Not present",groupF_notpresent_LEGEND);                
                row=sheet.getRow(6);
                outputNumericValueInCell(row, 0, counts[(int)OVERREPRESENTED_IN_TARGET_BY_DEFAULT], groupA_UP_LEGEND);
                outputNumericValueInCell(row, 1, counts[(int)OVERREPRESENTED_IN_TARGET], groupB_sigUP_LEGEND);
                outputNumericValueInCell(row, 2, counts[(int)SAME_IN_BOTH_SETS], groupC_same_LEGEND);
                outputNumericValueInCell(row, 3, counts[(int)OVERREPRESENTED_IN_CONTROL_BY_DEFAULT], groupD_sigDOWN_LEGEND);
                outputNumericValueInCell(row, 4, counts[(int)OVERREPRESENTED_IN_CONTROL], groupE_DOWN_LEGEND);
                outputNumericValueInCell(row, 5, counts[(int)NOT_PRESENT], groupF_notpresent_LEGEND);
                row=sheet.getRow(7);
                outputNumericValueInCell(row, 0, counts[(int)OVERREPRESENTED_IN_TARGET]+counts[(int)OVERREPRESENTED_IN_TARGET_BY_DEFAULT], groupB_sigUP_LEGEND);
                outputNumericValueInCell(row, 3, counts[(int)OVERREPRESENTED_IN_CONTROL]+counts[(int)OVERREPRESENTED_IN_CONTROL_BY_DEFAULT], groupD_sigDOWN_LEGEND);
                CellRangeAddress[] merged=new CellRangeAddress[]{
                    new CellRangeAddress(6,7,2,2),
                    new CellRangeAddress(6,7,5,5),
                    new CellRangeAddress(7,7,0,1),
                    new CellRangeAddress(7,7,3,4)
                };
                for (CellRangeAddress range:merged) {
                    sheet.addMergedRegion(range);
                    RegionUtil.setBorderBottom(HSSFCellStyle.BORDER_THIN, range, sheet, workbook); // borders must be updated on merged cells
                    RegionUtil.setBorderTop(HSSFCellStyle.BORDER_THIN, range, sheet, workbook); // borders must be updated on merged cells
                    RegionUtil.setBorderLeft(HSSFCellStyle.BORDER_THIN, range, sheet, workbook); // borders must be updated on merged cells
                    RegionUtil.setBorderRight(HSSFCellStyle.BORDER_THIN, range, sheet, workbook); // borders must be updated on merged cells                                   
                }               
                int[] widths=new int[]{3500,5000,3500,5000,3500,3500};
                for (int i=0;i<widths.length;i++) {
                    int currentwidth=sheet.getColumnWidth(i);
                    sheet.setColumnWidth(i,Math.max(currentwidth,widths[i]));                    
                }            
             } else if (statisticalTest.equalsIgnoreCase("Hypergeometric")) {
                row=sheet.getRow(5);
                outputStringValueInCell(row, 0, "Motifs overrepresented\nin target",groupB_sigUP_LEGEND);
                outputStringValueInCell(row, 1, "Same rate",groupC_same_LEGEND);
                outputStringValueInCell(row, 2, "Motifs overrepresented\nin control",groupD_sigDOWN_LEGEND);                
                outputStringValueInCell(row, 3, "Not present",groupF_notpresent_LEGEND);                
                row=sheet.getRow(6);
                outputNumericValueInCell(row, 0, counts[(int)OVERREPRESENTED_IN_TARGET], groupB_sigUP_LEGEND);
                outputNumericValueInCell(row, 1, counts[(int)SAME_IN_BOTH_SETS], groupC_same_LEGEND);
                outputNumericValueInCell(row, 2, counts[(int)OVERREPRESENTED_IN_CONTROL], groupD_sigDOWN_LEGEND);
                outputNumericValueInCell(row, 3, counts[(int)NOT_PRESENT], groupF_notpresent_LEGEND);
                int[] widths=new int[]{5000,3500,5000,3500};
                for (int i=0;i<widths.length;i++) {
                    int currentwidth=sheet.getColumnWidth(i);
                    sheet.setColumnWidth(i,Math.max(currentwidth,widths[i]));                    
                }  
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
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings outputSettings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        MotifCollection include=null;
        String sortorder=SORT_BY_MOTIF;
        String showSequenceLogosString=MOTIF_LOGO_NO;
        if (outputSettings!=null) {
          try {
                 Parameter[] defaults=getOutputParameters();
                 include=(MotifCollection)outputSettings.getResolvedParameter("Include",defaults,engine);
                 sortorder=(String)outputSettings.getResolvedParameter("Sort by",defaults,engine);
                 showSequenceLogosString=(String)outputSettings.getResolvedParameter("Logos",defaults,engine);
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos=(showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_NEW) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_SHARED) || showSequenceLogosString.equalsIgnoreCase(MOTIF_LOGO_TEXT));

        ArrayList<String> motifs=new ArrayList<String>(results.size());
        if (include!=null) {
           for (String entry:include.getValues()) {
               if (results.containsKey(entry)) motifs.add(entry);
           }
        } else motifs.addAll(results.keySet()); 
        if (sortorder.equalsIgnoreCase(SORT_BY_EXPRESSION)) Collections.sort(motifs,new SortOrderComparator());
        else Collections.sort(motifs);
        String withinString="";
        if (withinRegionsTrackName!=null) withinString=" within \""+withinRegionsTrackName+"\" regions";
        outputobject.append("# Motif occurrence comparison for \""+targetSetName+"\" (target) vs \""+controlSetName+"\" (control) on sites from \""+motifsTrackName+"\""+withinString+"\n",HTML);
        outputobject.append("# Statistical significance evaluated using a "+statisticalTest+" test with p-value threshold="+significanceThreshold+"\n",RAWDATA);
        if (bonferroniStrategy.equals(ALL_MOTIFS)) outputobject.append("# (Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering all "+multiplehypothesis+" motifs tested)",RAWDATA);
        if (bonferroniStrategy.equals(PRESENT_MOTIFS)) outputobject.append("# (Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering "+multiplehypothesis+" present motifs)",RAWDATA);
        outputobject.append("\n",RAWDATA);
        outputobject.append("#\n# Column 1 => Motif ID\n",RAWDATA);
        if (statisticalTest.equalsIgnoreCase("Binomial")) outputobject.append("# Column 2 => Number of motif occurrences in target set\n",RAWDATA);   
        else if (statisticalTest.equalsIgnoreCase("Hypergeometric")) outputobject.append("# Column 2 => Number of sequences containing motif in target set\n",RAWDATA);   
        outputobject.append("# Column 3 => Expression symbol: > overrepresented in target, < overrepresented in control, = same rate, - motif not present\n",RAWDATA);   
        if (statisticalTest.equalsIgnoreCase("Binomial")) outputobject.append("# Column 4 => Number of motif occurrences in control set\n",RAWDATA);   
        else if (statisticalTest.equalsIgnoreCase("Hypergeometric")) outputobject.append("# Column 4 => Number of sequences containing motif in control set\n",RAWDATA);   
        outputobject.append("# Column 5 => Calculated p-value for overrepresentation in target set\n",RAWDATA);   
        outputobject.append("# Column 6 => Calculated p-value for overrepresentation in control set\n",RAWDATA);   
        if (showSequenceLogos) outputobject.append("# Column 7 => motif consensus\n",RAWDATA);   
        outputobject.append("\n",RAWDATA);   
        int prog=0;
        for (String motifname:motifs) {
            prog++;
            //if (include!=null && !include.contains(motifname)) continue;            
            Data motif=engine.getDataItem(motifname);
            String motifpresentationname=motifname;
            if (motif!=null && motif instanceof Motif) motifpresentationname+="-"+((Motif)motif).getShortName();               
            Double[] value=results.get(motifname); 
            String valuestring=value[1].intValue()+"\t"+expressionResultSign[value[0].intValue()]+"\t"+(value[2].intValue())+"\t"+value[3]+"\t"+value[4];
            outputobject.append(motifpresentationname+"\t"+valuestring,RAWDATA);                    
            if (showSequenceLogos) {
                if (motif!=null && motif instanceof Motif) outputobject.append("\t"+((Motif)motif).getConsensusMotif(),RAWDATA);
                else outputobject.append("\t?",RAWDATA);
            }   
            outputobject.append("\n",RAWDATA);            
            format.setProgress((int)(prog*100f/(double)motifs.size()));
        }
        format.setProgress(100);
        return outputobject;
    }

    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        MotifCollection motifcollection=(MotifCollection)task.getParameter("Motifs");     
        RegionDataset motiftrack=(RegionDataset)task.getParameter("Motif track");  
        RegionDataset withinRegions=(RegionDataset)task.getParameter("Within regions");
        if (!motiftrack.isMotifTrack()) throw new ExecutionError(motiftrack.getName()+" is not a motif track");
        motifsTrackName=motiftrack.getName();
        motifCollectionName=motifcollection.getName();
        if (withinRegions==motiftrack) throw new ExecutionError("'Within regions' parameter should not be the same as the 'motif track' parameter");
        if (withinRegions!=null) withinRegions=withinRegions.flatten();
        if (withinRegions!=null) withinRegionsTrackName=withinRegions.getName();
        SequenceCollection targetSet=(SequenceCollection)task.getParameter("Target set");
        SequenceCollection controlSet=(SequenceCollection)task.getParameter("Control set");     
        if (targetSet.isEmpty()) throw new ExecutionError("Target sequence set is empty");
        if (controlSet.isEmpty()) throw new ExecutionError("Control sequence set is empty");
        targetSetName=targetSet.getName();
        controlSetName=controlSet.getName();
        bonferroniStrategy=(String)task.getParameter("Bonferroni correction");     
        statisticalTest=(String)task.getParameter("Statistical test");     
        Double sigDouble=(Double)task.getParameter("Significance threshold");     
        if (sigDouble!=null) significanceThreshold=sigDouble.doubleValue();
        
        MotifLabEngine engine=task.getEngine();
        ArrayList<String> sequenceNames=new ArrayList<String>(); // The names of all sequences in the target and control sets. Each sequence will only be included once!
        SequenceCollection allSequences=new SequenceCollection("all"); // the union of target and control sets (which might be overlapping). Each sequence will only be included once!
        for (int i=0;i<targetSet.size();i++) {
            Sequence seq=targetSet.getSequenceByIndex(i,engine);
            allSequences.addSequence(seq);
            sequenceNames.add(seq.getName());
        }
        for (int i=0;i<controlSet.size();i++) {
            Sequence seq=controlSet.getSequenceByIndex(i,engine);
            allSequences.addSequence(seq); // The addSequence method checks if the sequence is already present before adding, so each sequence will only be added once!
            if (!sequenceNames.contains(seq.getName())) sequenceNames.add(seq.getName());
        }  
       
        ArrayList<Motif> allmotifs=motifcollection.getAllMotifs(task.getEngine());         
        int allmotifsSize=allmotifs.size();
        HashSet<String> presentMotifs=null;
        if (bonferroniStrategy.equals(PRESENT_MOTIFS)) presentMotifs=new HashSet<String>();
 
        TaskRunner taskRunner=engine.getTaskRunner();
        task.setProgress(0L,allmotifs.size());
        long[] counters=new long[]{0,0,allmotifs.size()}; // counters[0]=motifs started, [1]=motifs completed, [2]=total number of motifs

        ArrayList<ProcessMotifTask> processTasks=new ArrayList<ProcessMotifTask>(allmotifs.size());
        for (int i=0;i<allmotifsSize;i++) {
            String motifname=allmotifs.get(i).getName();
            Data data=engine.getDataItem(motifname);
            if (data==null || !(data instanceof Motif)) throw new ExecutionError(motifname+" is not a known motif");
            Motif motif=(Motif)data;
            processTasks.add(new ProcessMotifTask(motif, sequenceNames, targetSet, controlSet, allSequences, motiftrack, withinRegions, allmotifsSize, presentMotifs, task, counters));
        }
        List<Future<Motif>> futures=null;
        int countOK=0;            
        try {
            futures=taskRunner.invokeAll(processTasks); // this call apparently blocks until all tasks finish (either normally or by exceptions or being cancelled)                             
            for (Future<Motif> future:futures) {
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
        if (countOK!=allmotifs.size()) {
            throw new ExecutionError("Some mysterious error occurred while performing analysis: "+getAnalysisName());
        }              
                      
        storage=null; // release temporary counts
    }

    
    private Double[] processMotif(Motif motif, ArrayList<String> sequenceNames, SequenceCollection targetSet, SequenceCollection controlSet, SequenceCollection allSequences, RegionDataset motiftrack, RegionDataset withinRegions, int allmotifsSize, HashSet<String> presentMotifs, MotifLabEngine engine) throws Exception {
        String motifname=motif.getName();
        // first count the number of times the (current) motif occurs in each sequence (and within limits) and store these values
        for (int j=0;j<sequenceNames.size();j++) {
            RegionSequenceData seq=(RegionSequenceData)motiftrack.getSequenceByName(sequenceNames.get(j));
            RegionSequenceData withinRegionsSeq=null;
            if (withinRegions!=null) withinRegionsSeq=(RegionSequenceData)withinRegions.getSequenceByName(sequenceNames.get(j));
            int count=motifCountInSequence(motifname, seq, withinRegionsSeq);
            storeCount(seq.getSequenceName(), motifname, count);
        }  
        if (statisticalTest.equalsIgnoreCase("Binomial")) {
                int targetSetCount=totalMotifCountInCollection(motifname, targetSet);
                int controlSetCount=totalMotifCountInCollection(motifname, controlSet);
                if (bonferroniStrategy.equals(PRESENT_MOTIFS) && targetSetCount+controlSetCount>0) presentMotifs.add(motifname);
                int maximumPossibleOccurencesTarget=(withinRegions!=null)?maxOccurrences(motif.getLength(),targetSet,withinRegions,engine):maxOccurrences(motif.getLength(),targetSet,engine); // maximum number of positions this motif can create a hit (double stranded!)
                int maximumPossibleOccurencesControl=(withinRegions!=null)?maxOccurrences(motif.getLength(),targetSet,withinRegions,engine):maxOccurrences(motif.getLength(),controlSet,engine); // maximum number of positions this motif can create a hit (double stranded!)
                double expectedFrequencyFromControl=(double)controlSetCount/(double)maximumPossibleOccurencesControl;
                double expectedFrequencyFromTarget=(double)targetSetCount/(double)maximumPossibleOccurencesTarget;

                // test for overrepresentation in target set
                BinomialDistribution binomialTarget = new BinomialDistribution(maximumPossibleOccurencesTarget,expectedFrequencyFromControl);
                double pvalueOverrep=(1-binomialTarget.cumulativeProbability(targetSetCount))+binomialTarget.probability(targetSetCount);
                if (pvalueOverrep>1) pvalueOverrep=1.0; // this can happen apparently :(            

                // test for overrepresentation in control set (i.e. underrepresentation in target set)
                BinomialDistribution binomialControl = new BinomialDistribution(maximumPossibleOccurencesControl,expectedFrequencyFromTarget);
                double pvalueUnderrep=(1-binomialControl.cumulativeProbability(controlSetCount))+binomialControl.probability(controlSetCount);
                if (pvalueUnderrep>1) pvalueUnderrep=1.0; // this can happen apparently :(

                     if (bonferroniStrategy.equals(ALL_MOTIFS)) multiplehypothesis=allmotifsSize;
                else if (bonferroniStrategy.equals(PRESENT_MOTIFS)) multiplehypothesis=presentMotifs.size();
                double correctedThreshold=significanceThreshold/(double)multiplehypothesis;    

                double expressionresult=0;
                     if (targetSetCount==0 && controlSetCount==0) expressionresult=NOT_PRESENT;
                else if (targetSetCount>0 && controlSetCount==0) expressionresult=OVERREPRESENTED_IN_TARGET_BY_DEFAULT;
                else if (targetSetCount==0 && controlSetCount>0) expressionresult=OVERREPRESENTED_IN_CONTROL_BY_DEFAULT;
                else if (pvalueOverrep<=correctedThreshold) expressionresult=OVERREPRESENTED_IN_TARGET;
                else if (pvalueUnderrep<=correctedThreshold) expressionresult=OVERREPRESENTED_IN_CONTROL;
                else expressionresult=SAME_IN_BOTH_SETS;
                return new Double[]{expressionresult,(double)targetSetCount,(double)controlSetCount,(double)pvalueOverrep,(double)pvalueUnderrep};                   
        } else if (statisticalTest.equalsIgnoreCase("Hypergeometric")) {
                int targetSetCount=countSequencesWithMotifInSequenceCollection(motifname, targetSet); // number of sequences in target set containing this motif
                int controlSetCount=countSequencesWithMotifInSequenceCollection(motifname, controlSet); // number of sequences in control set containing this motif
                int unionCount=countSequencesWithMotifInSequenceCollection(motifname,allSequences); // total number of sequences containing this motif. Note: Because the target set and control set could overlap we cannot simply add together the two previous counts.
                if (bonferroniStrategy.equals(PRESENT_MOTIFS) && unionCount>0) presentMotifs.add(motifname);
                HypergeometricDistribution hypergeomTarget = new HypergeometricDistribution(allSequences.size(), unionCount, targetSet.size());
                HypergeometricDistribution hypergeomControl = new HypergeometricDistribution(allSequences.size(), unionCount, controlSet.size());
                // test for overrepresentation in target set
                double pvalueOverrep=hypergeomTarget.upperCumulativeProbability(targetSetCount);
                if (pvalueOverrep>1) pvalueOverrep=1.0; // this can happen apparently :(            
                // test for overrepresentation in control set (i.e. underrepresentation in target set)
                double pvalueUnderrep=hypergeomControl.upperCumulativeProbability(controlSetCount);
                if (pvalueUnderrep>1) pvalueUnderrep=1.0; // this can happen apparently :(

                     if (bonferroniStrategy.equals(ALL_MOTIFS)) multiplehypothesis=allmotifsSize;
                else if (bonferroniStrategy.equals(PRESENT_MOTIFS)) multiplehypothesis=presentMotifs.size();
                double correctedThreshold=significanceThreshold/(double)multiplehypothesis;    

                double expressionresult=0;
                     if (targetSetCount==0 && controlSetCount==0) expressionresult=NOT_PRESENT;
                else if (pvalueOverrep<=correctedThreshold) expressionresult=OVERREPRESENTED_IN_TARGET;
                else if (pvalueUnderrep<=correctedThreshold) expressionresult=OVERREPRESENTED_IN_CONTROL; 
                else expressionresult=SAME_IN_BOTH_SETS;
                return new Double[]{expressionresult,(double)targetSetCount,(double)controlSetCount,(double)pvalueOverrep,(double)pvalueUnderrep};                          
        } else throw new ExecutionError("Unknown statistical test: "+statisticalTest);        
    }
    
    
    protected class ProcessMotifTask implements Callable<Motif> {
        final Motif motif;
        final SequenceCollection targetSet;
        final SequenceCollection controlSet;
        final SequenceCollection allSequences;
        final ArrayList<String> sequenceNames;
        final RegionDataset motiftrack;
        final RegionDataset withinRegions;
        final int allmotifsSize;
        final HashSet<String> presentMotifs;
        final long[] counters; // counters[0]=motifs started, [1]=motifs completed, [2]=total number of motifs.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final OperationTask task;  
        
        public ProcessMotifTask(Motif motif, ArrayList<String> sequenceNames, SequenceCollection targetSet, SequenceCollection controlSet, SequenceCollection allSequences, RegionDataset motiftrack, RegionDataset withinRegions, int allmotifsSize, HashSet<String> presentMotifs, OperationTask task, long[] counters) {
           this.motif=motif;
           this.targetSet=targetSet;
           this.controlSet=controlSet;
           this.allSequences=allSequences;
           this.motiftrack=motiftrack;
           this.withinRegions=withinRegions;
           this.allmotifsSize=allmotifsSize;
           this.presentMotifs=presentMotifs;
           this.sequenceNames=sequenceNames;
           this.counters=counters;
           this.task=task;
        }
         
        @Override
        @SuppressWarnings("unchecked")
        public Motif call() throws Exception {
            synchronized(counters) {
               counters[0]++; // number of sequences started  
            }        
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            
            Double[] counts=processMotif(motif, sequenceNames, targetSet, controlSet, allSequences, motiftrack, withinRegions, allmotifsSize, presentMotifs, task.getEngine());           
            
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                results.put(motif.getName(),counts);
                task.setStatusMessage("Executing analysis: "+getAnalysisName()+"  ("+counters[1]+"/"+counters[2]+")");
                task.setProgress(counters[1],counters[2]);                              
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();    
            return motif;
        }   
    }    
    
    
     /** Returns TRUE if the selected region is fully within any one of the regions in the provided Sequence */
    private boolean isRegionInsideOtherRegions(Region region, RegionSequenceData sequence) {
        for (Region other:sequence.getOriginalRegions()) {
            if (region.getRelativeStart()>=other.getRelativeStart() && region.getRelativeEnd()<=other.getRelativeEnd()) return true;
        }
        return false;
    }
 
    /** Counts the number of times a motif occurs in the given sequence (and also within the regions in 'withinRegionsSequence' if provided) */
    private int motifCountInSequence(String motifname, RegionSequenceData sequencedata, RegionSequenceData withinRegionsSequence) {
        int count=0;
        for (Region region:sequencedata.getAllRegions()) {
            boolean matchingType=region.getType().equals(motifname);
            if (matchingType) {
                if (withinRegionsSequence==null) count++;
                else if (isRegionInsideOtherRegions(region, withinRegionsSequence)) count++;
            }
        }        
        return count;
    }    
    
    private int totalMotifCountInCollection(String motifname, SequenceCollection collection) {
        int total=0;
        for (int i=0;i<collection.size();i++) {
            String seqname=collection.getSequenceNameByIndex(i);
            total+=getCount(seqname, motifname);
        }
        return total;
    }
    
    /** Returns the number of sequences from the sequence collection that contains the motif */
    private int countSequencesWithMotifInSequenceCollection(String motifname, SequenceCollection collection) {
        int total=0;
        for (int i=0;i<collection.size();i++) {
            String seqname=collection.getSequenceNameByIndex(i);
            if (getCount(seqname, motifname)>0) total++;
        }
        return total;
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
    
//    /** Calculates a background frequency for the given motif based on its presence in a background sequence set*/
//    private double getBackgroundFrequency(Motif motif, SequenceCollection backgroundset, MotifLabEngine engine) throws ExecutionError {
//        int observedInBackground=totalMotifCountInCollection(motif.getName(),backgroundset);
//        int maximum=maxOccurrences(motif.getLength(), backgroundset, engine);
//        return (double)observedInBackground/(double)maximum;      
//    }


    
    /** Returns a list of output parameters that can be set when an Analysis is output */
    @Override
    public Parameter[] getOutputParameters() {
        return new Parameter[] {
             new Parameter("Include",MotifCollection.class,null,new Class[]{MotifCollection.class},"Only include data from this collection",false,false),
             new Parameter("Sort by",String.class,SORT_BY_EXPRESSION,new String[]{SORT_BY_MOTIF,SORT_BY_EXPRESSION},null,false,false),
             new Parameter("Logos",String.class,MOTIF_LOGO_NO, new String[]{MOTIF_LOGO_NO,MOTIF_LOGO_NEW,MOTIF_LOGO_SHARED,MOTIF_LOGO_TEXT},"Include sequence logos in the table",false,false),
             new Parameter("Color boxes",Boolean.class,Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a box with the assigned color for the motif will be output as the first column",false,false),
             new Parameter("Legend",Boolean.class,Boolean.TRUE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a header with a title and analysis details will be included at the top of the Excel sheet.",false,false)       
        };
    }
    
    @Override
    public String[] getOutputParameterFilter(String parameter) {
        if (parameter.equals("Color boxes")) return new String[]{HTML};
        if (parameter.equals("Legend")) return new String[]{EXCEL};
        if (parameter.equals("Include") || parameter.equals("Logos") || parameter.equals("Sort by")) return new String[]{HTML,RAWDATA,EXCEL};        
        return null;
    }       
    
    private class SortOrderComparator implements Comparator<String> {
        private ExpressionOrderComparator expressionComparator=new ExpressionOrderComparator();
        
        @Override
        public int compare(String motif1, String motif2) { // these are two motifnames
            Double[] value1=results.get(motif1);
            Double[] value2=results.get(motif2);                
            Double expression1=value1[0]; // this is the expression class
            Double expression2=value2[0]; // this is the expression class
            if (expression1==null && expression2==null) return 0;
            if (expression1==null) return 1;
            if (expression2==null) return -1;
            if (Double.isNaN(expression1) && Double.isNaN(expression2)) return 0;
            if (Double.isNaN(expression1)) return 1;
            if (Double.isNaN(expression2)) return -1; 
            int comp=expressionComparator.compare(expression1, expression2);
            if (comp!=0) return comp;
            else { // same expression group. Compare p-values
                if (expression1<=1) return motif1.compareTo(motif2); // use name if expression is the same
                int index=(expression1>=4)?3:4; // index of p-value for overrepresented set (could be target(3) or control(4))
                Double pvalue1=value1[index];
                Double pvalue2=value2[index];
                if (pvalue1==null && pvalue2==null) return 0;
                if (pvalue1==null) return 1;
                if (pvalue2==null) return -1;
                if (Double.isNaN(pvalue1) && Double.isNaN(pvalue2)) return 0;
                if (Double.isNaN(pvalue1)) return 1;
                if (Double.isNaN(pvalue2)) return -1;                      
                     if (pvalue1<pvalue2) return -1;
                else if (pvalue1>pvalue2) return 1;
                else { // same p-value. compare counts, if if that fails sort by name
                    int index2=(expression1>=4)?1:2;
                    if (value1[index2]>value2[index2]) return -1;
                    else if (value1[index2]<value2[index2]) return 1;
                    else return motif1.compareTo(motif2);
                }
            }
            //return motif1.compareTo(motif2); // use name if expression is the same
        }    
    }

    private class ExpressionOrderComparator implements Comparator<Double> {
        @Override
        public int compare(Double value1, Double value2) { //
            double v1=value1.doubleValue();
            double v2=value2.doubleValue();
            if (v1==v2) return 0;
            else if (v1==OVERREPRESENTED_IN_TARGET_BY_DEFAULT) return -1;
            else if (v1==OVERREPRESENTED_IN_TARGET && v2!=OVERREPRESENTED_IN_TARGET_BY_DEFAULT) return -1;
            else if (v2==NOT_PRESENT) return -1;
            else if (v1==SAME_IN_BOTH_SETS && (v2==OVERREPRESENTED_IN_CONTROL || v2==OVERREPRESENTED_IN_CONTROL_BY_DEFAULT)) return -1;
            else if (v1==OVERREPRESENTED_IN_CONTROL &&  v2==OVERREPRESENTED_IN_CONTROL_BY_DEFAULT) return -1;
            else return 1;
        }
    }




    @Override
    protected JPanel getDisplayPanel(MotifLabGUI gui, boolean modal) {
        OutputData header=new OutputData("temp");
        gui.getEngine().createInternalHTMLheader(header);
        formatSummary(header, true);
        header.append("<br /></center></body></html>",HTML);
        header.setShowAsHTML(true);
        OutputPanel headerpanel=new OutputPanel(header, gui);
        MotifOccurrenceTableModel tablemodel=new MotifOccurrenceTableModel(gui);
        GenericMotifBrowserPanel tablepanel=new GenericMotifBrowserPanel(gui, tablemodel, modal);
        JTable table=tablepanel.getTable();
        CellRenderer_Color renderer=new CellRenderer_Color(gui.getVisualizationSettings());
        table.setDefaultRenderer(Integer.class, renderer);
        table.setDefaultRenderer(Double.class, renderer);        
        table.getColumn("Logo").setPreferredWidth(220);
        table.getColumn("Class").setCellRenderer(new CellRenderer_Classification());
        table.getColumn("Class").setPreferredWidth(50);        
        JCheckBox groupByExpressionCheckbox=new JCheckBox("Group by expression", true);
        final GroupRowSorter<MotifOccurrenceTableModel, Integer> sorter=new GroupRowSorter<MotifOccurrenceTableModel, Integer>(tablemodel, groupByExpressionCheckbox, MotifOccurrenceTableModel.EXPRESSION, new ExpressionOrderComparator());
        table.setRowSorter(sorter);
        sorter.setSortsOnUpdates(true);
        Comparator<Motif> logocomparator=tablepanel.getMotifLogoComparator();       
        for (int i=0;i<table.getColumnCount();i++) {
            Class columnclass=tablemodel.getColumnClass(i);
            if (columnclass==Motif.class) {
                ((GroupRowSorter)table.getRowSorter()).setComparator(i, logocomparator);                
            } else if (columnclass==String.class) {
                ((GroupRowSorter)table.getRowSorter()).setComparator(i, MotifLabEngine.getNaturalSortOrderComparator(true));
            }
        }        
        groupByExpressionCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sorter.allRowsChanged(); // this is done to update the sorting
            }
        });
        table.removeColumn(table.getColumn("Expression")); // hide the expression column. We do not need it in the view, only in the model
        table.getRowSorter().toggleSortOrder(MotifOccurrenceTableModel.TARGET);
        table.getRowSorter().toggleSortOrder(MotifOccurrenceTableModel.TARGET);
        JPanel buttonsPanel=tablepanel.getControlsPanel();
        buttonsPanel.add(new javax.swing.JLabel("   "));
        buttonsPanel.add(groupByExpressionCheckbox);
        JPanel panel=new JPanel(new BorderLayout());
        panel.add(headerpanel,BorderLayout.NORTH);
        panel.add(tablepanel,BorderLayout.CENTER);
        panel.setPreferredSize(new java.awt.Dimension(900,600));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8,5,0,5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)
        ));
        return panel;

    }

private class MotifOccurrenceTableModel extends AbstractTableModel {
    private String[] columnNames={" ","ID","Name","Class","Target","Control","p-value target","p-value control","Logo","Expression"};
    private String[] motifnames=null;
    private MotifLabEngine engine;
    private VisualizationSettings settings;
    public static final int COLOR=0;    
    public static final int ID=1;
    public static final int NAME=2;
    public static final int CLASS=3;
    public static final int TARGET=4;
    public static final int CONTROL=5;
    public static final int P_VALUE_TARGET=6;
    public static final int P_VALUE_CONTROL=7;
    public static final int LOGO=8;
    public static final int EXPRESSION=9;

    public MotifOccurrenceTableModel(MotifLabGUI gui) {
        engine=gui.getEngine();
        settings=gui.getVisualizationSettings();
        motifnames=new String[results.size()];
        int i=0;
        for (String name:results.keySet()) {
           motifnames[i]=name;
           i++;
        }
    }


    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case COLOR:return Color.class;            
            case ID:return String.class;
            case NAME:return String.class;
            case CLASS:return String.class;                
            case TARGET:return Integer.class;
            case CONTROL:return Integer.class;
            case P_VALUE_TARGET:return Double.class;
            case P_VALUE_CONTROL:return Double.class;
            case LOGO:return Motif.class;
            case EXPRESSION:return Double.class;
            default:return Object.class;
        }
    }

    public Motif getMotif(String id) {
        Data data=engine.getDataItem(id);
        if (data instanceof Motif) return (Motif)data;
        else return null;
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
            case CLASS:return getMotifClass(motifnames[rowIndex]);                
            case TARGET:return ((Double)results.get(motifnames[rowIndex])[1]).intValue();
            case CONTROL:return ((Double)results.get(motifnames[rowIndex])[2]).intValue();
            case P_VALUE_TARGET:return ((Double)results.get(motifnames[rowIndex])[3]);
            case P_VALUE_CONTROL:return ((Double)results.get(motifnames[rowIndex])[4]);
            case LOGO:return getMotif(motifnames[rowIndex]);
            case EXPRESSION:return ((Double)results.get(motifnames[rowIndex])[0]);
            default:return Object.class;
        }
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

   private class CellRenderer_Color extends DefaultTableCellRenderer {
      private java.awt.Color OVERREP_TARGET_DEFAULT_COLOR;
      private java.awt.Color OVERREP_TARGET_COLOR;
      private java.awt.Color OVERREP_CONTROL_DEFAULT_COLOR;
      private java.awt.Color OVERREP_CONTROL_COLOR;
      private java.awt.Color SAME_RATE_COLOR;
      private java.awt.Color NOT_PRESENT_COLOR;
      private java.awt.Color OVERREP_TARGET_DEFAULT_SELECTED_COLOR;
      private java.awt.Color OVERREP_TARGET_SELECTED_COLOR;
      private java.awt.Color OVERREP_CONTROL_DEFAULT_SELECTED_COLOR;
      private java.awt.Color OVERREP_CONTROL_SELECTED_COLOR;
      private java.awt.Color SAME_RATE_SELECTED_COLOR;
      private java.awt.Color NOT_PRESENT_SELECTED_COLOR;



      public CellRenderer_Color(VisualizationSettings settings) {
           OVERREP_TARGET_DEFAULT_COLOR=settings.getSystemColor("onlyintarget");
           OVERREP_TARGET_COLOR=settings.getSystemColor("overrepintarget");
           OVERREP_CONTROL_DEFAULT_COLOR=settings.getSystemColor("onlyincontrol");
           OVERREP_CONTROL_COLOR=settings.getSystemColor("overrepincontrol");
           SAME_RATE_COLOR=settings.getSystemColor("samerate");
           NOT_PRESENT_COLOR=settings.getSystemColor("notpresent");
           OVERREP_TARGET_DEFAULT_SELECTED_COLOR=OVERREP_TARGET_DEFAULT_COLOR.darker().darker();
           OVERREP_TARGET_SELECTED_COLOR=OVERREP_TARGET_COLOR.darker().darker();
           OVERREP_CONTROL_DEFAULT_SELECTED_COLOR=OVERREP_CONTROL_DEFAULT_COLOR.darker().darker();
           OVERREP_CONTROL_SELECTED_COLOR=OVERREP_CONTROL_COLOR.darker().darker();
           SAME_RATE_SELECTED_COLOR=SAME_RATE_COLOR.darker().darker();
           NOT_PRESENT_SELECTED_COLOR=NOT_PRESENT_COLOR.darker().darker();

      }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c=super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String motifID=(String)table.getValueAt(row, MotifOccurrenceTableModel.ID);
            double group=results.get(motifID)[0];
            if (isSelected) {
                if (group==OVERREPRESENTED_IN_TARGET_BY_DEFAULT) c.setBackground(OVERREP_TARGET_DEFAULT_SELECTED_COLOR);
                else if (group==OVERREPRESENTED_IN_TARGET) c.setBackground(OVERREP_TARGET_SELECTED_COLOR);
                else if (group==OVERREPRESENTED_IN_CONTROL_BY_DEFAULT) c.setBackground(OVERREP_CONTROL_DEFAULT_SELECTED_COLOR);
                else if (group==OVERREPRESENTED_IN_CONTROL) c.setBackground(OVERREP_CONTROL_SELECTED_COLOR);
                else if (group==SAME_IN_BOTH_SETS) c.setBackground(SAME_RATE_SELECTED_COLOR);
                else if (group==NOT_PRESENT) c.setBackground(NOT_PRESENT_SELECTED_COLOR);
                else c.setBackground(table.getSelectionBackground());
            } else {
                if (group==OVERREPRESENTED_IN_TARGET_BY_DEFAULT) c.setBackground(OVERREP_TARGET_DEFAULT_COLOR);
                else if (group==OVERREPRESENTED_IN_TARGET) c.setBackground(OVERREP_TARGET_COLOR);
                else if (group==OVERREPRESENTED_IN_CONTROL_BY_DEFAULT) c.setBackground(OVERREP_CONTROL_DEFAULT_COLOR);
                else if (group==OVERREPRESENTED_IN_CONTROL) c.setBackground(OVERREP_CONTROL_COLOR);
                else if (group==SAME_IN_BOTH_SETS) c.setBackground(SAME_RATE_COLOR);
                else if (group==NOT_PRESENT) c.setBackground(NOT_PRESENT_COLOR);
                else c.setBackground(table.getBackground());
            }
            if (c instanceof javax.swing.JLabel) {
                ((javax.swing.JLabel)c).setHorizontalAlignment(javax.swing.JLabel.RIGHT);
                if (value instanceof Double) {
                    ((javax.swing.JLabel)c).setText(Graph.formatNumber((Double)value, false));
                }
            }
            return c;
        }

}// end class CellRenderer_Color
   
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
