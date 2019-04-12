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
import java.util.Iterator;
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
import motiflab.gui.GenericRegionBrowserPanel;
import motiflab.gui.OutputPanel;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.VisualizationSettings;
import org.apache.poi.hslf.model.Picture;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.RegionUtil;

/**
 *
 * @author kjetikl
 */
public class CompareRegionOccurrencesAnalysis extends Analysis {
    
    private static String typedescription="Analysis: compare region occurrences";
    private static String analysisName="compare region occurrences";
    private static String description="Compares the distribution of regions within two sequence collections to find which region types are over- and underrepresented";
    private transient HashMap<String,HashMap<String,Integer>> storage=new HashMap<String,HashMap<String,Integer>>(); // temporary storage for region counts during runAnalysis
    private double significanceThreshold=0.05;
    private int multiplehypothesis=1; // number of regions counted in bonferroni correction
    private String bonferroniStrategy=PRESENT_TYPES;
    private String statisticalTest="Hypergeometric";
    private String regionsTrackName=null;
    private String targetSetName=null;
    private String controlSetName=null;
    private static double OVERREPRESENTED_IN_TARGET_BY_DEFAULT=5; // these are only present in target
    private static double OVERREPRESENTED_IN_TARGET=4;
    private static double OVERREPRESENTED_IN_CONTROL_BY_DEFAULT=3;  // these are only present in control
    private static double OVERREPRESENTED_IN_CONTROL=2;
    private static double SAME_IN_BOTH_SETS=1;
    private static double NOT_PRESENT=0;
    private static char[] expressionResultSign=new char[]{'-','=','<','<','>','>'};
            
    private static final String NONE="None";
    private static final String PRESENT_TYPES="Present types";

    private HashMap<String,Double[]> results=new HashMap<String,Double[]>(); // stores region name and a result-array for that type
    // the results are stored in a Double array for easy access and cloning
    // the Double[] contains 5 values:
      

    public CompareRegionOccurrencesAnalysis() {
        this.name="CompareRegionOccurrencesAnalysis_temp";
        addParameter("Region track",RegionDataset.class, null,new Class[]{RegionDataset.class},"A Region Dataset",true,false);
        addParameter("Target set",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"A set of sequences (for instance a set of upregulated sequences)",true,false);
        addParameter("Control set",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"A second set of sequences to compare the first set against (for instance a set of downregulated sequences)",true,false);
        addParameter("Statistical test",String.class, "Hypergeometric",new String[]{"Hypergeometric"},"The statistical test used to compute p-values",true,false);
        addParameter("Significance threshold",Double.class, new Double(0.05),new Double[]{0.0,1.0},"The initial p-value threshold to use when estimating significance",true,false);
        addParameter("Bonferroni correction",String.class, PRESENT_TYPES,new String[]{NONE,PRESENT_TYPES},"If selected, the initial p-value threshold will be adjusted according to the given number of regions",true,false);        
    }  
    
    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Region track","Target set","Control set"};}   
    
    @Override
    public boolean canUseAsSourceProxy(Data data) {
        if (data instanceof RegionDataset) return !(((RegionDataset)data).isMotifTrack() || ((RegionDataset)data).isModuleTrack()); // this is not used for Motif or Module tracks
        else return (data instanceof SequenceCollection);
    }      
    
    @Override
    public String getAnalysisName() {
        return analysisName;
    }
     
    @Override
    public String getDescription() {return description;}
    
    private final String[] variables = new String[]{"Overrepresented in target","Overrepresented in control","Present only in target","Present only in control","Same expression","All present","corrected threshold"};

    @Override
    public String[] getResultVariables() {
        return variables;
    }

