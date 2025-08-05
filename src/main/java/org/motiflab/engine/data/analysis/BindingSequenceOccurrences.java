/*
 
 
 */

package org.motiflab.engine.data.analysis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import javax.swing.JTable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.gui.GenericMotifBrowserPanel;
import org.motiflab.gui.MotifLogo;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.VisualizationSettings;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.TextVariable;

/**
 *
 * @author kjetikl
 */
public class BindingSequenceOccurrences extends Analysis {
    private final static String typedescription="Analysis: binding sequence occurrences";
    private final static String analysisName="binding sequence occurrences";
    private final static String description="Counts the number of occurrences of each unique binding sequence for motifs in a motif track";
    private static final String SORT_BY_BINDING_SEQUENCE="Binding sequence";
    private static final String SORT_BY_TOTAL_OCCURRENCES="Total occurrences";
    private static final String SORT_BY_SEQUENCE_OCCURRENCES="Sequence occurrences";
 
    private HashMap<String,HashMap<String,int[]>> counts=null; // motif=>inner data structure. Inner key is binding sequence. Value is int[]{sequence support, total count, match score}.  The match score is really a percentage value, but for backwards compatibility it is stored as a value between 0 and 100.000; it is dynamically converted to a double between 0 and 100 (with 3 decimals) by dividing with 1000 when necessary
    private int sequenceCollectionSize=0;
    private String motifCollectionName=null;
    private String motifTrackName=null;
    private String sequenceCollectionName=null;
    private String withinRegionsTrackName=null;    
    private int ignored=0; // number of TFBS sequences that was ignored because the 'sequence' property was not set
    private int counted=0; // number of TFBS sequences that was counted
    
    private static final int MATCH_SCORE_FACTOR=100000; // a scaling factor used since match scores are represented as integers instead of doubles


    private static final int MOTIF=0;
    private static final int NAME=1;    
    private static final int TFBS=2;
    private static final int TOTAL=3;
    private static final int SEQUENCE_SUPPORT=4;    
    private static final int MATCH_SCORE=5;     
    private static final int LOGO=6;

    
    public BindingSequenceOccurrences() {
        this.name="BindingSequenceOccurrences_temp";
        addParameter("Motif track",RegionDataset.class, null,new Class[]{RegionDataset.class},"A region track containing motif sites",true,false);
        addParameter("Motifs",MotifCollection.class, null,new Class[]{MotifCollection.class},"The motif to consider in this analysis",true,false);
        addParameter("Sequences",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"If specified, the analysis will be limited to sequences in this collection",false,false);
        addParameter("Within regions",RegionDataset.class, null,new Class[]{RegionDataset.class},"Limits the analysis to motifs found within the selected regions and not the full sequences",false,false); 
    }

    @Override
    public String getAnalysisName() {
        return analysisName;
    }

    @Override
    public String getDescription() {return description;}
    
    
    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Motif track","Motif"};}
    
    @Override
    public boolean canUseAsSourceProxy(Data data) {
        if (data instanceof RegionDataset) return ((RegionDataset)data).isMotifTrack(); // only allow Motif Tracks as input
        else return (data instanceof Motif);
    }       
    
