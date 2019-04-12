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
import motiflab.gui.GenericRegionBrowserPanel;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.VisualizationSettings;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;

/**
 *
 * @author kjetikl
 */
public class RegionOccurrenceAnalysis extends Analysis {
    private final static String typedescription="Analysis: count region occurrences";
    private final static String analysisName="count region occurrences";
    private final static String description="Counts the number of occurrences of each region type in each sequence and the total number";
    private static final String SORT_BY_TYPE="Region type";
    private static final String SORT_BY_TOTAL_OCCURRENCES="Total occurrences";
    private static final String SORT_BY_SEQUENCE_OCCURRENCES="Sequence occurrences"; 
    private HashMap<String,double[]> counts=null; // key is region name. Value is double[]{sequence support, total count}
    private int sequenceCollectionSize=0;
    private String regionTrackName=null;
    private String sequenceCollectionName=null; 


    
    public RegionOccurrenceAnalysis() {
        this.name="RegionOccurrenceAnalysis_temp";
        addParameter("Region track",RegionDataset.class, null,new Class[]{RegionDataset.class},"A region track",true,false);
        addParameter("Sequences",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"If specified, the analysis will be limited to sequences in this collection",false,false);
    }

    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Region track","Sequences"};}
    
    @Override
    public boolean canUseAsSourceProxy(Data data) {
        if (data instanceof RegionDataset) return !(((RegionDataset)data).isMotifTrack() || ((RegionDataset)data).isModuleTrack()); // this is not used for Motif or Module tracks (although possible, the specific analysis for those track types are preferred)
        else return (data instanceof SequenceCollection);
    }  
    
    
    @Override
    public Parameter[] getOutputParameters() {
        return new Parameter[] {
             new Parameter("Sort by",String.class,SORT_BY_SEQUENCE_OCCURRENCES, new String[]{SORT_BY_TYPE,SORT_BY_SEQUENCE_OCCURRENCES,SORT_BY_TOTAL_OCCURRENCES},"Sorting order for the results table",false,false),
             new Parameter("Legend",Boolean.class,Boolean.TRUE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, a header with a title and analysis details will be included at the top of the Excel sheet.",false,false)       
        };
    }
    
    @Override
    public String[] getOutputParameterFilter(String parameter) {
        if (parameter.equals("Legend")) return new String[]{EXCEL};        
        if (parameter.equals("Sort by")) return new String[]{HTML,RAWDATA,EXCEL};        
        return null;
    }      

    @Override
    public String[] getResultVariables() {
        return new String[]{};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       //if (!hasResult(variablename)) return null;
       return null;
    }

    @Override
    public String getAnalysisName() {
        return analysisName;
    }

    @Override
    public String getDescription() {return description;}


    @Override
    @SuppressWarnings("unchecked")
    public RegionOccurrenceAnalysis clone() {
        RegionOccurrenceAnalysis newanalysis=new RegionOccurrenceAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.counts=(HashMap<String,double[]>)this.counts.clone();
        newanalysis.sequenceCollectionSize=this.sequenceCollectionSize;
        newanalysis.regionTrackName=this.regionTrackName;
        newanalysis.sequenceCollectionName=this.sequenceCollectionName;   
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        this.counts=(HashMap<String,double[]>)((RegionOccurrenceAnalysis)source).counts;
        this.sequenceCollectionSize=((RegionOccurrenceAnalysis)source).sequenceCollectionSize;
        this.regionTrackName=((RegionOccurrenceAnalysis)source).regionTrackName;
        this.sequenceCollectionName=((RegionOccurrenceAnalysis)source).sequenceCollectionName;     
    }

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

    /** Constructs a sorted list with the region name, total occurrence count and sequence count
     *  This method is contains code common for both formatHTML and formatRaw
     */
    private ArrayList<Object[]> assembleList(String sortorder, MotifLabEngine engine) {
        ArrayList<Object[]> resultList=new ArrayList<Object[]>(counts.size());
        Set<String> keys=counts.keySet();
        Iterator<String> iterator=keys.iterator();
        int i=0;      
        while (iterator.hasNext()) {
            i++;
            String regionkey=iterator.next();
            double[] values=counts.get(regionkey);
            int sequencesupport=(int)values[0];            
            int totalcount=(int)values[1];       
            resultList.add(new Object[]{regionkey,new Integer(sequencesupport), new Integer(totalcount)});
        }
        Collections.sort(resultList, new SortOrderComparator(sortorder));
        return resultList;
    }

    @Override
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder=SORT_BY_TYPE;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        ArrayList<Object[]> resultList=assembleList(sortorder,engine);
        engine.createHTMLheader("Region Occurrence Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<h1 class=\"headline\">Region Occurrence Analysis</h1>\n",HTML);
        outputobject.append("<div class=\"summary\">\n",HTML);
        outputobject.append("Analysis performed on regions from <span class=\"dataitem\">",HTML);
        outputobject.append(regionTrackName,HTML);
        outputobject.append("</span>",HTML);
        outputobject.append(" on "+sequenceCollectionSize,HTML);
        outputobject.append(" sequence"+((sequenceCollectionSize!=1)?"s":""),HTML);
        if (sequenceCollectionName!=null) {
            outputobject.append(" from collection <span class=\"dataitem\">",HTML);
            outputobject.append(sequenceCollectionName,HTML);
            outputobject.append("</span>",HTML);
        }
        outputobject.append(".\n</div>\n<br>\n",HTML);
        outputobject.append("<table class=\"sortable\">",HTML);
        outputobject.append("<tr><th>Region</th><th>Total</th><th>Sequences</th><th class=\"sorttable_numeric\">%</th></tr>\n",HTML);
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String regionname=(String)entry[0];
            int seqcount=(Integer)entry[1];
            int totalcount=(Integer)entry[2];
            outputobject.append("<tr><td>"+escapeHTML(regionname)+"</td><td class=\"num\">"+totalcount+"</td><td class=\"num\">"+seqcount+"</td><td class=\"num\">"+(int)((double)seqcount*100f/(double)sequenceCollectionSize)+"%</td></tr>\n",HTML);
            if (i%30==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                Thread.yield();
            }
            task.setStatusMessage("Executing operation: output ("+i+"/"+resultList.size()+")");
            format.setProgress(i, resultList.size()); //
        }
        outputobject.append("</table>\n</body>\n</html>\n",HTML);
        format.setProgress(100);
        return outputobject;
    }
    
    @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {       
        String sortorder=SORT_BY_TYPE;
        boolean includeLegend=false;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
             includeLegend=(Boolean)settings.getResolvedParameter("Legend",defaults,engine);             
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        ArrayList<Object[]> resultList=assembleList(sortorder,engine);

        int rownum=0;
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(outputobject.getName());  
        
        CellStyle title=createExcelStyle(workbook, HSSFCellStyle.BORDER_NONE, (short)0, HSSFCellStyle.ALIGN_LEFT, false);      
        addFontToExcelCellStyle(workbook, title, null, (short)(workbook.getFontAt((short)0).getFontHeightInPoints()*2.5), true, false);
        CellStyle tableheader=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.LIGHT_YELLOW.index, HSSFCellStyle.ALIGN_CENTER, true);      
 
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
        outputStringValuesInCells(row, new String[]{"Type","Total","Sequences"}, 0, tableheader);      
        col+=3;
        for (int i=0;i<resultList.size();i++) {
            rownum++;
            row = sheet.createRow(rownum);
            col=0;
            Object[] entry=resultList.get(i);
            String regionname=(String)entry[0];
            int seqcount=(Integer)entry[1];
            int totalcount=(Integer)entry[2];           
            outputStringValueInCell(row, col, regionname, null);
            col+=1;
            outputNumericValuesInCells(row, new double[]{totalcount,seqcount}, col, null);
            col+=2;
 
            //outputobject.append("\n",RAWDATA);
            if (i%10==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                task.setStatusMessage("Executing operation: output ("+i+"/"+resultList.size()+")");
                format.setProgress(i, resultList.size()); // 
                if (i%100==0) Thread.yield();
            }
        }
        format.setProgress(95);
        sheet.autoSizeColumn((short)0);
        sheet.autoSizeColumn((short)1);
        sheet.autoSizeColumn((short)2);       
        
        // Add the header on top of the page
        if (includeLegend) {        
            sheet.createFreezePane(0,headerrows,0,headerrows);
            row=sheet.getRow(0);
            outputStringValueInCell(row, 0, "Region Occurrence Analysis", title);
            StringBuilder firstLine=new StringBuilder();
            firstLine.append("Analysis performed on regions from \"");
            firstLine.append(regionTrackName);
            firstLine.append("\"");

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
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder=SORT_BY_TYPE;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             sortorder=(String)settings.getResolvedParameter("Sort by",defaults,engine);
          }
          catch (ExecutionError e) {throw e;}
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        }
        ArrayList<Object[]> resultList=assembleList(sortorder,engine);
        outputobject.append("# Region occurrence analysis on regions from '"+regionTrackName+"'",RAWDATA);     
        outputobject.append(" on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),RAWDATA);
        if (sequenceCollectionName!=null) outputobject.append(" from collection '"+sequenceCollectionName+"'",RAWDATA);

        outputobject.append("\n\n#Region type, total occurrences, number of sequences containing region, total number of sequences, percentage of sequences containing region type",RAWDATA);
        outputobject.append("\n",RAWDATA);      
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String regionname=(String)entry[0];
            int seqcount=(Integer)entry[1];
            int totalcount=(Integer)entry[2];
            outputobject.append(regionname+"\t"+totalcount+"\t"+seqcount+"\t"+sequenceCollectionSize+"\t"+(int)((double)seqcount*100/(double)sequenceCollectionSize)+"%",RAWDATA);
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
        RegionDataset source=(RegionDataset)task.getParameter("Region track");      
        regionTrackName=source.getName();      
        counts=new HashMap<String,double[]>();
        SequenceCollection collection=(SequenceCollection)task.getParameter("Sequences");
        if (collection==null) collection=task.getEngine().getDefaultSequenceCollection();
        else sequenceCollectionName=collection.getName();
        if (sequenceCollectionName!=null && sequenceCollectionName.equals(task.getEngine().getDefaultSequenceCollectionName())) sequenceCollectionName=null;
        sequenceCollectionSize=collection.size();
        int s=0;
        for (String sequenceName:collection.getAllSequenceNames()) {
            s++;
            RegionSequenceData seq=(RegionSequenceData)source.getSequenceByName(sequenceName);
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            task.setStatusMessage("Executing analysis: "+getAnalysisName()+" ("+s+"/"+sequenceCollectionSize+")");
            task.setProgress(s, sequenceCollectionSize); //
            Thread.yield();
 
            HashSet<String> present=new HashSet<String>();
            for (Region r:seq.getOriginalRegions()) {
              String type=r.getType();
              if (!counts.containsKey(type)) {
                counts.put(type, new double[]{0.0,0.0});
              } 
              if (!present.contains(type)) present.add(type);
              double[] regioncounts=counts.get(type);
              regioncounts[1]+=1; // total occurrences
              
            }
            // increase sequence support for regions present in this sequences
            for (String region:present) {
                double[] regioncounts=counts.get(region);
                regioncounts[0]+=1; // sequence occurrences
            }
        } // end for each sequence

    }

 
    private class SortOrderComparator implements Comparator<Object[]> {
            String sortorder=null;
            public SortOrderComparator(String order) {
                sortorder=order;
            }
            @Override
            public int compare(Object[] region1, Object[] region2) { //
                 if (sortorder.equals(SORT_BY_SEQUENCE_OCCURRENCES)) {
                     Integer value1=(Integer)region1[1];
                     Integer value2=(Integer)region2[1];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;                                     
                     int res=value2.compareTo(value1); // sorts descending!
                     if (res!=0) return res;
                     else return ((Integer)region2[2]).compareTo(((Integer)region1[2])); // if equal, sorts by total count descending!
                } else if (sortorder.equals(SORT_BY_TOTAL_OCCURRENCES)) {
                     Integer value1=(Integer)region1[2];
                     Integer value2=(Integer)region2[2];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;                         
                     int res=value2.compareTo(value1); // sorts descending!
                     if (res!=0) return res;
                     else return ((Integer)region2[1]).compareTo(((Integer)region1[1])); // if equal, sorts by total count descending!
                } else { // sort by region type
                    String regionname1=(String)region1[0];
                    String regionname2=(String)region2[0];
                    return MotifLabEngine.compareNaturalOrder(regionname1, regionname2);
                    // return regionname1.compareTo(regionname2); // sorts ascending!
                }
            }
    }

    
    
    private static final int COLOR=0;    
    private static final int TYPE=1;
    private static final int TOTAL=2;    
    private static final int SEQUENCE_SUPPORT=3;    

    @Override
    protected JPanel getDisplayPanel(MotifLabGUI gui, boolean modal) {
        JPanel displayPanel=new JPanel(new BorderLayout());
        JPanel headerPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        StringBuilder headerString=new StringBuilder("<html>Region occurrence analysis on regions from <b>");
        headerString.append(regionTrackName);
        headerString.append("</b>");            
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
        RegionOccurrenceTableModel tablemodel=new RegionOccurrenceTableModel(gui);
        GenericRegionBrowserPanel panel=new GenericRegionBrowserPanel(gui, tablemodel, TYPE, modal);
        CellRenderer_Support supportrenderer=new CellRenderer_Support();
        JTable table=panel.getTable();
        table.getColumn("Sequences").setCellRenderer(supportrenderer);
        table.getColumn("Sequences").setPreferredWidth(80);
        table.getColumn("Total").setPreferredWidth(60);
        panel.setPreferredSize(new java.awt.Dimension(500,500));
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

private class RegionOccurrenceTableModel extends AbstractTableModel {
    private String[] columnNames=new String[]{" ","Type","Total","Sequences"};
    private String[] regionTypes=null;
    private VisualizationSettings settings;

    public RegionOccurrenceTableModel(MotifLabGUI gui) {
        this.settings=gui.getVisualizationSettings();
        regionTypes=new String[counts.size()];
        int i=0;
        for (String type:counts.keySet()) {
           regionTypes[i]=type;
           i++;
        }   
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case COLOR:return Color.class;            
            case TYPE:return String.class;                
            case TOTAL:return Integer.class;
            case SEQUENCE_SUPPORT:return Integer.class;
            default:return Object.class;
        }
    }


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLOR:return settings.getFeatureColor(regionTypes[rowIndex]);            
            case TYPE:return regionTypes[rowIndex];              
            case SEQUENCE_SUPPORT:return getIntValueAt(rowIndex,0);
            case TOTAL:return getIntValueAt(rowIndex,1);
            default:return Object.class;
        }
    }
    
    private int getIntValueAt(int row,int col) { // this method is an ad-hoc solution to a casting problem that sometimes occur (perhaps from old sessions)
        Object countsrow=counts.get(regionTypes[row]);
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
        return regionTypes.length;
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
