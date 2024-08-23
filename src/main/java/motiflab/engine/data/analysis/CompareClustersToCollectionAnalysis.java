

package motiflab.engine.data.analysis;

import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import motiflab.engine.data.*;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.Graph;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.dataformat.DataFormat;
import org.apache.commons.math3.distribution.HypergeometricDistribution;

/**
 *
 * @author kjetikl
 */
public class CompareClustersToCollectionAnalysis extends Analysis {
    private final static String typedescription="Analysis: compare clusters to collection";
    private final static String analysisName="compare clusters to collection";
    private final static String description="Compares every cluster in a partition to a target collection using Fisher's exact test";
    private String partitionName=null;
    private String collectionName=null;
    private String backgroundCollectionName=null;
    private HashMap<String,int[]> contingencyTables=null; 
    private HashMap<String,Double> pvalues=null;
    private int collectionSize=0;
    private int backgroundCollectionSize=0;

    private static final int CLUSTER_SIZE=0;
    private static final int INTERSECTION=1;
    private static final int UNION=2;
    private static final int NEITHER=3;
    
    private static final String SORT_BY_CLUSTER="Cluster";
    private static final String SORT_BY_PVALUE="p-value";    

    public CompareClustersToCollectionAnalysis() {
        this.name="CompareClustersToCollectionAnalysis_temp";
        addParameter("Partition",DataPartition.class, null,new Class[]{DataPartition.class},null,true,false);
        addParameter("Collection",DataCollection.class, null,new Class[]{DataCollection.class},null,true,false);
        addParameter("Total",DataCollection.class, null,new Class[]{DataCollection.class},"The total collection is used to find the number of entries that are not present in either collection",false,false);
    }

    @Override
    public String[] getSourceProxyParameters() {return new String[]{"Partition","Collection"};}   
    
    @Override
    public Parameter[] getOutputParameters(String dataformat) {
        if (dataformat.equals(HTML) || dataformat.equals(EXCEL) || dataformat.equals(RAWDATA)) {
            return new Parameter[] {
                 new Parameter("Sort by",String.class,SORT_BY_PVALUE, new String[]{SORT_BY_CLUSTER,SORT_BY_PVALUE},null,false,false),
            };
        } else return new Parameter[0];
    }
    
//    @Override
//    public String[] getOutputParameterFilter(String parameter) {
//        if (parameter.equals("Sort by")) return new String[]{"HTML","RawData"};        
//        return null;
//    }       

