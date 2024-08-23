/*


 */

package motiflab.engine.data.analysis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.dataformat.DataFormat;
import motiflab.gui.GenericModuleBrowserPanel;
import motiflab.gui.ModuleLogo;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.VisualizationSettings;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author kjetikl
 */
public class ModuleOccurrenceAnalysis extends Analysis {
    private final static String typedescription="Analysis: count module occurrences";
    private final static String analysisName="count module occurrences";
    private final static String description="Counts the number of occurrences of each module in each sequence and the total number";
    private static final String SORT_BY_MODULE="Module ID";
    private static final String SORT_BY_TOTAL_OCCURRENCES="Total occurrences";
    private static final String SORT_BY_SEQUENCE_OCCURRENCES="Sequence occurrences";
    private static final String OUTPUT_ALL="All modules";
    private static final String OUTPUT_PRESENT="Only present modules";
    private HashMap<String,double[]> counts=null; // key is module name. Value is double[]{sequence support, total count}
    private int sequenceCollectionSize=0;
    private String moduleCollectionName=null;
    private String moduleTrackName=null;
    private String sequenceCollectionName=null;
    private String withinRegionsTrackName=null;

    private static final int COLOR=0;
    private static final int MODULE_ID=1;
    private static final int SEQUENCE_SUPPORT=2;
    private static final int TOTAL=3;
    private static final int LOGO=4;

    public ModuleOccurrenceAnalysis() {
        this.name="ModuleOccurrenceAnalysis_temp";
        addParameter("Module track",RegionDataset.class, null,new Class[]{RegionDataset.class},"A region track containing module sites",true,false);
        addParameter("Modules",ModuleCollection.class, null,new Class[]{ModuleCollection.class},"The modules to consider in this analysis",true,false);
        addParameter("Sequences",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"If specified, the analysis will be limited to sequences in this collection",false,false);
        addParameter("Within regions",RegionDataset.class, null,new Class[]{RegionDataset.class},"Limits the analysis to modules found within the selected regions and not the full sequences",false,false);     }

    @Override
    public String getAnalysisName() {
        return analysisName;
    }

    @Override
    public String getDescription() {return description;}


    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Module track","Modules"};}

    @Override
    public boolean canUseAsSourceProxy(Data data) {
        if (data instanceof RegionDataset) return ((RegionDataset)data).isModuleTrack(); // only allow Module Tracks as input
        else return (data instanceof ModuleCollection);
    }     
    
