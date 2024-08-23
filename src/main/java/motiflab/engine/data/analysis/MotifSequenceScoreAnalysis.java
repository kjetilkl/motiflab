/*
 
 
 */

package motiflab.engine.data.analysis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import javax.swing.JTable;
import motiflab.engine.data.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import motiflab.engine.Graph;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.dataformat.DataFormat;
import motiflab.gui.GenericMotifBrowserPanel;
import motiflab.gui.MotifLogo;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class MotifSequenceScoreAnalysis extends Analysis {
    private final static String typedescription="Analysis: motif-sequence score analysis";
    private final static String analysisName="motif-sequence score";
    private final static String description="Sums up the scores for each motif in each sequence and reports min, max, average and range of motif sum-scores across all sequences for each motif";
    private static final String SORT_BY_MOTIF="Motif ID";
    private static final String SORT_BY_MIN="Min";
    private static final String SORT_BY_MAX="Max";
    private static final String SORT_BY_AVERAGE="Average";
    private static final String SORT_BY_RANGE="Range";  
    private HashMap<String,double[]> counts=null; // key is motif name. Value is double[]{min, max, average, range}
    private String motifCollectionName=null;
    private String motifTrackName=null;
    private String sequenceCollectionName=null;
    private int sequenceCollectionSize=0;
     
    private static final int COLOR=0;    
    private static final int ID=1;
    private static final int NAME=2;
    private static final int MOTIF_CLASS=3;    
    private static final int MIN=4;
    private static final int MAX=5;
    private static final int AVERAGE=6;
    private static final int RANGE=7;
    private static final int LOGO=8;
    
    public MotifSequenceScoreAnalysis() {
        this.name="TestAnalysis_temp";
        addParameter("Motif track",RegionDataset.class, null,new Class[]{RegionDataset.class},"A region track containing motif sites",true,false);
        addParameter("Motifs",MotifCollection.class, null,new Class[]{MotifCollection.class},"The motifs to consider in this analysis",true,false);
        addParameter("Sequences",SequenceCollection.class, null,new Class[]{SequenceCollection.class},"If specified, the analysis will be limited to sequences in this collection",false,false);
    }

    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Motif track","Motifs"};}

    @Override
    public boolean canUseAsSourceProxy(Data data) {
        if (data instanceof RegionDataset) return ((RegionDataset)data).isMotifTrack(); // only allow Motif Tracks as input
        else return (data instanceof MotifCollection);
    }     
    
    @Override
    public Parameter[] getOutputParameters(String dataformat) {  
        if (dataformat.equals(HTML) || dataformat.equals(EXCEL) || dataformat.equals(RAWDATA)) {
            return new Parameter[] {
                new Parameter("Sort by",String.class,SORT_BY_AVERAGE, new String[]{SORT_BY_MOTIF,SORT_BY_MIN,SORT_BY_MAX, SORT_BY_AVERAGE,SORT_BY_RANGE},"Sorting order for the results table",false,false),
                new Parameter("Logos",String.class,getMotifLogoDefaultOption(dataformat), getMotifLogoOptions(dataformat),"Include motif sequence logos in the table",false,false)
            };
        } else return new Parameter[0];
    }
    
//    @Override
//    public String[] getOutputParameterFilter(String parameter) {
//        if (parameter.equals("Sort by") || parameter.equals("Logos")) return new String[]{"HTML","RawData"};        
//        return null;
//    }     

    @Override
    public String[] getResultVariables() {
        return new String[]{"min","max","average","range"};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        int dataindex=0;
        if (variablename==null || variablename.isEmpty()) throw new ExecutionError("'"+getName()+"' does not have a result for ''");
        else if (variablename.equals("min")) dataindex=0;        
        else if (variablename.equals("max")) dataindex=1;        
        else if (variablename.equals("average")) dataindex=2;  
        else if (variablename.equals("range")) dataindex=3;         
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
        MotifNumericMap map=new MotifNumericMap("temp",0);
        for (String motifname:counts.keySet()) {
            double[] stats=counts.get(motifname);
            map.setValue(motifname, stats[dataindex]);
        }
        return map;        
    }

    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else return MotifNumericMap.class;
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
        return new String[]{"min","max","average","range"};
    }

    @Override
    public Class getColumnType(String column) {
             if (   column.equalsIgnoreCase("min") || column.equalsIgnoreCase("max")
                 || column.equalsIgnoreCase("average") || column.equalsIgnoreCase("range")) return Double.class;
        else return null;
    }

    @Override
    public HashMap<String,Object> getColumnData(String column) throws ExecutionError {
        Class coltype=getColumnType(column);
        if (coltype==null) throw new ExecutionError("Analysis '"+name+"' does not have a column named '"+column+"'");
        HashMap<String,Object> columnData=new HashMap<String, Object>();
        for (String motifname:counts.keySet()) {
            double[] stat=counts.get(motifname);
                 if (column.equalsIgnoreCase("min")) columnData.put(motifname, new Double(stat[0]));
            else if (column.equalsIgnoreCase("max")) columnData.put(motifname, new Double(stat[1]));
            else if (column.equalsIgnoreCase("average")) columnData.put(motifname, new Double(stat[2]));
            else if (column.equalsIgnoreCase("range")) columnData.put(motifname, new Double(stat[3]));
         }
        return columnData;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MotifSequenceScoreAnalysis clone() {
        MotifSequenceScoreAnalysis newanalysis=new MotifSequenceScoreAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.counts=(HashMap<String,double[]>)this.counts.clone();
         newanalysis.sequenceCollectionSize=this.sequenceCollectionSize;
        newanalysis.motifCollectionName=this.motifCollectionName;
        newanalysis.motifTrackName=this.motifTrackName;
        newanalysis.sequenceCollectionName=this.sequenceCollectionName;
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        this.counts=(HashMap<String,double[]>)((MotifSequenceScoreAnalysis)source).counts;
        this.sequenceCollectionSize=((MotifSequenceScoreAnalysis)source).sequenceCollectionSize;
        this.motifCollectionName=((MotifSequenceScoreAnalysis)source).motifCollectionName;
        this.motifTrackName=((MotifSequenceScoreAnalysis)source).motifTrackName;
        this.sequenceCollectionName=((MotifSequenceScoreAnalysis)source).sequenceCollectionName;
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
    private ArrayList<Object[]> assembleList(String sortorder) {
        ArrayList<Object[]> resultList=new ArrayList<Object[]>(counts.size());
        Set<String> keys=counts.keySet();
        Iterator<String> iterator=keys.iterator();
        int i=0;     
        while (iterator.hasNext()) {
            i++;
            String motifkey=iterator.next();
            double[] values=counts.get(motifkey);
            double min=values[0];            
            double max=values[1];
            double average=values[2];            
            double range=values[3];     
            resultList.add(new Object[]{motifkey,new Double(min), new Double(max), new Double(average), new Double(range)});
        }
        Collections.sort(resultList, new SortOrderComparator(sortorder));
        return resultList;
    }

    @Override
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder=SORT_BY_MOTIF;
        Color [] basecolors=engine.getClient().getVisualizationSettings().getBaseColors();
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
        engine.createHTMLheader("Motif-Sequence Score Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<h1 class=\"headline\">Motif-Sequence Score Analysis</h1>\n",HTML);
        outputobject.append("<div class=\"summary\">\n",HTML);
        outputobject.append("Analysis performed with motifs from <span class=\"dataitem\">",HTML);
        outputobject.append(motifCollectionName,HTML);
        outputobject.append("</span> and sites from <span class=\"dataitem\">",HTML);
        outputobject.append(motifTrackName,HTML);
        outputobject.append("</span> on "+sequenceCollectionSize,HTML);
        outputobject.append(" sequence"+((sequenceCollectionSize!=1)?"s":""),HTML);
        if (sequenceCollectionName!=null) {
            outputobject.append(" from collection <span class=\"dataitem\">",HTML);
            outputobject.append(sequenceCollectionName,HTML);
            outputobject.append("</span>",HTML);
        }
        outputobject.append(".\n</div>\n<br>\n",HTML);
        outputobject.append("<table class=\"sortable\">\n",HTML);
        String logoheader=(showSequenceLogos)?"<th class=\"sorttable_nosort\">Logo</th>":"";
        outputobject.append("<tr><th>ID</th><th>Name</th><th class=\"sorttable_ip\">Class</th><th>Min</th><th>Max</th><th>Average</th><th>Range</th>"+logoheader+"</tr>\n",HTML);
        DecimalFormat decimalformatter=DataFormat.getDecimalFormatter(3);
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[0];
            Double min=(Double)entry[1];
            Double max=(Double)entry[2];
            Double average=(Double)entry[3];
            Double range=(Double)entry[4];            
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
            outputobject.append("<td>"+escapeHTML(motifname)+"</td>",HTML);
            outputobject.append("<td>"+escapeHTML(motifpresentationname)+"</td>",HTML);
            outputobject.append("<td"+((motifclassname!=null)?(" title=\""+motifclassname+"\""):"")+">"+escapeHTML(motifclass)+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+Graph.formatNumber(min,false)+"</td><td class=\"num\">"+Graph.formatNumber(max,false)+"</td><td class=\"num\">"+Graph.formatNumber(average,false)+"</td><td class=\"num\">"+Graph.formatNumber(range,false)+"</td>",HTML);
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
            task.setStatusMessage("Executing operation: output ("+i+"/"+resultList.size()+")");
            format.setProgress(i, resultList.size()); //
        }
        outputobject.append("</table>\n</body>\n</html>\n",HTML);
        format.setProgress(100);
        return outputobject;
    }

    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortorder=SORT_BY_MOTIF;
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
        outputobject.append("# Motif-Sequence score analysis with motifs from '"+motifCollectionName+"' and sites from '"+motifTrackName+"'",RAWDATA);
        outputobject.append(" on "+sequenceCollectionSize+" sequence"+((sequenceCollectionSize!=1)?"s":""),RAWDATA);
        if (sequenceCollectionName!=null) outputobject.append(" from collection '"+sequenceCollectionName+"'",RAWDATA);


        outputobject.append("\n\n#Motif ID, min, max, average, range",RAWDATA);
        if (showSequenceLogos) outputobject.append(", motif consensus",RAWDATA);
        outputobject.append("\n",RAWDATA);
        DecimalFormat decimalformatter=DataFormat.getDecimalFormatter(3);    
        for (int i=0;i<resultList.size();i++) {
            Object[] entry=resultList.get(i);
            String motifname=(String)entry[0];
            Double min=(Double)entry[1];
            Double max=(Double)entry[2];
            Double average=(Double)entry[3];
            Double range=(Double)entry[4];
            Motif motif=null;
            if (engine.dataExists(motifname, Motif.class)) motif=(Motif)engine.getDataItem(motifname);            
            outputobject.append(motifname+"\t"+min+"\t"+max+"\t"+average+"\t"+range,RAWDATA);
            outputobject.append(motifname,RAWDATA);
            outputobject.append("\t"+((min==null || Double.isNaN(min))?"":min),RAWDATA);
            outputobject.append("\t"+((max==null || Double.isNaN(max))?"":max),RAWDATA);              
            outputobject.append("\t"+((average==null || Double.isNaN(average))?"":average),RAWDATA);  
            outputobject.append("\t"+((range==null || Double.isNaN(range))?"":range),RAWDATA);            
            if (showSequenceLogos) {
                if (motif!=null) outputobject.append("\t"+motif.getConsensusMotif(),RAWDATA);
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
        RegionDataset source=(RegionDataset)task.getParameter("Motif track");
        if (!source.isMotifTrack()) throw new ExecutionError("Motif-Sequence Score Analysis can only be performed on motif tracks");
        motifTrackName=source.getName();
        MotifCollection motifcollection=(MotifCollection)task.getParameter("Motifs");
        motifCollectionName=motifcollection.getName();
        counts=new HashMap<String,double[]>(motifcollection.size());
        for (String motifname:motifcollection.getAllMotifNames()) {
            counts.put(motifname, new double[]{0,0,0,0});
        }
        SequenceCollection collection=(SequenceCollection)task.getParameter("Sequences");
        if (collection==null) collection=task.getEngine().getDefaultSequenceCollection();
        else sequenceCollectionName=collection.getName();
        if (sequenceCollectionName!=null && sequenceCollectionName.equals(task.getEngine().getDefaultSequenceCollectionName())) sequenceCollectionName=null;
        sequenceCollectionSize=collection.size();
        MotifLabEngine engine=task.getEngine();
        int s=0;
        for (String sequenceName:collection.getAllSequenceNames()) {
            HashMap<String,Double> thisSeq=new HashMap<String,Double>();
            s++;
            RegionSequenceData seq=(RegionSequenceData)source.getSequenceByName(sequenceName);
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            task.setStatusMessage("Executing analysis: "+getAnalysisName()+" ("+s+"/"+sequenceCollectionSize+")");
            task.setProgress(s, sequenceCollectionSize); //
            Thread.yield();
            for (Region r:seq.getOriginalRegions()) {
              String type=r.getType();
              Double prev=thisSeq.get(type);
              if (prev==null) thisSeq.put(type, r.getScore());
              else thisSeq.put(type, r.getScore()+prev);
            }
            for (String type:thisSeq.keySet()) {
                Double value=thisSeq.get(type);
                double[] motifcounts=counts.get(type);
                if (motifcounts==null) continue; // the region type is not of the selected motifs
                if (value<motifcounts[0] || motifcounts[0]==0) motifcounts[0]=value;
                if (value>motifcounts[1]) motifcounts[1]=value;
                motifcounts[2]+=value; // sum
            }            
        } // end for each sequence
        for (String type:counts.keySet()) {
            double[] motifcounts=counts.get(type);    
            motifcounts[2]=motifcounts[2]/collection.size();  
            motifcounts[3]=motifcounts[1]-motifcounts[0];            
        }        

    }



    private class SortOrderComparator implements Comparator<Object[]> {
            String sortorder=null;
            public SortOrderComparator(String order) {
                sortorder=order;
            }
            @Override
            public int compare(Object[] motif1, Object[] motif2) { //
                 if (sortorder.equals(SORT_BY_MIN)) {
                     Double value1=(Double)motif1[1];
                     Double value2=(Double)motif2[1];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;     
                     if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;            
                     if (Double.isNaN(value1)) return 1;
                     if (Double.isNaN(value2)) return -1;                     
                     int res=value2.compareTo(value1); // sorts descending!
                     if (res!=0) return res;
                     else return ((String)motif2[0]).compareTo(((String)motif1[0])); 
                } else if (sortorder.equals(SORT_BY_MAX)) {
                     Double value1=(Double)motif1[2];
                     Double value2=(Double)motif2[2];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;     
                     if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;            
                     if (Double.isNaN(value1)) return 1;
                     if (Double.isNaN(value2)) return -1;                     
                     int res=value1.compareTo(value2); // sorts ascending!
                     if (res!=0) return res;
                     else return ((String)motif2[0]).compareTo(((String)motif1[0])); 
                } else if (sortorder.equals(SORT_BY_AVERAGE)) {
                     Double value1=(Double)motif1[3];
                     Double value2=(Double)motif2[3];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;     
                     if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;            
                     if (Double.isNaN(value1)) return 1;
                     if (Double.isNaN(value2)) return -1;                     
                     int res=value1.compareTo(value2); // sorts ascending!
                     if (res!=0) return res;
                     else return ((String)motif2[0]).compareTo(((String)motif1[0])); 
                } else if (sortorder.equals(SORT_BY_RANGE)) {
                     Double value1=(Double)motif1[4];
                     Double value2=(Double)motif2[4];
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;     
                     if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;            
                     if (Double.isNaN(value1)) return 1;
                     if (Double.isNaN(value2)) return -1;                    
                     int res=value1.compareTo(value2); // sorts ascending!
                     if (res!=0) return res;
                     else return ((String)motif2[0]).compareTo(((String)motif1[0])); 
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
        StringBuilder headerString=new StringBuilder("<html>Motif-Sequence Score Analysis with motifs from <b>");
        headerString.append(motifCollectionName);
        headerString.append("</b> and sites from <b>");
        headerString.append(motifTrackName);
        headerString.append("</b><br>on ");
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
        MotifOccurrenceTableModel tablemodel=new MotifOccurrenceTableModel(gui);
        GenericMotifBrowserPanel panel=new GenericMotifBrowserPanel(gui, tablemodel, modal);
        CellRenderer_Classification classrenderer=new CellRenderer_Classification();
        JTable table=panel.getTable();

        table.getColumn("Class").setCellRenderer(classrenderer);
        table.getColumn("ID").setPreferredWidth(60);
        panel.setPreferredSize(new java.awt.Dimension(700,500));     
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
        columnNames=new String[]{" ","ID","Name","Class","Min","Max","Average","Range","Logo"};     
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case COLOR:return Color.class;            
            case ID:return String.class;
            case NAME:return String.class;
            case MOTIF_CLASS:return String.class;                
            case MIN:return Double.class;
            case MAX:return Double.class;
            case AVERAGE:return Double.class;
            case RANGE:return Double.class;
            case LOGO:return Motif.class;
            default:return Object.class;
        }
    }

    public final Motif getMotif(String id) {
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
            case MOTIF_CLASS:return getMotifClass(motifnames[rowIndex]);                
            case MIN:return counts.get(motifnames[rowIndex])[0];
            case MAX:return counts.get(motifnames[rowIndex])[1];
            case AVERAGE:return counts.get(motifnames[rowIndex])[2];
            case RANGE:return counts.get(motifnames[rowIndex])[3];
            case LOGO:return getMotif(motifnames[rowIndex]);
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