    @Override
    public Parameter[] getOutputParameters(String dataformat) {      
        Parameter sortByPar = new Parameter("Sort by",String.class,SORT_BY_TOTAL_OCCURRENCES, new String[]{SORT_BY_BINDING_SEQUENCE,SORT_BY_SEQUENCE_OCCURRENCES,SORT_BY_TOTAL_OCCURRENCES},"Sorting order for the results table",false,false);
        Parameter legendPar = new Parameter("Legend",Boolean.class,Boolean.TRUE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a header with a title and analysis details will be included at the top of the Excel sheet.",false,false);       
        Parameter logos = new Parameter("Logos",String.class,getMotifLogoDefaultOption(dataformat), getMotifLogoOptions(dataformat),"Include motif sequence logos in the table",false,false);         
        
        if (dataformat.equals(HTML)) return new Parameter[] {sortByPar,logos};
        if (dataformat.equals(EXCEL)) return new Parameter[] {sortByPar,legendPar,logos};
        if (dataformat.equals(RAWDATA)) return new Parameter[] {sortByPar,logos};
        return new Parameter[0];
    }
       

    @Override
    public String[] getResultVariables() {
        // return new String[]{"support","total","match score"};
        return new String[]{"support","total","match score","reverse"};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename==null || variablename.isEmpty()) throw new ExecutionError("'"+getName()+"' does not have a result for ''");
        else if (variablename.equals("support") || variablename.equals("total") || variablename.equals("match score")) {
            int index=0;
                 if (variablename.equals("support")) index=2;
            else if (variablename.equals("total")) index=3;
            else if (variablename.equals("match score")) index=4;
            TextVariable text=new TextVariable("temp");
            ArrayList<Object[]> list=assembleList("motif");
            for (Object[] entry:list) {
               text.append(entry[0]+"\t"+entry[1]+"\t"+entry[index]);              
            }
            return text;
        }   
        else if (variablename.equals("reverse")) {
            BindingSequenceOccurrences newanalysis=this.clone();
            HashMap<String,HashMap<String,int[]>> newcounts=reverseBindingSequences(counts);
            newanalysis.counts=newcounts;
            return newanalysis;
        }
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else if (variablename.equals("reverse")) return BindingSequenceOccurrences.class;
       else return TextVariable.class;
    }

  
    
    
    @Override
    @SuppressWarnings("unchecked")
    public BindingSequenceOccurrences clone() {
        BindingSequenceOccurrences newanalysis=new BindingSequenceOccurrences();
        super.cloneCommonSettings(newanalysis);
        newanalysis.counts=cloneCounts();
        newanalysis.sequenceCollectionSize=this.sequenceCollectionSize;
        newanalysis.motifCollectionName=this.motifCollectionName;
        newanalysis.motifTrackName=this.motifTrackName;
        newanalysis.sequenceCollectionName=this.sequenceCollectionName;
        newanalysis.withinRegionsTrackName=this.withinRegionsTrackName;    
        newanalysis.counted=this.counted;   
        newanalysis.ignored=this.ignored;   
        return newanalysis;
    }
    
    private HashMap<String,HashMap<String,int[]>> cloneCounts() {
        if (counts==null) return null;
        HashMap<String,HashMap<String,int[]>> newcounts=new HashMap<String,HashMap<String,int[]>>();
        for (String key:counts.keySet()) {
            HashMap<String,int[]> innervalues=counts.get(key);
            HashMap<String,int[]> innerclone=new HashMap<String,int[]>();
            for (String innerkey:innervalues.keySet()) {
                int[] innerarray=innervalues.get(innerkey);
                int[] innerarraycopy=new int[innerarray.length];
                System.arraycopy(innerarray, 0, innerarraycopy, 0, innerarray.length);
                innerclone.put(innerkey, innerarraycopy);
            }
            newcounts.put(key,innerclone);
        }
        return newcounts;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        this.counts=(HashMap<String,HashMap<String,int[]>>)((BindingSequenceOccurrences)source).counts;
        this.sequenceCollectionSize=((BindingSequenceOccurrences)source).sequenceCollectionSize;
        this.motifCollectionName=((BindingSequenceOccurrences)source).motifCollectionName;
        this.motifTrackName=((BindingSequenceOccurrences)source).motifTrackName;
        this.sequenceCollectionName=((BindingSequenceOccurrences)source).sequenceCollectionName;
        this.withinRegionsTrackName=((BindingSequenceOccurrences)source).withinRegionsTrackName;   
        this.counted=((BindingSequenceOccurrences)source).counted;  
        this.ignored=((BindingSequenceOccurrences)source).ignored;          
    }

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

    /** Constructs a sorted list with the motif name [0], TFBS sequence [1], sequence count [2], total occurrence count [3] and match score [4]. (The match score will be returned as a double between 0 and 1.0)
     *  This method is contains code common for both formatHTML and formatRaw
     */
    private ArrayList<Object[]> assembleList(String sortorder) {
        ArrayList<Object[]> resultList=new ArrayList<Object[]>();
        ArrayList<String> motifNames=new ArrayList<String>(counts.keySet());
        Collections.sort(motifNames);
        for (String motifname:motifNames) {
            ArrayList<Object[]> subList=new ArrayList<Object[]>();
            HashMap<String,int[]> innercounts=counts.get(motifname);
            for (String tfbs:innercounts.keySet()) {
                int[] values=innercounts.get(tfbs);
                int sequencesupport=values[0];            
                int totalcount=values[1];  
                double matchscore=(values.length>2)?((double)values[2]/MATCH_SCORE_FACTOR*100.00):0; // return 0 if missing for backwards compatibility
                subList.add(new Object[]{motifname,tfbs,new Integer(sequencesupport), new Integer(totalcount), new Double(matchscore)});
            }
            Collections.sort(subList, new SortOrderComparator(sortorder));
            resultList.addAll(subList);
        }        
        return resultList;
    }