    @Override
    public Parameter[] getOutputParameters(String dataformat) {     
        Parameter incPar = new Parameter("Include",ModuleCollection.class,null,new Class[]{ModuleCollection.class},"Only include data from this collection",false,false);
        Parameter sortPar = new Parameter("Sort by",String.class,SORT_BY_SEQUENCE_OCCURRENCES, new String[]{SORT_BY_MODULE,SORT_BY_SEQUENCE_OCCURRENCES,SORT_BY_TOTAL_OCCURRENCES},"Sorting order for the results table",false,false);
        Parameter logos = new Parameter("Logos",String.class,getMotifLogoDefaultOption(dataformat), getMotifLogoOptions(dataformat),"Include module logos in the table",false,false);
        Parameter colorPar = new Parameter("Color boxes",Boolean.class,Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a box with the assigned color for the module will be output as the first column",false,false); 
        Parameter legendPar = new Parameter("Legend",Boolean.class,Boolean.TRUE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a header with a title and analysis details will be included at the top of the Excel sheet.",false,false);       
        if (dataformat.equals(HTML)) return new Parameter[]{incPar,sortPar,logos,colorPar};
        if (dataformat.equals(EXCEL)) return new Parameter[]{incPar,sortPar,logos,legendPar};
        if (dataformat.equals(RAWDATA)) return new Parameter[]{incPar,sortPar,logos};
        return new Parameter[0]; 
    }

//    @Override
//    public String[] getOutputParameterFilter(String parameter) {
//        if (parameter.equals("Color boxes")) return new String[]{HTML};
//        if (parameter.equals("Legend")) return new String[]{EXCEL};
//        if (parameter.equals("Include") || parameter.equals("Sort by") || parameter.equals("Logos")) return new String[]{HTML,RAWDATA,EXCEL};       
//        return null;
//    }      
    
    @Override
    public String[] getResultVariables() {
        return new String[]{"support","total","present"};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename==null || variablename.isEmpty()) throw new ExecutionError("'"+getName()+"' does not have a result for ''");
        else if (variablename.equals("support")) {
            ModuleNumericMap map=new ModuleNumericMap("temp",0);
            for (String modulename:counts.keySet()) {
                double[] stats=counts.get(modulename);
                map.setValue(modulename, stats[0]);
            }
            return map;
        }
        else if (variablename.equals("total")) {
            ModuleNumericMap map=new ModuleNumericMap("temp",0);
            for (String modulename:counts.keySet()) {
                double[] stats=counts.get(modulename);
                map.setValue(modulename, stats[1]);
            }
            return map;
        }
        else if (variablename.equals("present")) {
            ModuleCollection collection=new ModuleCollection("temp");         
            for (String modulename:counts.keySet()) {
                    double[] c=counts.get(modulename);
                    double occurrences=c[1];
                    if (occurrences>0) {
                        Data module=engine.getDataItem(modulename);
                        if (module instanceof Module) collection.addModule((Module)module);
                    }
            }
            return collection;
        }          
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else if (variablename.equals("present")) return ModuleCollection.class;
       else return ModuleNumericMap.class;
    }

    @Override
    public String[] getColumnsExportedForCollation() {
        return new String[]{"support","total"};
    }

    @Override
    public Class getCollateType() {
        return Module.class;
    }

    @Override
    public HashMap<String,Object> getColumnData(String column) throws ExecutionError {
        Class coltype=getColumnType(column);
        if (coltype==null) throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
        HashMap<String,Object> columnData=new HashMap<String, Object>();
        for (String modulename:counts.keySet()) {
            double[] stat=counts.get(modulename);
            if (column.equalsIgnoreCase("support")) columnData.put(modulename, new Integer((int)stat[0]));
            else if (column.equalsIgnoreCase("total")) columnData.put(modulename, new Integer((int)stat[1]));
        }
        return columnData;
    }

    @Override
    public Class getColumnType(String column) {
             if (column.equalsIgnoreCase("support")) return Integer.class;
        else if (column.equalsIgnoreCase("total")) return Integer.class;
        else return null;
    }


    @Override
    @SuppressWarnings("unchecked")
    public ModuleOccurrenceAnalysis clone() {
        ModuleOccurrenceAnalysis newanalysis=new ModuleOccurrenceAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.counts=(HashMap<String,double[]>)this.counts.clone();
        newanalysis.sequenceCollectionSize=this.sequenceCollectionSize;
        newanalysis.moduleCollectionName=this.moduleCollectionName;
        newanalysis.moduleTrackName=this.moduleTrackName;
        newanalysis.sequenceCollectionName=this.sequenceCollectionName;
        newanalysis.withinRegionsTrackName=this.withinRegionsTrackName;
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        this.counts=(HashMap<String,double[]>)((ModuleOccurrenceAnalysis)source).counts;
        this.sequenceCollectionSize=((ModuleOccurrenceAnalysis)source).sequenceCollectionSize;
        this.moduleCollectionName=((ModuleOccurrenceAnalysis)source).moduleCollectionName;
        this.moduleTrackName=((ModuleOccurrenceAnalysis)source).moduleTrackName;
        this.sequenceCollectionName=((ModuleOccurrenceAnalysis)source).sequenceCollectionName;
        this.withinRegionsTrackName=((ModuleOccurrenceAnalysis)source).withinRegionsTrackName;
    }

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }

    @Override
    public String getTypeDescription() {return typedescription;}