    @Override
    public String[] getResultVariables() {
        return new String[]{};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename==null || variablename.isEmpty()) throw new ExecutionError("'"+getName()+"' does not have a result for ''");
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else return NumericVariable.class;
    }

    @Override
    public String getAnalysisName() {
        return analysisName;
    }

    @Override
    public String getDescription() {return description;}



    @Override
    @SuppressWarnings("unchecked")
    public CompareClustersToCollectionAnalysis clone() {
        CompareClustersToCollectionAnalysis newanalysis=new CompareClustersToCollectionAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.collectionName=this.collectionName;
        newanalysis.partitionName=this.partitionName;
        newanalysis.backgroundCollectionName=this.backgroundCollectionName;
        newanalysis.contingencyTables=new HashMap<String, int[]>(this.contingencyTables.size());
        for (String key:this.contingencyTables.keySet()) {
            newanalysis.contingencyTables.put(key,this.contingencyTables.get(key).clone());
        }        
        newanalysis.pvalues=(HashMap<String,Double>)this.pvalues.clone();
        newanalysis.collectionSize=this.collectionSize;
        newanalysis.backgroundCollectionSize=this.backgroundCollectionSize;
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        CompareClustersToCollectionAnalysis other=((CompareClustersToCollectionAnalysis)source);
        this.partitionName=other.partitionName;
        this.collectionName=other.collectionName;
        this.backgroundCollectionName=other.backgroundCollectionName;
        this.contingencyTables=other.contingencyTables;
        this.pvalues=other.pvalues;
        this.collectionSize=other.collectionSize;
        this.backgroundCollectionSize=other.backgroundCollectionSize;        
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
        String sortBy=SORT_BY_PVALUE;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             sortBy=(String)settings.getResolvedParameter("Sort by",defaults,engine);
          } 
          catch (ExecutionError e) {throw e;} 
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        } 

        DecimalFormat decimalformatter=new DecimalFormat("0.0");
        engine.createHTMLheader("Compare Clusters to Collection Analysis", null, null, true, true, true, outputobject);
        outputobject.append("<div align=\"center\">",HTML);
        outputobject.append("<h2 class=\"headline\">Compare Clusters to Collection Analysis</h2>\n",HTML);
        //outputobject.append("<br />",HTML);
        outputobject.append("Comparison between clusters in <span class=\"dataitem\">"+partitionName+"</span> and collection <span class=\"dataitem\">"+collectionName+"</span> (size="+collectionSize+")<br>\n",HTML);
        outputobject.append("with respect to a total of "+backgroundCollectionSize+" entries\n",HTML);
        if (backgroundCollectionName!=null) {
            outputobject.append(" from collection <span class=\"dataitem\">",HTML);
            outputobject.append(backgroundCollectionName,HTML);
            outputobject.append("</span>\n",HTML);
        }
        outputobject.append("<br><br>\n<table class=\"sortable\">\n",HTML);
        outputobject.append("<tr><th>Cluster</th><th>Size</th><th>Overlap</th><th><nobr>Cluster Overlap</nobr></th><th><nobr>Collection Overlap</nobr></th><th>p-value</th></tr>\n",HTML);
        ArrayList<String> names=new ArrayList<String>(pvalues.size());
        for (String clusterName:pvalues.keySet()) names.add(clusterName);
        Collections.sort(names,new SortOrderComparator(sortBy));
        for (String clusterName:names) {
            int[] contingencyTable=contingencyTables.get(clusterName);
            int totalA=contingencyTable[CLUSTER_SIZE];
            int totalB=collectionSize;
            int A_B=contingencyTable[INTERSECTION]; 
            double pvalue=pvalues.get(clusterName);
            outputobject.append("<tr>",HTML);
            outputobject.append("<td style=\"text-align:left\">"+clusterName+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+totalA+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+A_B+"</td>",HTML);
            outputobject.append("<td class=\"num\">"+decimalformatter.format(((double)A_B/(double)totalA)*100.0)+"%</td>",HTML);
            outputobject.append("<td class=\"num\">"+decimalformatter.format(((double)A_B/(double)totalB)*100.0)+"%</td>",HTML);
            outputobject.append("<td class=\"num\">"+Graph.formatNumber(pvalue,false)+"</td>",HTML);
            outputobject.append("</tr>\n",HTML);
        }
        outputobject.append("</table>\n<br>\n",HTML);
        outputobject.append("</div>\n",HTML);
        outputobject.append("</body>\n</html>\n",HTML);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }


    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        String sortBy=SORT_BY_PVALUE;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters(format);
             sortBy=(String)settings.getResolvedParameter("Sort by",defaults,engine);
          } 
          catch (ExecutionError e) {throw e;} 
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        } 
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();

        DecimalFormat decimalformatter=new DecimalFormat("0.0");
        task.setStatusMessage("Executing operation: output");
        outputobject.append("#Comparing clusters to collection\n",RAWDATA);
        outputobject.append("#\n",RAWDATA);
        outputobject.append("#Comparison between clusters in '"+partitionName+"' and collection '"+collectionName+"' (size="+collectionSize+")",RAWDATA);
        outputobject.append(" with respect to a total of "+backgroundCollectionSize+" entries",RAWDATA);
        if (backgroundCollectionName!=null) outputobject.append(" from collection '"+backgroundCollectionName+"'",RAWDATA);
        outputobject.append("\n#\n#Cluster\tCluster size\tOverlap\tCluster Overlap\tCollection Overlap\tp-value\n",RAWDATA);
        ArrayList<String> names=new ArrayList<String>(pvalues.size());
        for (String clusterName:pvalues.keySet()) names.add(clusterName);
        Collections.sort(names,new SortOrderComparator(sortBy));
        for (String clusterName:names) {
            int[] contingencyTable=contingencyTables.get(clusterName);
            int totalA=contingencyTable[CLUSTER_SIZE];
            int totalB=collectionSize;
            int A_B=contingencyTable[INTERSECTION]; 
            double pvalue=pvalues.get(clusterName);
            outputobject.append(clusterName+"\t",RAWDATA);
            outputobject.append(totalA+"\t",RAWDATA);
            outputobject.append(A_B+"\t",RAWDATA);
            outputobject.append(decimalformatter.format(((double)A_B/(double)totalA)*100.0)+"%\t",RAWDATA);
            outputobject.append(decimalformatter.format(((double)A_B/(double)totalB)*100.0)+"%\t",RAWDATA);
            outputobject.append(Graph.formatNumber(pvalue,false)+"\n",RAWDATA);
        }
        format.setProgress(100);
        return outputobject;
    }

    @Override
    protected Dimension getDefaultDisplayPanelDimensions() {
        return new Dimension(600,530);
    }

    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        DataPartition partition=(DataPartition)task.getParameter("Partition");
        DataCollection collection=(DataCollection)task.getParameter("Collection");
        DataCollection backgroundCollection=(DataCollection)task.getParameter("Total");
        Class targetClass=partition.getMembersClass();
        if (!collection.getMembersClass().equals(targetClass)) throw new ExecutionError("The target collection must hold the same type of data objects as the partition");

        if (backgroundCollection!=null) {
            if (!backgroundCollection.getMembersClass().equals(targetClass)) throw new ExecutionError("The Total collection must hold the same type of data objects as the partition");
            backgroundCollectionName=backgroundCollection.getName();
        } else {
                 if (collection instanceof SequenceCollection) backgroundCollection=task.getEngine().getDefaultSequenceCollection();
            else if (collection instanceof MotifCollection) {backgroundCollection=new MotifCollection("temp"); ((MotifCollection)backgroundCollection).addMotifNames(task.getEngine().getNamesForAllDataItemsOfType(Motif.class));}
            else if (collection instanceof ModuleCollection) {backgroundCollection=new MotifCollection("temp"); ((ModuleCollection)backgroundCollection).addModuleNames(task.getEngine().getNamesForAllDataItemsOfType(Module.class));}
        }
        collectionName=collection.getName();
        partitionName=partition.getName();
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setStatusMessage("Executing analysis: "+getAnalysisName());
        task.setProgress(20); //
        Thread.yield();
        backgroundCollectionSize=backgroundCollection.size();
        collectionSize=0;
        for (String entry:collection.getValues()) {
            if (!backgroundCollection.contains(entry)) continue;
            collectionSize++;
        }        
        contingencyTables=new HashMap<String,int[]>();  
        pvalues=new HashMap<String,Double>();   
        for (String clusterName:partition.getClusterNames()) {
            ArrayList<String> clusterMembers=partition.getAllMembersInCluster(clusterName);
            int clusterSize=0;
            int intersection=0;
            int union=0;
            for (String entry:clusterMembers) {
                if (!backgroundCollection.contains(entry)) continue; // do not count those that are not in the background
                clusterSize++;
                if (collection.contains(entry)) intersection++;
                union++;
            }
            for (String entry:collection.getValues()) {
                if (!backgroundCollection.contains(entry)) continue;
                if (!clusterMembers.contains(entry)) union++;
            }
            int[] contingencyTable=new int[4];
            contingencyTable[CLUSTER_SIZE]=clusterSize;
            contingencyTable[INTERSECTION]=intersection;
            contingencyTable[UNION]=union;
            contingencyTable[NEITHER]=backgroundCollection.size()-union;
            // calculate statistics
            HypergeometricDistribution hypergeometric = new HypergeometricDistribution(backgroundCollection.size(), clusterSize, collectionSize);
            double pvalue=hypergeometric.upperCumulativeProbability(intersection);
            if (pvalue>1.0) pvalue=1.0; // this could happen because of rounding errors
            contingencyTables.put(clusterName,contingencyTable);
            pvalues.put(clusterName,pvalue);
        }
     }

    private class SortOrderComparator implements Comparator<String> {
            String sortorder=null;
            public SortOrderComparator(String order) {
                sortorder=order;
            }
            @Override
            public int compare(String cluster1, String cluster2) { //
                 if (sortorder.equals(SORT_BY_PVALUE)) {
                     Double value1=pvalues.get(cluster1);
                     Double value2=pvalues.get(cluster2);
                     if (value1==null && value2==null) return 0;
                     if (value1==null) return 1;
                     if (value2==null) return -1;     
                     if (Double.isNaN(value1) && Double.isNaN(value2)) return 0;
                     if (Double.isNaN(value1)) return 1;
                     if (Double.isNaN(value2)) return -1;                 
                     return value1.compareTo(value2); 
                 } else { // sort by clustername
                    return cluster1.compareTo(cluster2);  // sorts ascending!
                }
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