    @Override
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder=SORT_BY_BINDING_SEQUENCE;
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        Color [] basecolors=vizSettings.getBaseColors();
        MotifLogo sequencelogo=new MotifLogo(basecolors,sequencelogoSize);
        String showSequenceLogosString="";
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);   
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos = includeLogosInOutput(showSequenceLogosString);
        
        ArrayList<Object[]> resultList=assembleList(sortorder);
        engine.createHTMLheader("Binding Sequence Occurrence Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<h1 class=\"headline\">Binding Sequence Occurrence Analysis</h1>\n",HTML);
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
        outputobject.append("\n</div>\n",HTML);
        outputobject.append("<br>\n",HTML);
        outputobject.append("<table class=\"sortable\">\n",HTML);
        outputobject.append("<tr>",HTML);             
        outputobject.append("<th>Motif</th>",HTML);
        outputobject.append("<th>Name</th>",HTML);        
        outputobject.append("<th>TFBS</th>",HTML);
        outputobject.append("<th>Total</th>",HTML);
        outputobject.append("<th>Sequences</th>",HTML);
        outputobject.append("<th class=\"sorttable_numeric\">%</th>",HTML);
        outputobject.append("<th>Match score</th>",HTML);
        if (showSequenceLogos) outputobject.append("<th class=\"sorttable_nosort\">Logo</th>",HTML);
        outputobject.append("</tr>\n",HTML);
        DecimalFormat decimalformatter=DataFormat.getDecimalFormatter(3);
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifID=(String)entry[0];
            String motifname="???";            
            String tfbs=(String)entry[1];
            int seqcount=(Integer)entry[2];
            int totalcount=(Integer)entry[3];
            double matchscore=(Double)entry[4];
            Motif motif=null;
            if (engine.dataExists(motifID, Motif.class)) {
                motif=(Motif)engine.getDataItem(motifID);
                motifname=motif.getShortName();
            }            
            outputobject.append("<tr>",HTML);          
            outputobject.append("<td>"+escapeHTML(motifID)+"</td>",HTML);
            outputobject.append("<td>"+escapeHTML(motifname)+"</td>",HTML);            
            outputobject.append("<td>"+tfbs+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+totalcount+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+seqcount+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+(int)((double)seqcount*100f/(double)sequenceCollectionSize)+"%</td>",HTML);
            outputobject.append("<td class=\"num\">"+decimalformatter.format(matchscore)+"</td>",HTML);
            if (showSequenceLogos) {
                if (motif instanceof Motif) {
                    sequencelogo.setMotif((Motif)motif);
                    sequencelogo.setBindingSequence(tfbs);
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
        String sortorder=SORT_BY_BINDING_SEQUENCE;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        ArrayList<Object[]> resultList=assembleList(sortorder);
        outputobject.append("# Binding sequence occurrence analysis with motifs from '"+motifCollectionName+"' and sites from '"+motifTrackName+"'",RAWDATA);
        if (withinRegionsTrackName!=null) {            
            outputobject.append(" within '",RAWDATA);
            outputobject.append(withinRegionsTrackName,RAWDATA);
            outputobject.append("' regions",RAWDATA);
        }        
        outputobject.append(" on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),RAWDATA);
        if (sequenceCollectionName!=null) outputobject.append(" from collection '"+sequenceCollectionName+"'",RAWDATA);

        outputobject.append("\n\n#motif ID, motif name, binding sequence, total occurrences, number of sequences containing motif, total number of sequences, percentage of sequences containing motif, match score",RAWDATA);
        outputobject.append("\n",RAWDATA);     
        DecimalFormat decimalformatter=DataFormat.getDecimalFormatter(3);
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifID=(String)entry[0];
            String motifname="???";
            if (engine.dataExists(motifID, Motif.class)) {
                Motif motif=(Motif)engine.getDataItem(motifID);
                motifname=motif.getShortName();
            }              
            String tfbs=(String)entry[1];
            int seqcount=(Integer)entry[2];
            int totalcount=(Integer)entry[3];
            double matchscore=(Double)entry[4];
            outputobject.append(motifID+"\t"+motifname+"\t"+tfbs+"\t"+totalcount+"\t"+seqcount+"\t"+sequenceCollectionSize+"\t"+(int)((double)seqcount*100/(double)sequenceCollectionSize)+"%"+"\t"+decimalformatter.format(matchscore),RAWDATA);
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
        String sortorder=SORT_BY_BINDING_SEQUENCE;
        boolean includeLegend=true;
        int logoheight=19;
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        Color [] basecolors=vizSettings.getBaseColors();
        boolean border=(Boolean)vizSettings.getSettingAsType("motif.border", Boolean.TRUE);        
        MotifLogo sequencelogo=new MotifLogo(basecolors,sequencelogoSize);         
        String showSequenceLogosString="";        
        if (settings!=null) {
            try {
                Parameter[] defaults=getOutputParameters(format);
                sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);         
                includeLegend=(Boolean)settings.getResolvedParameter("Legend",defaults,engine);
                showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);              
            }
            catch (ExecutionError e) {throw e;}
            catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos = includeLogosInOutput(showSequenceLogosString); 
        boolean showLogosAsImages = includeLogosInOutputAsImages(showSequenceLogosString);
        
        ArrayList<Object[]> resultList=assembleList(sortorder);
        int rownum=0;
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Binding Sequence Occurrences"); 
        CreationHelper helper = (showLogosAsImages)?workbook.getCreationHelper():null;
        Drawing drawing = (showLogosAsImages)?sheet.createDrawingPatriarch():null;         
        
        CellStyle title = getExcelTitleStyle(workbook);
        CellStyle tableheader = getExcelTableHeaderStyle(workbook);
        
        // Make room for the header which will be added later
        Row row = null;
        int headerrows=5;
        if (includeLegend) {
            for (int j=0;j<headerrows;j++) {
               row = sheet.createRow(j); 
            }
            rownum=headerrows-1; // -1 because it will be incremented later on...
        }        
        int col=0;
        int logocolumn=0;
        row = sheet.createRow(rownum);
        outputStringValuesInCells(row, new String[]{"Motif","Name","TFBS","Total","Sequences","Match Score"}, 0, tableheader);      
        col+=6;
        if (showSequenceLogos) {
            logocolumn=col;
            outputStringValuesInCells(row, new String[]{"Logo"}, logocolumn, tableheader);
        } 
        int maxlogowidth=0; // the number of bases in the longest motif 
        HashMap<String,Integer> imageMap=null; // since the same motif can appear on multiple lines, we will reuse motif logo images
        if (showLogosAsImages) {
            sheet.setColumnWidth(logocolumn, 10000);
            imageMap=new HashMap<>();
        }        
        for (int i=0;i<resultList.size();i++) {
            rownum++;
            row = sheet.createRow(rownum);
            col=0;
            Object[] entry=resultList.get(i);
            String motifID=(String)entry[0];
            String motifname="???";
            String tfbs=(String)entry[1];
            int seqcount=(Integer)entry[2];
            int totalcount=(Integer)entry[3];
            double matchscore=(Double)entry[4];
            Motif motif=null;
            if (engine.dataExists(motifID, Motif.class)) {
                motif=(Motif)engine.getDataItem(motifID);
                motifname=motif.getShortName();
            }               
            outputStringValuesInCells(row, new String[]{motifID,motifname,tfbs}, col);
            col+=3;
            outputNumericValuesInCells(row, new double[]{totalcount,seqcount,matchscore}, col);
            col+=3;            
            if (showSequenceLogos && motif!=null) {
                if (showLogosAsImages) {
                    try {
                        row.setHeightInPoints((short)(sheet.getDefaultRowHeightInPoints()*1.2));
                        int imageIndex=0;
                        if (imageMap.containsKey(motifID)) {
                            imageIndex=imageMap.get(motifID);
                        } else {
                        sequencelogo.setMotif(motif);
                            int width=motif.getLength();
                            if (width>maxlogowidth) maxlogowidth=width;
                            byte[] image=getMotifLogoImageAsByteArray(sequencelogo, logoheight, border, "png");
                            imageIndex=workbook.addPicture(image, XSSFWorkbook.PICTURE_TYPE_PNG);
                            imageMap.put(motifID, imageIndex);
                        }
                        ClientAnchor anchor = helper.createClientAnchor();
                        anchor.setCol1(logocolumn);
                        anchor.setRow1(rownum);
                        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
                        Picture pict=drawing.createPicture(anchor, imageIndex);	
                        pict.resize();
                        int offsetX=25000;
                        int offsetY=25000;
                        anchor.setDx1(offsetX);
                        anchor.setDy1(offsetY);    
                        anchor.setDx2(anchor.getDx2()+offsetX);
                        anchor.setDy2(anchor.getDy2()+offsetY);                        
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
        autoSizeExcelColumns(sheet, 0, 5, 800);
           
        // Add the header on top of the page
        if (includeLegend) {        
            sheet.createFreezePane(0,headerrows,0,headerrows);
            row=sheet.getRow(0);
            outputStringValueInCell(row, 0, "Binding Sequence Occurrence Analysis", title);
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
            row=sheet.getRow(2);
            outputStringValueInCell(row, 0, firstLine.toString(), null);                                        
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
    public void runAnalysis(OperationTask task) throws Exception {
        RegionDataset source=(RegionDataset)task.getParameter("Motif track");
        if (!source.isMotifTrack()) throw new ExecutionError("Binding sequence occurrence analysis can only be performed on motif tracks");
        RegionDataset withinRegions=(RegionDataset)task.getParameter("Within regions");        
        motifTrackName=source.getName();
        if (withinRegions==source) throw new ExecutionError("'Within regions' parameter should not be the same as the 'Motif track' parameter");
        if (withinRegions!=null) withinRegions=withinRegions.flatten(); // this is necessary to avoid overlapping regions in the original dataset
        if (withinRegions!=null) withinRegionsTrackName=withinRegions.getName();        
        MotifCollection motifs=(MotifCollection)task.getParameter("Motifs");
        motifCollectionName=motifs.getName();
        counts=new HashMap<String,HashMap<String,int[]>>();
   
        SequenceCollection collection=(SequenceCollection)task.getParameter("Sequences");
        if (collection==null) collection=task.getEngine().getDefaultSequenceCollection();
        else sequenceCollectionName=collection.getName();
        if (sequenceCollectionName!=null && sequenceCollectionName.equals(task.getEngine().getDefaultSequenceCollectionName())) sequenceCollectionName=null;
        sequenceCollectionSize=collection.size();
        ArrayList<String> motifNames=motifs.getAllMotifNames();
        ArrayList<String> sequenceNames=collection.getAllSequenceNames();
        ignored=0;
        counted=0;
        int motifSize=motifNames.size();
        TaskRunner taskRunner=task.getEngine().getTaskRunner();
        task.setProgress(0L,motifSize);
        long[] counters=new long[]{0,0,motifSize}; // counters[0]=motifs started, [1]=motifs completed, [2]=total number of motifs

        ArrayList<ProcessMotifTask> processTasks=new ArrayList<ProcessMotifTask>(motifSize);
        for (int i=0;i<motifSize;i++) {
            String motifname=motifNames.get(i);
            Data data=task.getEngine().getDataItem(motifname);
            if (data==null || !(data instanceof Motif)) throw new ExecutionError(motifname+" is not a known motif");
            Motif motif=(Motif)data;
            processTasks.add(new ProcessMotifTask(motif, sequenceNames, source, withinRegions, task, counters));
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
        if (countOK!=motifSize) {
            throw new ExecutionError("Some mysterious error occurred while performing analysis: "+getAnalysisName());
        }        
        
    }
    
    private Object[] processMotif(Motif motif, ArrayList<String> sequenceNames, RegionDataset motiftrack, RegionDataset withinRegions, ExecutableTask task) throws Exception {
         // count the motif occurrences in the sequences       
        int counted_here=0;
        int ignored_here=0;
        String motifName=motif.getName();
        HashMap<String,int[]> motifcounts=new HashMap<String,int[]>();
        int s=0;
        for (String sequenceName:sequenceNames) {
            s++;
            RegionSequenceData seq=(RegionSequenceData)motiftrack.getSequenceByName(sequenceName);
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            Thread.yield();
            RegionSequenceData withinRegionsSeq=(withinRegions==null)?null:(RegionSequenceData)withinRegions.getSequenceByName(sequenceName);

            HashSet<String> present=new HashSet<String>();
            for (Region r:seq.getOriginalRegions()) {
              if (withinRegionsSeq!=null && !isRegionInsideOtherRegions(r, withinRegionsSeq)) continue; // target region is not inside one of the 'within region' regions                        
              if (r.getType().equals(motifName)) {
                  String bindingsequence=r.getSequence();
                  if (bindingsequence==null || bindingsequence.isEmpty()) ignored_here++;
                  else {
                      counted_here++;
                      if (!present.contains(bindingsequence)) present.add(bindingsequence);
                      if (!motifcounts.containsKey(bindingsequence)) motifcounts.put(bindingsequence,new int[]{0,0,0}); // [0] is number of sequence containing the TFBS binding sequence, [1] is the total number of occurrences of this TFBS sequence, [2] is the binding sequence "match score" compared to the motif
                      int[] BScounts=motifcounts.get(bindingsequence);
                      BScounts[1]++; // increase total occurrences (this affects the 'counts' variable!) 
                  }
              }
            }
            // increase sequence support for binding sequences present in this sequences
            for (String bindingsequence:present) {
                int[] BScounts=motifcounts.get(bindingsequence);
                BScounts[0]++; // increase sequence occurrences
                if (motif!=null) { // calculate matchscore for binding sequence
                    double score=motif.calculatePWMmatchScore(bindingsequence);
                    BScounts[2]=(int)Math.round(score*MATCH_SCORE_FACTOR); // the score is stored as an integer between 0 and 100000 rather than as a double between 0 and 1.0 for backwards compatibility
                }
            }
        } // end for each sequence (count motifs)
        // counts.put(motifName, motifcounts);       
        return new Object[]{motifcounts,counted_here,ignored_here};    
    }
    

    protected class ProcessMotifTask implements Callable<Motif> {
        final Motif motif;
        final ArrayList<String> sequenceNames;
        final RegionDataset motiftrack;
        final RegionDataset withinRegions;
        final long[] counters; // counters[0]=motifs started, [1]=motifs completed, [2]=total number of motifs.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final ExecutableTask task;  
        
        public ProcessMotifTask(Motif motif, ArrayList<String> sequenceNames, RegionDataset motiftrack, RegionDataset withinRegions, ExecutableTask task, long[] counters) {
           this.motif=motif;
           this.motiftrack=motiftrack;
           this.withinRegions=withinRegions;
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
            
            Object[] motifresults=processMotif(motif, sequenceNames, motiftrack, withinRegions, task);          
            
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                HashMap<String,int[]> motifcounts=(HashMap<String,int[]>)motifresults[0];             
                counts.put(motif.getName(),motifcounts);
                int counted_here=(Integer)motifresults[1];
                int ignored_here=(Integer)motifresults[2];   
                counted+=counted_here;
                ignored+=ignored_here;
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

    
    private class SortOrderComparator implements Comparator<Object[]> {
            String sortorder=null;
            public SortOrderComparator(String order) {
                sortorder=order;
            }
            @Override
            public int compare(Object[] tfbs1, Object[] tfbs2) { //
                 if (sortorder.equals(SORT_BY_SEQUENCE_OCCURRENCES)) {
                     Integer value1=(Integer)tfbs1[2];
                     Integer value2=(Integer)tfbs2[2];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;                     
                     int res=value2.compareTo(value1); // sorts descending!
                     if (res!=0) return res;
                     else return ((Integer)tfbs2[3]).compareTo(((Integer)tfbs1[3])); // if equal, sorts by total count descending!
                } else if (sortorder.equals(SORT_BY_TOTAL_OCCURRENCES)) {
                     Integer value1=(Integer)tfbs1[3];
                     Integer value2=(Integer)tfbs2[3];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;                      
                     int res=value2.compareTo(value1); // sorts descending!
                     if (res!=0) return res;
                     else return ((Integer)tfbs2[2]).compareTo(((Integer)tfbs1[2])); // if equal, sorts by total count descending!
                } else { // sort by TFBS sequence
                    String motifname1=(String)tfbs1[1];
                    String motifname2=(String)tfbs2[1];
                    return motifname1.compareTo(motifname2); // sorts ascending!
                }
            }
    }


    @Override
    protected JPanel getDisplayPanel(MotifLabGUI gui, boolean modal) {
        Color [] basecolors=gui.getVisualizationSettings().getBaseColors(); 

        JPanel displayPanel=new JPanel(new BorderLayout());
        JPanel headerPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        StringBuilder headerString=new StringBuilder("<html>Binding sequence occurrence analysis with motifs from <b>");
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

        headerString.append("</html>");
        headerPanel.add(new JLabel(headerString.toString()));
        TFBSOccurrenceTableModel tablemodel=new TFBSOccurrenceTableModel(gui);
        CellRenderer_Support renderer=new CellRenderer_Support();
        GenericMotifBrowserPanel panel=new GenericMotifBrowserPanel(gui, tablemodel, modal);
        final JTable table=panel.getTable(); 
        int rowheight=table.getRowHeight();
        final MotifLogoRenderer logorenderer=new MotifLogoRenderer(basecolors,(int)(rowheight*1.25));
        logorenderer.setUseAntialias(gui.getVisualizationSettings().useMotifAntialiasing());        
        table.getColumn("Sequences").setCellRenderer(renderer);
        table.getColumn("Sequences").setPreferredWidth(80);
        table.getColumn("Total").setPreferredWidth(60);
        table.getColumn("Logo").setCellRenderer(logorenderer);
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
               if (table.isCellEditable(table.getSelectionModel().getLeadSelectionIndex(), table.getColumnModel().getSelectionModel().getLeadSelectionIndex())) return; // do nothing if cell can be edited, so as to not interfere with editing process               
               if (e.getKeyCode()==KeyEvent.VK_PLUS || e.getKeyCode()==KeyEvent.VK_ADD) {
                    int newheight=table.getRowHeight()+1;
                    if (newheight>80) return;
                    logorenderer.setFontHeight((int)(newheight*1.25));
                    table.setRowHeight(newheight);
                } else if (e.getKeyCode()==KeyEvent.VK_MINUS || e.getKeyCode()==KeyEvent.VK_SUBTRACT) {
                    int newheight=table.getRowHeight()-1;
                    if (newheight<8) return;
                    logorenderer.setFontHeight((int)(newheight*1.25));
                    table.setRowHeight(newheight);
                } else if (e.getKeyCode()==KeyEvent.VK_L) {
                    logorenderer.setScaleByIC(!logorenderer.getScaleByIC());
                    table.repaint();
                } 
            }
        });        
        panel.setPreferredSize(new java.awt.Dimension(600,500));
        
        table.getRowSorter().toggleSortOrder(TOTAL);
        table.getRowSorter().toggleSortOrder(TOTAL);
        table.getRowSorter().toggleSortOrder(MOTIF); // sort first by motifname, then descending by total count
        
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8,5,0,5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)
          ));
        displayPanel.add(headerPanel,BorderLayout.NORTH);
        displayPanel.add(panel,BorderLayout.CENTER);
        return displayPanel;
    }
    
private class MotifLogoRenderer extends MotifLogo {
    
    public MotifLogoRenderer(Color[] basecolors, int fontheight) {
        super(basecolors, fontheight);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component comp=super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String tfbs=(String)table.getValueAt(row, TFBS);
        this.setBindingSequence(tfbs);
        return comp;
    }    
}    

private class TFBSOccurrenceTableModel extends AbstractTableModel {
    private String[] columnNames=null;
    private ArrayList<String> motifIDs=null;
    private ArrayList<String> tfbs=null;
    private MotifLabEngine engine;


    public TFBSOccurrenceTableModel(MotifLabGUI gui) {
        this.engine=gui.getEngine();
        motifIDs=new ArrayList<String>();
        tfbs=new ArrayList<String>();     
        for (String motifname:counts.keySet()) {
           HashMap<String,int[]> motifcounts=counts.get(motifname);
           for (String tfbsSequence:motifcounts.keySet()) {
                 motifIDs.add(motifname);
                 tfbs.add(tfbsSequence);
           }
        }
        columnNames=new String[]{"Motif","Name","TFBS","Total","Sequences","Match score","Logo"};
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {      
            case MOTIF:return String.class;
            case NAME:return String.class;             
            case TFBS:return String.class;            
            case TOTAL:return Integer.class;
            case SEQUENCE_SUPPORT:return Integer.class;            
            case MATCH_SCORE:return Double.class;            
            case LOGO:return Motif.class;
            default:return Object.class;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {       
            case MOTIF:return motifIDs.get(rowIndex);
            case NAME:return getMotifName(motifIDs.get(rowIndex));           
            case TFBS:return tfbs.get(rowIndex);          
            case TOTAL:return getIntValueAt(rowIndex,1);
            case SEQUENCE_SUPPORT:return getIntValueAt(rowIndex,0);            
            case MATCH_SCORE:return getMatchValue(rowIndex,2);            
            case LOGO:return getMotif(motifIDs.get(rowIndex));
            default:return Object.class;
        }
    }
    
    public final Motif getMotif(String id) {
        Data data=engine.getDataItem(id);
        if (data instanceof Motif) return (Motif)data;
        else return null;
    }

    public final String getMotifName(String id) {
        Data data=engine.getDataItem(id);
        if (data instanceof Motif) return ((Motif)data).getShortName();
        else return null;
    }     
    
    private int getIntValueAt(int row,int col) { // this method is an ad-hoc solution to a casting problem that sometimes occur (perhaps from old sessions)
        String motifname=motifIDs.get(row);
        HashMap<String,int[]> motifcounts=counts.get(motifname);
        Object countsrow=motifcounts.get(tfbs.get(row));
        if (countsrow instanceof double[]) {
            double[] rowcounts=(double[])countsrow;
            if (col>=rowcounts.length) return 0;
            double value=rowcounts[col];
            return (int)value;
        } else if (countsrow instanceof int[]) {
            int[] rowcounts=(int[])countsrow;
            if (col>=rowcounts.length) return 0;
            return rowcounts[col];         
        } else return 0; // this should not happen
    }    
    
    private double getMatchValue(int row,int col) { // this method is an ad-hoc solution to a casting problem that sometimes occur (perhaps from old sessions)
        String motifname=motifIDs.get(row);
        HashMap<String,int[]> motifcounts=counts.get(motifname);
        Object countsrow=motifcounts.get(tfbs.get(row));
        if (countsrow instanceof double[]) {
            double[] rowcounts=(double[])countsrow;
            if (col>=rowcounts.length) return 0;
            double value=rowcounts[col]/MATCH_SCORE_FACTOR*100; // return as percentage value between 0 and 100
            return value;
        } else if (countsrow instanceof int[]) {
            int[] rowcounts=(int[])countsrow;
            if (col>=rowcounts.length) return 0;
            return (double)rowcounts[col]/MATCH_SCORE_FACTOR*100; // return as percentage value between 0 and 100         
        } else return 0.0; // this should not happen
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
        return motifIDs.size();
    }

}

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



    private HashMap<String,HashMap<String,int[]>> reverseBindingSequences(HashMap<String,HashMap<String,int[]>> countsMap) {
        HashMap<String,HashMap<String,int[]>> newcounts=new HashMap<String,HashMap<String,int[]>>();
        for (String motifName:countsMap.keySet()) {
            HashMap<String,int[]> innermap=countsMap.get(motifName);
            HashMap<String,int[]> newInnerMap=new HashMap<String,int[]>();
            for (String tfbs:innermap.keySet()) {
                String reverseTFBS=MotifLabEngine.reverseSequence(tfbs);
                int[] originalArray=innermap.get(tfbs);
                int[] newArray=Arrays.copyOf(originalArray, originalArray.length);
                newInnerMap.put(reverseTFBS,newArray);               
            }
            newcounts.put(motifName, newInnerMap);
        }
        return newcounts;
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