    @Override    
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename.equals("corrected threshold")) return new NumericVariable("temp",significanceThreshold/(double)multiplehypothesis);
        else {
            TextVariable collection=new TextVariable("comparetemp");
                 if (variablename.equals("Overrepresented in target"))  {addRegionsToCollection(collection,OVERREPRESENTED_IN_TARGET,true,engine);addRegionsToCollection(collection,OVERREPRESENTED_IN_TARGET_BY_DEFAULT,true,engine);}
            else if (variablename.equals("Overrepresented in control")) {addRegionsToCollection(collection,OVERREPRESENTED_IN_CONTROL,true,engine);addRegionsToCollection(collection,OVERREPRESENTED_IN_CONTROL_BY_DEFAULT,true,engine);}
            else if (variablename.equals("Present only in target")) addRegionsToCollection(collection,OVERREPRESENTED_IN_TARGET_BY_DEFAULT,true,engine);
            else if (variablename.equals("Present only in control")) addRegionsToCollection(collection,OVERREPRESENTED_IN_CONTROL_BY_DEFAULT,true,engine);
            else if (variablename.equals("Same expression")) addRegionsToCollection(collection,SAME_IN_BOTH_SETS,true,engine);
            else if (variablename.equals("All present regions")) addRegionsToCollection(collection,NOT_PRESENT,false,engine);
            else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
            return collection;
        }
        //throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }
    
    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       if (variablename.equals("corrected threshold")) return NumericVariable.class;
       else return TextVariable.class; // all other exported values in this analysis are TextVariable
    }      
    
    private void addRegionsToCollection(TextVariable collection, double expressionpattern, boolean hasexpression, MotifLabEngine engine) {        
        for (String regionname:results.keySet()) {
            Double[] value=results.get(regionname);
            if ((hasexpression && value[0]==expressionpattern) || (!hasexpression && value[0]!=expressionpattern)) collection.append(regionname);
        }
    }
    
    
    /** Stores a count for the given region in the given sequence */
    public synchronized void storeCount(String sequenceName,String regionType,int count) {
        if (!storage.containsKey(sequenceName)) storage.put(sequenceName, new HashMap<String,Integer>());
        HashMap<String,Integer> seqHash=storage.get(sequenceName);
        seqHash.put(regionType, count);
    }
    
    /** Increases the count for the given region in the given sequence */
    public synchronized void increaseCount(String sequenceName,String regionType) {
        if (!storage.containsKey(sequenceName)) storage.put(sequenceName, new HashMap<String,Integer>());
        HashMap<String,Integer> seqHash=storage.get(sequenceName);
        if (seqHash.containsKey(regionType)) {
            seqHash.put(regionType, seqHash.get(regionType)+1);
        }
        else seqHash.put(regionType, 1);
    }
    
    /** Returns the count for the given region in the given sequence */
    public synchronized int getCount(String sequenceName,String regionType) {
        if (!storage.containsKey(sequenceName)) return 0;
        HashMap<String,Integer> seqHash=storage.get(sequenceName);
        if (!seqHash.containsKey(regionType)) return 0;
        else return seqHash.get(regionType);
    }
       
    
    @Override
    @SuppressWarnings("unchecked")    
    public CompareRegionOccurrencesAnalysis clone() {
        CompareRegionOccurrencesAnalysis newanalysis=new CompareRegionOccurrencesAnalysis();
        super.cloneCommonSettings(newanalysis);    
        newanalysis.results=(HashMap<String,Double[]>)this.results.clone();
        newanalysis.statisticalTest=this.statisticalTest;
        newanalysis.bonferroniStrategy=this.bonferroniStrategy;
        newanalysis.significanceThreshold=this.significanceThreshold;    
        newanalysis.multiplehypothesis=this.multiplehypothesis;
        newanalysis.regionsTrackName=this.regionsTrackName;
        newanalysis.targetSetName=this.targetSetName;
        newanalysis.controlSetName=this.controlSetName;
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        this.results=(HashMap<String,Double[]>)((CompareRegionOccurrencesAnalysis)source).results.clone();
        this.bonferroniStrategy=((CompareRegionOccurrencesAnalysis)source).bonferroniStrategy;
        this.statisticalTest=((CompareRegionOccurrencesAnalysis)source).statisticalTest;
        this.significanceThreshold=((CompareRegionOccurrencesAnalysis)source).significanceThreshold;
        this.multiplehypothesis=((CompareRegionOccurrencesAnalysis)source).multiplehypothesis;
        this.regionsTrackName=((CompareRegionOccurrencesAnalysis)source).regionsTrackName;
        this.targetSetName=((CompareRegionOccurrencesAnalysis)source).targetSetName;
        this.controlSetName=((CompareRegionOccurrencesAnalysis)source).controlSetName;
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
        String sortorder="By type";
        if (outputSettings!=null) {
          try {
                 Parameter[] defaults=getOutputParameters();
                 sortorder=(String)outputSettings.getResolvedParameter("Sort by",defaults,engine);
          } 
          catch (ExecutionError e) {throw e;} 
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        } 
        engine.createHTMLheader("Region Occurrence Comparison", null, null, true, true, true, outputobject);
        formatSummary(outputobject,false);
        outputobject.append("<br />\n<table class=\"sortable\">\n",HTML);
        outputobject.append("<tr><th>Type</th><th>Target</th><th>Control</th><th class=\"sorttable_numeric\"><nobr>p-value target</nobr></th><th class=\"sorttable_numeric\"><nobr>p-value control</nobr></th><th>Group</th></tr>\n",HTML);
        ArrayList<String> regions=new ArrayList<String>(results.keySet());
        if (sortorder.equalsIgnoreCase("By expression")) Collections.sort(regions,new SortOrderComparator());
        else Collections.sort(regions);
        int prog=0;
        for (String regiontype:regions) {
            prog++;          
            Double[] value=results.get(regiontype);
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
            outputobject.append("<td>"+escapeHTML(regiontype)+"</td>",HTML);
            outputobject.append("<td class=\""+overrepclass+"\">"+value[1].intValue()+"</td>",HTML);
            outputobject.append("<td class=\""+overrepclass+"\">"+value[2].intValue()+"</td>",HTML);
            outputobject.append("<td class=\""+overrepclass+"\">"+Graph.formatNumber(value[3],false)+"</td>",HTML);
            outputobject.append("<td class=\""+overrepclass+"\">"+Graph.formatNumber(value[4],false)+"</td>",HTML);
            outputobject.append("<td class=\""+overrepclass+"\">"+group+"</td>",HTML);
            outputobject.append("</tr>\n",HTML);
             
            if (task!=null) task.setStatusMessage("Executing operation: output ("+prog+"/"+regions.size()+")");
            format.setProgress((int)(prog*100f/(double)regions.size()));
        }
        outputobject.append("</table>\n</body>\n</html>\n",HTML);
        format.setProgress(100);
        return outputobject;
    }


    public void formatSummary(OutputData outputobject, boolean alignCenter) {
        int[] counts=new int[6]; // count the number of regions that fall in each of the 6 classes
        for (String typename:results.keySet()) {
            Double[] value=results.get(typename);
            counts[value[0].intValue()]++;
        }
        if (alignCenter) outputobject.append("<center>",HTML);
        outputobject.append("<h1 class=\"headline\">Region occurrence comparison for \""+targetSetName+"\" vs \""+controlSetName+"\"</h1>\n",HTML);
        outputobject.append("<div class=\"summary\">The analysis was performed on regions from <span class=\"dataitem\">"+regionsTrackName+"</span>",HTML);
        outputobject.append("<br />Statistical significance evaluated using a "+statisticalTest.toLowerCase()+" test with p-value threshold="+significanceThreshold,HTML);
        if (bonferroniStrategy.equals(PRESENT_TYPES)) outputobject.append("<br />(Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering "+multiplehypothesis+" region types present)",HTML);
        outputobject.append("<br /><br />\n<table>\n",HTML);
        if (statisticalTest.equalsIgnoreCase("Binomial")) {
            outputobject.append("<tr><td class=\"onlyintarget\">Types present only in target</td><td class=\"overrepintarget\">Types overrepresented in target</td><td class=\"samerate\">Same rate</td><td class=\"overrepincontrol\">Types overrepresented in control</td><td class=\"onlyincontrol\">Types present only in control</td></tr>\n",HTML);
            outputobject.append("<tr><td class=\"onlyintarget\" style=\"text-align:center\">"+counts[(int)OVERREPRESENTED_IN_TARGET_BY_DEFAULT]+"</td><td class=\"overrepintarget\" style=\"text-align:center\">"+counts[(int)OVERREPRESENTED_IN_TARGET]+"</td><td class=\"samerate\" style=\"text-align:center\" rowspan=\"2\">"+counts[(int)SAME_IN_BOTH_SETS]+"</td><td class=\"overrepincontrol\" style=\"text-align:center\">"+counts[(int)OVERREPRESENTED_IN_CONTROL]+"</td><td class=\"onlyincontrol\" style=\"text-align:center\">"+counts[(int)OVERREPRESENTED_IN_CONTROL_BY_DEFAULT]+"</td></tr>\n",HTML);
            outputobject.append("<tr><td class=\"intarget\" style=\"text-align:center\" colspan=\"2\">"+(counts[(int)OVERREPRESENTED_IN_TARGET]+counts[(int)OVERREPRESENTED_IN_TARGET_BY_DEFAULT])+"</td><td class=\"incontrol\" style=\"text-align:center\" colspan=\"2\">"+(counts[(int)OVERREPRESENTED_IN_CONTROL_BY_DEFAULT]+counts[(int)OVERREPRESENTED_IN_CONTROL])+"</td></tr>\n",HTML);
        } else if (statisticalTest.equalsIgnoreCase("Hypergeometric")) {
            outputobject.append("<tr><td class=\"overrepintarget\">Types overrepresented in target</td><td class=\"samerate\">Same rate</td><td class=\"overrepincontrol\">Types overrepresented in control</td></tr>\n",HTML);
            outputobject.append("<tr><td class=\"overrepintarget\" style=\"text-align:center\">"+counts[(int)OVERREPRESENTED_IN_TARGET]+"</td><td class=\"samerate\" style=\"text-align:center\">"+counts[(int)SAME_IN_BOTH_SETS]+"</td><td class=\"overrepincontrol\" style=\"text-align:center\">"+counts[(int)OVERREPRESENTED_IN_CONTROL]+"</td></tr>\n",HTML);
        }
        outputobject.append("</table>\n",HTML);
        outputobject.append("</div>\n",HTML);
    }

    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings outputSettings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder="By type";
        if (outputSettings!=null) {
          try {
                 Parameter[] defaults=getOutputParameters();
                 sortorder=(String)outputSettings.getResolvedParameter("Sort by",defaults,engine);
          } 
          catch (ExecutionError e) {throw e;} 
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        } 
        ArrayList<String> regions=new ArrayList<String>(results.keySet());
        if (sortorder.equalsIgnoreCase("By expression")) Collections.sort(regions,new SortOrderComparator());
        else Collections.sort(regions);
        String withinString="";
        outputobject.append("# Region occurrence comparison for \""+targetSetName+"\" vs \""+controlSetName+"\" on regions from \""+regionsTrackName+"\""+withinString+"\n",HTML);
        outputobject.append("# Statistical significance evaluated using a "+statisticalTest+" test with p-value threshold="+significanceThreshold+"\n",RAWDATA);
        if (bonferroniStrategy.equals(PRESENT_TYPES)) outputobject.append("# (Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering "+multiplehypothesis+" different types)",RAWDATA);
        outputobject.append("\n",RAWDATA);
        outputobject.append("#\n# Column 1 => Region type\n",RAWDATA);
        if (statisticalTest.equalsIgnoreCase("Binomial")) outputobject.append("# Column 2 => Number of region occurrences in target set\n",RAWDATA);   
        else if (statisticalTest.equalsIgnoreCase("Hypergeometric")) outputobject.append("# Column 2 => Number of sequences containing region in target set\n",RAWDATA);   
        outputobject.append("# Column 3 => Expression symbol: > overrepresented in target, < overrepresented in control, = same rate\n",RAWDATA);   
        if (statisticalTest.equalsIgnoreCase("Binomial")) outputobject.append("# Column 4 => Number of region occurrences in control set\n",RAWDATA);   
        else if (statisticalTest.equalsIgnoreCase("Hypergeometric")) outputobject.append("# Column 4 => Number of sequences containing region in control set\n",RAWDATA);   
        outputobject.append("# Column 5 => Calculated p-value for overrepresentation in target set\n",RAWDATA);   
        outputobject.append("# Column 6 => Calculated p-value for overrepresentation in control set\n",RAWDATA);     
        outputobject.append("\n",RAWDATA);   
        int prog=0;
        for (String regionname:regions) {
            prog++;
            Double[] value=results.get(regionname); 
            outputobject.append(regionname,RAWDATA);
            outputobject.append("\t"+expressionResultSign[value[0].intValue()],RAWDATA);
            outputobject.append("\t"+(value[2].intValue()),RAWDATA);
            outputobject.append("\t"+value[3],RAWDATA);
            outputobject.append("\t"+value[4],RAWDATA);
            outputobject.append("\n",RAWDATA);             
            format.setProgress((int)(prog*100f/(double)regions.size()));
        }
        format.setProgress(100);
        return outputobject;
    }

    @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings outputSettings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder="By type";
        boolean includeLegend=true;
        if (outputSettings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             sortorder=(String)outputSettings.getResolvedParameter("Sort by",defaults,engine);
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
   
        int rownum=0;
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(outputobject.getName());  
        
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
        row = sheet.createRow(rownum);
        outputStringValuesInCells(row, new String[]{"Type","Target","Control","P-value target","P-value control","Group"}, 0, tableheader);      
        col+=6; 
        ArrayList<String> types=new ArrayList<String>(results.size());
        types.addAll(results.keySet());        
        if (sortorder.equalsIgnoreCase("By expression")) Collections.sort(types,new SortOrderComparator());
        else Collections.sort(types);
        int prog=0;
        for (String type:types) {
            rownum++;
            row = sheet.createRow(rownum);
            col=0;            
            prog++;
           
            Double[] value=results.get(type);   
            String group="F";
            CellStyle style=null;
                 if (value[0]==OVERREPRESENTED_IN_TARGET_BY_DEFAULT) {style=groupA_UP;group="A";}
            else if (value[0]==OVERREPRESENTED_IN_CONTROL_BY_DEFAULT) {style=groupE_DOWN;group="E";}
            else if (value[0]==OVERREPRESENTED_IN_TARGET) {style=groupB_sigUP;group="B";}
            else if (value[0]==OVERREPRESENTED_IN_CONTROL) {style=groupD_sigDOWN;group="D";}
            else if (value[0]==SAME_IN_BOTH_SETS) {style=groupC_same;group="C";}
            else {style=groupF_notpresent;group="F";} // group F                 
            outputStringValuesInCells(row, new String[]{type}, col);
            col+=1;
            outputNumericValuesInCells(row, new double[]{value[1],value[2],value[3],value[4]}, col, style);
            col+=4;
            outputStringValueInCell(row, col, group, style);
            col+=1;
         
            if (task!=null) task.setStatusMessage("Executing operation: output ("+prog+"/"+types.size()+")");
            format.setProgress(prog,types.size());
        }           
        format.setProgress(95);  
        for (short i=0;i<col;i++) {
            sheet.autoSizeColumn(i);               
        }
        
        // Add the header on top of the page
        if (includeLegend) { 
            int[] counts=new int[6]; // count the number of motifs that fall in each of the 6 classes
            for (String type:results.keySet()) {
                Double[] value=results.get(type);
                counts[value[0].intValue()]++;
            }            
            sheet.createFreezePane(0,headerrows,0,headerrows);
            row=sheet.getRow(0);
            outputStringValueInCell(row, 0, "Region occurrence analysis for \""+targetSetName+"\" vs \""+controlSetName+"\"", title);
            StringBuilder firstLine=new StringBuilder();          
            firstLine.append("The analysis was performed on regions from \"");                        
            firstLine.append(regionsTrackName);
            firstLine.append("\".");
            row=sheet.getRow(2);
            outputStringValueInCell(row, 0, firstLine.toString(), null);  
            
            StringBuilder secondLine=new StringBuilder();
            secondLine.append("Statistical significance evaluated using a "+statisticalTest.toLowerCase()+" test with p-value threshold="+significanceThreshold);
            if (bonferroniStrategy.equals(PRESENT_TYPES)) secondLine.append(" (Bonferroni-corrected threshold="+(significanceThreshold/(double)multiplehypothesis)+" considering "+multiplehypothesis+" present region types)");
            secondLine.append(".");
            row=sheet.getRow(3);
            outputStringValueInCell(row, 0, secondLine.toString(), null);  
                       
//            if (statisticalTest.equalsIgnoreCase("Binomial")) {
//                row=sheet.getRow(5);
//                outputStringValueInCell(row, 0, "Motifs present\nonly in target", groupA_UP_LEGEND);
//                outputStringValueInCell(row, 1, "Motifs overrepresented\nin target",groupB_sigUP_LEGEND);
//                outputStringValueInCell(row, 2, "Same rate",groupC_same_LEGEND);
//                outputStringValueInCell(row, 3, "Motifs overrepresented\nin control",groupD_sigDOWN_LEGEND);                
//                outputStringValueInCell(row, 4, "Motifs present\nonly in control",groupE_DOWN_LEGEND);
//                outputStringValueInCell(row, 5, "Not present",groupF_notpresent_LEGEND);                
//                row=sheet.getRow(6);
//                outputNumericValueInCell(row, 0, counts[(int)OVERREPRESENTED_IN_TARGET_BY_DEFAULT], groupA_UP_LEGEND);
//                outputNumericValueInCell(row, 1, counts[(int)OVERREPRESENTED_IN_TARGET], groupB_sigUP_LEGEND);
//                outputNumericValueInCell(row, 2, counts[(int)SAME_IN_BOTH_SETS], groupC_same_LEGEND);
//                outputNumericValueInCell(row, 3, counts[(int)OVERREPRESENTED_IN_CONTROL_BY_DEFAULT], groupD_sigDOWN_LEGEND);
//                outputNumericValueInCell(row, 4, counts[(int)OVERREPRESENTED_IN_CONTROL], groupE_DOWN_LEGEND);
//                outputNumericValueInCell(row, 5, counts[(int)NOT_PRESENT], groupF_notpresent_LEGEND);
//                row=sheet.getRow(7);
//                outputNumericValueInCell(row, 0, counts[(int)OVERREPRESENTED_IN_TARGET]+counts[(int)OVERREPRESENTED_IN_TARGET_BY_DEFAULT], groupB_sigUP_LEGEND);
//                outputNumericValueInCell(row, 3, counts[(int)OVERREPRESENTED_IN_CONTROL]+counts[(int)OVERREPRESENTED_IN_CONTROL_BY_DEFAULT], groupD_sigDOWN_LEGEND);
//                CellRangeAddress[] merged=new CellRangeAddress[]{
//                    new CellRangeAddress(6,7,2,2),
//                    new CellRangeAddress(6,7,5,5),
//                    new CellRangeAddress(7,7,0,1),
//                    new CellRangeAddress(7,7,3,4)
//                };
//                for (CellRangeAddress range:merged) {
//                    sheet.addMergedRegion(range);
//                    RegionUtil.setBorderBottom(HSSFCellStyle.BORDER_THIN, range, sheet, workbook); // borders must be updated on merged cells
//                    RegionUtil.setBorderTop(HSSFCellStyle.BORDER_THIN, range, sheet, workbook); // borders must be updated on merged cells
//                    RegionUtil.setBorderLeft(HSSFCellStyle.BORDER_THIN, range, sheet, workbook); // borders must be updated on merged cells
//                    RegionUtil.setBorderRight(HSSFCellStyle.BORDER_THIN, range, sheet, workbook); // borders must be updated on merged cells                                   
//                }               
//                int[] widths=new int[]{3500,5000,3500,5000,3500,3500};
//                for (int i=0;i<widths.length;i++) {
//                    int currentwidth=sheet.getColumnWidth(i);
//                    sheet.setColumnWidth(i,Math.max(currentwidth,widths[i]));                    
//                }            
//             } else 
             if (statisticalTest.equalsIgnoreCase("Hypergeometric")) {
                row=sheet.getRow(5);
                outputStringValueInCell(row, 0, "Types overrepresented\nin target",groupB_sigUP_LEGEND);
                outputStringValueInCell(row, 1, "Same rate",groupC_same_LEGEND);
                outputStringValueInCell(row, 2, "Types overrepresented\nin control",groupD_sigDOWN_LEGEND);                
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
    public void runAnalysis(OperationTask task) throws Exception {   
        RegionDataset regiontrack=(RegionDataset)task.getParameter("Region track");  
        regionsTrackName=regiontrack.getName();
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
        
        HashSet<String> presentRegions=new HashSet<String>();
        
        MotifLabEngine engine=task.getEngine();
        ArrayList<String> sequenceNames=new ArrayList<String>();
        SequenceCollection allSequences=new SequenceCollection("all"); // the union of target and control sets (which might be overlapping)     
        // first: find which types of regions are present in the target and control sets
        for (int i=0;i<targetSet.size();i++) {
            Sequence seq=targetSet.getSequenceByIndex(i,engine);
            allSequences.addSequence(seq);
            sequenceNames.add(seq.getName());
            presentRegions.addAll( ((RegionSequenceData)regiontrack.getSequenceByName(seq.getName())).getRegionTypes() );
        }
        for (int i=0;i<controlSet.size();i++) {
            Sequence seq=controlSet.getSequenceByIndex(i,engine);
            allSequences.addSequence(seq);
            if (!sequenceNames.contains(seq.getName())) sequenceNames.add(seq.getName());
            presentRegions.addAll( ((RegionSequenceData)regiontrack.getSequenceByName(seq.getName())).getRegionTypes() );            
        }  
        // next: count the number of these types in each set and compare

        int allregionsSize=presentRegions.size();        

        TaskRunner taskRunner=task.getEngine().getTaskRunner();
        task.setProgress(0L,allregionsSize);
        long[] counters=new long[]{0,0,allregionsSize}; // counters[0]=motifs started, [1]=motifs completed, [2]=total number of motifs

        ArrayList<ProcessRegionTask> processTasks=new ArrayList<ProcessRegionTask>(allregionsSize);
        Iterator<String> iterator=presentRegions.iterator();
        while(iterator.hasNext()) {
            String regionname=iterator.next();
            processTasks.add(new ProcessRegionTask(regionname, sequenceNames, targetSet, controlSet, allSequences, regiontrack, presentRegions, task, counters));        
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
        if (countOK!=allregionsSize) {
            throw new ExecutionError("Some mysterious error occurred while performing analysis: "+getAnalysisName());
        }           
        
//        while(iterator.hasNext()) {
//            String regionname=iterator.next();
//            // first count the number of times the region occurs in each sequence (and within limits) and store these values
//            for (int j=0;j<sequenceNames.size();j++) {
//                RegionSequenceData seq=(RegionSequenceData)regiontrack.getSequenceByName(sequenceNames.get(j));
//                RegionSequenceData withinRegionsSeq=null;
//                int count=regionCountInSequence(regionname, seq, withinRegionsSeq);
//                storeCount(seq.getSequenceName(), regionname, count);
//            }  
//
//            if (statisticalTest.equalsIgnoreCase("Hypergeometric")) {
//                    int targetSetCount=countSequencesWithRegionInSequenceCollection(regionname, targetSet); // number of sequences in target set containing this region
//                    int controlSetCount=countSequencesWithRegionInSequenceCollection(regionname, controlSet); // number of sequences in control set containing this region
//                    int unionCount=countSequencesWithRegionInSequenceCollection(regionname,allSequences); // total number of sequences containing this region
//                    HypergeometricDistribution hypergeomTarget = new HypergeometricDistribution(allSequences.size(), unionCount, targetSet.size());
//                    HypergeometricDistribution hypergeomControl = new HypergeometricDistribution(allSequences.size(), unionCount, controlSet.size());
//                    // test for overrepresentation in target set
//                    double pvalueOverrep=hypergeomTarget.upperCumulativeProbability(targetSetCount);
//                    if (pvalueOverrep>1) pvalueOverrep=1.0; // this can happen apparently :(            
//                    // test for overrepresentation in control set (i.e. underrepresentation in target set)
//                    double pvalueUnderrep=hypergeomControl.upperCumulativeProbability(controlSetCount);
//                    if (pvalueUnderrep>1) pvalueUnderrep=1.0; // this can happen apparently :(
//                    
//                         if (bonferroniStrategy.equals(PRESENT_TYPES)) multiplehypothesis=presentRegions.size();
//                    double correctedThreshold=significanceThreshold/(double)multiplehypothesis;    
//                    
//                    double expressionresult=0;
//                         if (targetSetCount==0 && controlSetCount==0) expressionresult=NOT_PRESENT;
//                    else if (pvalueOverrep<=correctedThreshold) expressionresult=OVERREPRESENTED_IN_TARGET;
//                    else if (pvalueUnderrep<=correctedThreshold) expressionresult=OVERREPRESENTED_IN_CONTROL; 
//                    else expressionresult=SAME_IN_BOTH_SETS;
//                    results.put(regionname, new Double[]{expressionresult,(double)targetSetCount,(double)controlSetCount,(double)pvalueOverrep,(double)pvalueUnderrep});                          
//            }
//            
//            
//            if (i%25==0) {
//                task.checkExecutionLock(); // checks to see if this task should suspend execution
//                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
//            }
//            task.setStatusMessage("Executing analysis: "+getAnalysisName()+" ("+(i+1)+"/"+allregionsSize+")");
//            task.setProgress(i+1, allregionsSize+20); // the +20 is just to not reach 100% in this loop
//            i++;
//                        
//        }
        storage=null; // release temporary counts
    }
    
    private Double[] processRegion(String regionname, ArrayList<String> sequenceNames, SequenceCollection targetSet, SequenceCollection controlSet, SequenceCollection allSequences, RegionDataset regiontrack, HashSet<String> presentRegions) throws ExecutionError  {
        // first count the number of times the region occurs in each sequence (and within limits) and store these values
        for (int j=0;j<sequenceNames.size();j++) {
            RegionSequenceData seq=(RegionSequenceData)regiontrack.getSequenceByName(sequenceNames.get(j));
            RegionSequenceData withinRegionsSeq=null;
            int count=regionCountInSequence(regionname, seq, withinRegionsSeq);
            storeCount(seq.getSequenceName(), regionname, count);
        }  

        if (statisticalTest.equalsIgnoreCase("Hypergeometric")) {
                int targetSetCount=countSequencesWithRegionInSequenceCollection(regionname, targetSet); // number of sequences in target set containing this region
                int controlSetCount=countSequencesWithRegionInSequenceCollection(regionname, controlSet); // number of sequences in control set containing this region
                int unionCount=countSequencesWithRegionInSequenceCollection(regionname,allSequences); // total number of sequences containing this region
                HypergeometricDistribution hypergeomTarget = new HypergeometricDistribution(allSequences.size(), unionCount, targetSet.size());
                HypergeometricDistribution hypergeomControl = new HypergeometricDistribution(allSequences.size(), unionCount, controlSet.size());
                // test for overrepresentation in target set
                double pvalueOverrep=hypergeomTarget.upperCumulativeProbability(targetSetCount);
                if (pvalueOverrep>1) pvalueOverrep=1.0; // this can happen apparently :(            
                // test for overrepresentation in control set (i.e. underrepresentation in target set)
                double pvalueUnderrep=hypergeomControl.upperCumulativeProbability(controlSetCount);
                if (pvalueUnderrep>1) pvalueUnderrep=1.0; // this can happen apparently :(

                     if (bonferroniStrategy.equals(PRESENT_TYPES)) multiplehypothesis=presentRegions.size();
                double correctedThreshold=significanceThreshold/(double)multiplehypothesis;    

                double expressionresult=0;
                     if (targetSetCount==0 && controlSetCount==0) expressionresult=NOT_PRESENT;
                else if (pvalueOverrep<=correctedThreshold) expressionresult=OVERREPRESENTED_IN_TARGET;
                else if (pvalueUnderrep<=correctedThreshold) expressionresult=OVERREPRESENTED_IN_CONTROL; 
                else expressionresult=SAME_IN_BOTH_SETS;
                //results.put(regionname, new Double[]{expressionresult,(double)targetSetCount,(double)controlSetCount,(double)pvalueOverrep,(double)pvalueUnderrep});     
                return new Double[]{expressionresult,(double)targetSetCount,(double)controlSetCount,(double)pvalueOverrep,(double)pvalueUnderrep};
        } else throw new ExecutionError("Unknown statistical test: "+statisticalTest);   
    }
    
    
    protected class ProcessRegionTask implements Callable<String> {
        final String regionType;
        final SequenceCollection targetSet;
        final SequenceCollection controlSet;
        final SequenceCollection allSequences;
        final ArrayList<String> sequenceNames;
        final RegionDataset regiontrack;
        final HashSet<String> presentRegions;
        final long[] counters; // counters[0]=motifs started, [1]=motifs completed, [2]=total number of motifs.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final OperationTask task;  
        
        public ProcessRegionTask(String regionType, ArrayList<String> sequenceNames, SequenceCollection targetSet, SequenceCollection controlSet, SequenceCollection allSequences, RegionDataset regiontrack,  HashSet<String> presentRegions, OperationTask task, long[] counters) {
           this.regionType=regionType;
           this.targetSet=targetSet;
           this.controlSet=controlSet;
           this.allSequences=allSequences;
           this.regiontrack=regiontrack;
           this.sequenceNames=sequenceNames;
           this.presentRegions=presentRegions;           
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
            
            Double[] counts=processRegion(regionType, sequenceNames, targetSet, controlSet, allSequences, regiontrack, presentRegions);           
            
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                results.put(regionType,counts);
                task.setStatusMessage("Executing analysis: "+getAnalysisName()+"  ("+counters[1]+"/"+counters[2]+")");
                task.setProgress(counters[1],counters[2]);                              
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();    
            return regionType;
        }   
    }
    

    /** Counts the number of times a region occurs in the given sequence (and also within the regions in 'withinRegionsSequence' if provided) */
    private int regionCountInSequence(String regionname, RegionSequenceData sequencedata, RegionSequenceData withinRegionsSequence) {
        int count=0;
        for (Region region:sequencedata.getAllRegions()) {
            boolean matchingType=region.getType().equals(regionname);
            if (matchingType) {
                count++;
            }
        }        
        return count;
    }    

    /** Returns the number of sequences from the sequence collection that contains the region */
    private int countSequencesWithRegionInSequenceCollection(String regionname, SequenceCollection collection) {
        int total=0;
        for (int i=0;i<collection.size();i++) {
            String seqname=collection.getSequenceNameByIndex(i);
            if (getCount(seqname, regionname)>0) total++;
        }
        return total;
    }
  

    
    /** Returns a list of output parameters that can be set when an Analysis is output */
    @Override
    public Parameter[] getOutputParameters() {
        return new Parameter[] {
             new Parameter("Sort by",String.class,"By type",new String[]{"By type","By expression"},null,false,false),
         };
    }
    
    @Override
    public String[] getOutputParameterFilter(String parameter) {
        if (parameter.equals("By type") || parameter.equals("Sort by")) return new String[]{"HTML","RawData","Excel"};        
        return null;
    }     
    
    private class SortOrderComparator implements Comparator<String> {
            @Override
            public int compare(String region1, String region2) { // these are two regionnames
                Double[] value1=results.get(region1);
                Double[] value2=results.get(region2);
                Double expression1=value1[0];
                Double expression2=value2[0];
                if (expression1==null && expression2==null) return 0;
                if (expression1==null) return 1;
                if (expression2==null) return -1;
                if (Double.isNaN(expression1) && Double.isNaN(expression2)) return 0;
                if (Double.isNaN(expression1)) return 1;
                if (Double.isNaN(expression2)) return -1;                  
                if (expression1>expression2) return -1;
                else if (expression1<expression2) return 1;
                else { // same expression group. Compare p-values
                    if (expression1<=1) return region1.compareTo(region2); // use name if expression is the same
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
                        else return region1.compareTo(region2);
                    }
                }
                //return region1.compareTo(region2); // use name if expression is the same
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
        formatSummary(header,true);
        header.append("<br /></center></body></html>",HTML);
        header.setShowAsHTML(true);
        OutputPanel headerpanel=new OutputPanel(header, gui);
        RegionOccurrenceTableModel tablemodel=new RegionOccurrenceTableModel(gui);
        GenericRegionBrowserPanel tablepanel=new GenericRegionBrowserPanel(gui, tablemodel,RegionOccurrenceTableModel.TYPE, modal);
        JTable table=tablepanel.getTable();
        CellRenderer_Color renderer=new CellRenderer_Color(gui.getVisualizationSettings());
        table.setDefaultRenderer(Integer.class, renderer);
        table.setDefaultRenderer(Double.class, renderer); 
        
        JCheckBox groupByExpressionCheckbox=new JCheckBox("Group by expression", true);
        final GroupRowSorter<RegionOccurrenceTableModel, Integer> sorter=new GroupRowSorter<RegionOccurrenceTableModel, Integer>(tablemodel, groupByExpressionCheckbox, RegionOccurrenceTableModel.EXPRESSION, new ExpressionOrderComparator());
        table.setRowSorter(sorter);
        sorter.setSortsOnUpdates(true);
        groupByExpressionCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sorter.allRowsChanged(); // this is done to update the sorting
            }
        });
        table.removeColumn(table.getColumn("Expression")); // hide the expression column. We do not need it in the view, only in the model
        table.getRowSorter().toggleSortOrder(RegionOccurrenceTableModel.TARGET);
        table.getRowSorter().toggleSortOrder(RegionOccurrenceTableModel.TARGET);
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

private class RegionOccurrenceTableModel extends AbstractTableModel {
    private String[] columnNames={" ","Type","Target","Control","p-value target","p-value control","Expression"};
    private String[] regionnames=null;
    private VisualizationSettings settings;
    public static final int COLOR=0;    
    public static final int TYPE=1;
    public static final int TARGET=2;
    public static final int CONTROL=3;
    public static final int P_VALUE_TARGET=4;
    public static final int P_VALUE_CONTROL=5;
    public static final int EXPRESSION=6;

    public RegionOccurrenceTableModel(MotifLabGUI gui) {
        settings=gui.getVisualizationSettings();
        regionnames=new String[results.size()];
        int i=0;
        for (String name:results.keySet()) {
           regionnames[i]=name;
           i++;
        }
    }


    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case COLOR:return Color.class;            
            case TYPE:return String.class;            
            case TARGET:return Integer.class;
            case CONTROL:return Integer.class;
            case P_VALUE_TARGET:return Double.class;
            case P_VALUE_CONTROL:return Double.class;
            case EXPRESSION:return Double.class;
            default:return Object.class;
        }
    }


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLOR:return settings.getFeatureColor(regionnames[rowIndex]);
            case TYPE:return regionnames[rowIndex];           
            case TARGET:return ((Double)results.get(regionnames[rowIndex])[1]).intValue();
            case CONTROL:return ((Double)results.get(regionnames[rowIndex])[2]).intValue();
            case P_VALUE_TARGET:return ((Double)results.get(regionnames[rowIndex])[3]);
            case P_VALUE_CONTROL:return ((Double)results.get(regionnames[rowIndex])[4]);
            case EXPRESSION:return ((Double)results.get(regionnames[rowIndex])[0]);
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
        return regionnames.length;
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
            String motifID=(String)table.getValueAt(row, RegionOccurrenceTableModel.TYPE);
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