    /** Constructs a sorted list with the module name, total occurrence count and sequence count
     *  This method is contains code common for both formatHTML and formatRaw
     */
    private ArrayList<Object[]> assembleList(String sortorder, ModuleCollection include, MotifLabEngine engine) {
        ArrayList<Object[]> resultList=new ArrayList<Object[]>(counts.size());
        Set<String> keys=counts.keySet();
        Iterator<String> iterator=keys.iterator();
        int i=0;
        while (iterator.hasNext()) {
            i++;
            String modulekey=iterator.next();
            if (include!=null && !include.contains(modulekey)) continue;
            double[] values=counts.get(modulekey);
            int sequencesupport=(int)values[0];
            int totalcount=(int)values[1];
            resultList.add(new Object[]{modulekey,new Integer(sequencesupport), new Integer(totalcount)});
        }
        Collections.sort(resultList, new SortOrderComparator(sortorder));
        return resultList;
    }

    @Override
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();        
        String sortorder=SORT_BY_MODULE;
        String showSequenceLogosString="";
        ModuleCollection include=null;
        boolean showColorBoxes=false;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             include=(ModuleCollection)settings.getResolvedParameter("Include",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
             showColorBoxes=(Boolean)settings.getResolvedParameter("Color boxes",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos = includeLogosInOutput(showSequenceLogosString);
        ArrayList<Object[]> resultList=assembleList(sortorder,include,engine);
        engine.createHTMLheader("Module Occurrence Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<h1 class=\"headline\">Module Occurrence Analysis</h1>\n",HTML);
        outputobject.append("<div class=\"summary\">\n",HTML);
        outputobject.append("Analysis performed with modules from <span class=\"dataitem\">",HTML);
        outputobject.append(moduleCollectionName,HTML);
        outputobject.append("</span> and sites from <span class=\"dataitem\">",HTML);
        outputobject.append(moduleTrackName,HTML);
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
        if (showColorBoxes) outputobject.append("<th>&nbsp;</th>",HTML);               
        outputobject.append("<th>ID</th>",HTML);
        outputobject.append("<th>Total</th>",HTML);
        outputobject.append("<th>Sequences</th>",HTML);
        outputobject.append("<th class=\"sorttable_numeric\">%</th>",HTML);
        if (showSequenceLogos) outputobject.append("<th class=\"sorttable_nosort\">Logo</th>",HTML);
        outputobject.append("</tr>\n",HTML);
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String modulename=(String)entry[0];
            int seqcount=(Integer)entry[1];
            int totalcount=(Integer)entry[2];
            Module module=null;
            if (engine.dataExists(modulename, Module.class)) module=(Module)engine.getDataItem(modulename);             
            outputobject.append("<tr>",HTML);
            if (showColorBoxes) {
                Color color=Color.WHITE;               
                if (module!=null) color=vizSettings.getFeatureColor(modulename);             
                String colorString=VisualizationSettings.convertColorToHTMLrepresentation(color);
                outputobject.append("<td><div style=\"width:10px;height:10px;border:1px solid #000;background-color:"+colorString+";\"></div></td>",HTML);
            }              
            outputobject.append("<td>"+escapeHTML(modulename)+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+totalcount+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+seqcount+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+(int)((double)seqcount*100f/(double)sequenceCollectionSize)+"%</td>",HTML);
            if (showSequenceLogos) {              
                outputobject.append("<td>",HTML);
                outputobject.append(getModuleLogoTag(module, outputobject, showSequenceLogosString, engine),HTML);
                outputobject.append("</td>",HTML);
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
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();  
        boolean border=(Boolean)vizSettings.getSettingAsType("module.border", Boolean.FALSE);
        String sortorder=SORT_BY_MODULE;
        String showSequenceLogosString="";
        ModuleCollection include=null;
        int logoheight=28;
        boolean includeLegend=false;
        ModuleLogo modulelogo=new ModuleLogo(vizSettings);
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             include=(ModuleCollection)settings.getResolvedParameter("Include",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
             includeLegend=(Boolean)settings.getResolvedParameter("Legend",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos = includeLogosInOutput(showSequenceLogosString);          
        boolean showLogosAsImages = includeLogosInOutputAsImages(showSequenceLogosString);                    
    
        ArrayList<Object[]> resultList=assembleList(sortorder,include,engine);

        int rownum=0;
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Module occurrences");
        CreationHelper helper = (showLogosAsImages)?workbook.getCreationHelper():null;
        Drawing drawing = (showLogosAsImages)?sheet.createDrawingPatriarch():null;       
        
        CellStyle title=getExcelTitleStyle(workbook);
        CellStyle tableheader=getExcelTableHeaderStyle(workbook);
        CellStyle normal=createExcelStyle(workbook, BorderStyle.NONE, null, HorizontalAlignment.GENERAL, false);      
        normal.setVerticalAlignment(VerticalAlignment.CENTER);
        
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
        outputStringValuesInCells(row, new String[]{"Module ID","Total","Sequences"}, 0, tableheader);      
        col+=3;
        if (showSequenceLogos) {
            logocolumn=col;
            outputStringValuesInCells(row, new String[]{"Logo"}, logocolumn, tableheader);
        }       
        if (showLogosAsImages) sheet.setColumnWidth((short)3, 10000);  // this should be wide enough for most purposes      
        for (int i=0;i<resultList.size();i++) {
            rownum++;
            row = sheet.createRow(rownum);
            col=0;
            Object[] entry=resultList.get(i);
            String modulename=(String)entry[0];
            int seqcount=(Integer)entry[1];
            int totalcount=(Integer)entry[2];           
            outputStringValueInCell(row, col, modulename, normal);
            col+=1;
            outputNumericValuesInCells(row, new double[]{totalcount,seqcount}, col, normal);
            col+=2;
            Module module=null;
            if (engine.dataExists(modulename, Module.class)) module=(Module)engine.getDataItem(modulename);             

            if (showSequenceLogos && module!=null) {
                if (showLogosAsImages) {
                    try {
                        row.setHeightInPoints((short)(logoheight));                        
                        modulelogo.setModule(module);                        
                        byte[] image=getModuleLogoImageAsByteArray(modulelogo, logoheight, border, "png");
                        int imageIndex=workbook.addPicture(image, XSSFWorkbook.PICTURE_TYPE_PNG);
                        ClientAnchor anchor = helper.createClientAnchor();
                        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
                        anchor.setCol1(logocolumn);
                        anchor.setRow1(rownum);                                                
                        Picture pict=drawing.createPicture(anchor, imageIndex);	
                        pict.resize();   
                        // now offset the image a little bit down and to the left to avoid overlap with the Excel cell borders
                        int offsetX=25000;
                        int offSetY=25000;
                        anchor.setDx1(anchor.getDx1()+offsetX); // top-left
                        anchor.setDy1(anchor.getDy1()+offSetY); // top-left  
                        anchor.setDx2(anchor.getDx2()+offsetX); // bottom-right
                        anchor.setDy2(anchor.getDy2()+offSetY); // bottom-right
                    } catch (Exception e) {throw new ExecutionError(e.getMessage(),e);}
                }
                else outputStringValuesInCells(row, new String[]{module.getModuleLogo()}, logocolumn, normal);
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
        autoSizeExcelColumns(sheet, 0,2 ,800);     
        if (!showLogosAsImages) sheet.autoSizeColumn((short)3);
        
        // Add the header on top of the page
        if (includeLegend) {        
            sheet.createFreezePane(0,headerrows,0,headerrows);
            row=sheet.getRow(0);
            outputStringValueInCell(row, 0, "Module Occurrence Analysis", title);
            StringBuilder firstLine=new StringBuilder();
            firstLine.append("Analysis performed with modules from \"");
            firstLine.append(moduleCollectionName);
            firstLine.append("\" and sites from \"");
            firstLine.append(moduleTrackName);
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
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder=SORT_BY_MODULE;
        String showSequenceLogosString="";
        ModuleCollection include=null;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             include=(ModuleCollection)settings.getResolvedParameter("Include",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        boolean showSequenceLogos = includeLogosInOutput(showSequenceLogosString);
        ArrayList<Object[]> resultList=assembleList(sortorder,include,engine);
        outputobject.append("# Module occurrence analysis with modules from '"+moduleCollectionName+"' and sites from '"+moduleTrackName+"'",RAWDATA);
        if (withinRegionsTrackName!=null) {
            outputobject.append(" within '",RAWDATA);
            outputobject.append(withinRegionsTrackName,RAWDATA);
            outputobject.append("' regions",RAWDATA);
        }
        outputobject.append(" on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),RAWDATA);
        if (sequenceCollectionName!=null) outputobject.append(" from collection '"+sequenceCollectionName+"'",RAWDATA);
        outputobject.append("\n\n#Module ID, total occurrences, number of sequences containing module, total number of sequences, percentage of sequences containing module",RAWDATA);
        if (showSequenceLogos) outputobject.append(", module logo",RAWDATA);
        outputobject.append("\n",RAWDATA);
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String modulename=(String)entry[0];
            int seqcount=(Integer)entry[1];
            int totalcount=(Integer)entry[2];
            Module module=null;
            if (engine.dataExists(modulename, Module.class)) module=(Module)engine.getDataItem(modulename);
            outputobject.append(modulename+"\t"+totalcount+"\t"+seqcount+"\t"+sequenceCollectionSize+"\t"+(int)((double)seqcount*100/(double)sequenceCollectionSize)+"%",RAWDATA);
            if (showSequenceLogos) {
                if (module!=null) outputobject.append("\t"+module.getModuleLogo(),RAWDATA);
                else outputobject.append("\t?",RAWDATA);
            }
            outputobject.append("\n",RAWDATA);
            if (i%100==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                Thread.yield();
            }
            task.setStatusMessage("Executing operation: output ("+i+"/"+resultList.size()+")");
            format.setProgress(i, resultList.size()); //
        }
        format.setProgress(100);
        return outputobject;
    }


    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        RegionDataset source=(RegionDataset)task.getParameter("Module track");
        if (!source.isModuleTrack()) throw new ExecutionError("Module occurrence analysis can only be performed on module tracks");
        RegionDataset withinRegions=(RegionDataset)task.getParameter("Within regions");
        moduleTrackName=source.getName();
        if (withinRegions==source) throw new ExecutionError("'Within regions' parameter should not be the same as the 'Module track' parameter");
        if (withinRegions!=null) withinRegions=withinRegions.flatten();
        if (withinRegions!=null) withinRegionsTrackName=withinRegions.getName();
        ModuleCollection modulecollection=(ModuleCollection)task.getParameter("Modules");
        moduleCollectionName=modulecollection.getName();
        counts=new HashMap<String,double[]>(modulecollection.size());
        for (String modulename:modulecollection.getAllModuleNames()) {
            counts.put(modulename, new double[]{0,0});
        }
        SequenceCollection collection=(SequenceCollection)task.getParameter("Sequences");
        if (collection==null) collection=task.getEngine().getDefaultSequenceCollection();
        else sequenceCollectionName=collection.getName();
        if (sequenceCollectionName!=null && sequenceCollectionName.equals(task.getEngine().getDefaultSequenceCollectionName())) sequenceCollectionName=null;
        sequenceCollectionSize=collection.size();
        MotifLabEngine engine=task.getEngine();
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
              if (counts.containsKey(type)) {
                  if (!present.contains(type)) present.add(type);
                  double[] modulecounts=counts.get(type);
                  modulecounts[1]++; // total occurrences
              }
            }
            // increase sequence support for modules present in this sequences
            for (String module:present) {
                double[] modulecounts=counts.get(module);
                modulecounts[0]++; // sequence occurrences
            }
        } // end for each sequence
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
            public int compare(Object[] module1, Object[] module2) { //
                 if (sortorder.equals(SORT_BY_SEQUENCE_OCCURRENCES)) {
                     Integer value1=(Integer)module1[1];
                     Integer value2=(Integer)module2[1];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;                         
                     int res=value2.compareTo(value1); // sorts descending!
                     if (res!=0) return res;
                     else return ((Integer)module2[2]).compareTo(((Integer)module1[2])); // if equal, sorts by total count descending!
                } else if (sortorder.equals(SORT_BY_TOTAL_OCCURRENCES)) {
                     Integer value1=(Integer)module1[2];
                     Integer value2=(Integer)module2[2];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;                         
                     int res=value2.compareTo(value1); // sorts descending!
                     if (res!=0) return res;
                     else return ((Integer)module2[1]).compareTo(((Integer)module1[1])); // if equal, sorts by total count descending!
                } else { // sort by module ID
                    String modulename1=(String)module1[0];
                    String modulename2=(String)module2[0];
                    return modulename1.compareTo(modulename2); // sorts ascending!
                }
            }
    }


    @Override
    protected JPanel getDisplayPanel(MotifLabGUI gui, boolean modal) {
        JPanel displayPanel=new JPanel(new BorderLayout());
        JPanel headerPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        StringBuilder headerString=new StringBuilder("<html>Module occurrence analysis with modules from <b>");
        headerString.append(moduleCollectionName);
        headerString.append("</b> and sites from <b>");
        headerString.append(moduleTrackName);
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
        ModuleOccurrenceTableModel tablemodel=new ModuleOccurrenceTableModel(gui);
        GenericModuleBrowserPanel panel=new GenericModuleBrowserPanel(gui, tablemodel, modal);
        CellRenderer_Support supportrenderer=new CellRenderer_Support();
        JTable table=panel.getTable();
        table.getColumn("Sequences").setCellRenderer(supportrenderer);
        table.getColumn("ID").setPreferredWidth(60);
        table.getColumn("Sequences").setPreferredWidth(80);
        table.getColumn("Total").setPreferredWidth(60);
        panel.setPreferredSize(new java.awt.Dimension(600,500));
        table.getRowSorter().toggleSortOrder(TOTAL);
        table.getRowSorter().toggleSortOrder(TOTAL);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(8,5,0,5),
            BorderFactory.createBevelBorder(BevelBorder.RAISED)
        ));
        displayPanel.add(headerPanel,BorderLayout.NORTH);
        displayPanel.add(panel,BorderLayout.CENTER);
        return displayPanel;
    }

private class ModuleOccurrenceTableModel extends AbstractTableModel {
    private String[] columnNames=null;
    private String[] modulenames=null;
    private MotifLabEngine engine;
    private VisualizationSettings settings;

    public ModuleOccurrenceTableModel(MotifLabGUI gui) {
        this.engine=gui.getEngine();
        this.settings=gui.getVisualizationSettings();
        modulenames=new String[counts.size()];
        int i=0;
        for (String name:counts.keySet()) {
           modulenames[i]=name;
           i++;
        }
        columnNames=new String[]{" ","ID","Sequences","Total","Logo"};

    }

    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case COLOR:return Color.class;
            case MODULE_ID:return String.class;
            case SEQUENCE_SUPPORT:return Integer.class;
            case TOTAL:return Integer.class;
            case LOGO:return Module.class;
            default:return Object.class;
        }
    }

    public final Module getModule(String id) {
        Data data=engine.getDataItem(id);
        if (data instanceof Module) return (Module)data;
        else return null;
    }


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLOR:return settings.getFeatureColor(modulenames[rowIndex]);
            case MODULE_ID:return modulenames[rowIndex];
            case SEQUENCE_SUPPORT:return getIntValueAt(rowIndex,0);
            case TOTAL:return getIntValueAt(rowIndex,1);
            case LOGO:return getModule(modulenames[rowIndex]);
            default:return Object.class;
        }
    }

    private int getIntValueAt(int row,int col) { // this method is an ad-hoc solution to a casting problem that sometimes occur (perhaps from old sessions)
        Object countsrow=counts.get(modulenames[row]);
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
        return modulenames.length;
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
           int percentage=(int)(((double)support*100)/(double)sequenceCollectionSize);
           String text=support+" of "+sequenceCollectionSize+" ("+percentage+"%)";
           setText(text);
           setToolTipText(text);
       } else if (value!=null && value instanceof Double) {
           String text=""+((Double)value).doubleValue();
           setText(text);
           setToolTipText(text);
       }
   }
}// end class CellRenderer_Support


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
