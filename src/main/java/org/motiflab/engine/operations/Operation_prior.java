package org.motiflab.engine.operations;

import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.task.OperationTask;


/**
 *
 * @author kjetikl
 */
public class Operation_prior extends FeatureTransformOperation {
    private static final String name="prior";
    private static final String description="Converts a Numeric Dataset into a positional prior";
    private Class[] datasourcePreferences=new Class[]{NumericDataset.class};
 
    @Override
    public String getOperationGroup() {
        return "Transform";
    }

    @Override
    public Class[] getDataSourcePreferences() {
        return datasourcePreferences;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {return name;}
    
    @Override
    public boolean isSubrangeApplicable() {return false;}
    
    @Override
    public void resolveParameters(OperationTask task) throws Exception {

    }

    
    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        String seqname=sourceSequence.getName();
        //double[] logs=logTransform(((NumericSequenceData)sourceSequence).getData());
        double[] inverseLogs=logTransformInverse(((NumericSequenceData)sourceSequence).getData());
        double[] prob=new double[inverseLogs.length];
        double prob0=arraySum(inverseLogs);
        prob0=Math.exp(prob0);
        for (int j=0;j<prob.length;j++) {
//            double PZj=arraySumWithInsertion(inverseLogs, j, logs[j],seqname);
//            //double PZj=arrayProductWithInsertion(inverseLogs, j, logs[j],seqname);
////            if (seqname.equals("ENSG00000100345"))  System.err.println("1#PZj="+PZj);
//            //PZj=Math.exp(PZj);
//            PZj=Math.pow(10.0, PZj);
            double seqvalue=((NumericSequenceData)sourceSequence).getValueAtRelativePosition(j);
           // prob[j]=prob0*seqvalue/(1-seqvalue);
            prob[j]=prob0;
//            if (seqname.equals("ENSG00000100345"))  System.err.println("2#PZj="+PZj);
        }
        double totalsum=arraySum(prob)+prob0;
//        if (seqname.equals("ENSG00000100345")) {
//            System.err.println("Totalsum="+totalsum+", prob0="+prob0);
//            System.err.print("ENSG00000100345-{prob}");
//            debug(prob);            
//        }
        for (int j=0;j<prob.length;j++) {
            prob[j]=prob[j]/totalsum; // normalize 
        }
//        if (seqname.equals("ENSG00000100345")) {
//            System.err.print("ENSG00000100345-{probNormalized}");
//            debug(prob);
//            System.err.print("ENSG00000100345-{log}");
//            debug(logs);
//            System.err.print("ENSG00000100345-{inv}");
//            debug(inverseLogs);            
//        }
        for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) { // for each base
            if (positionSatisfiesCondition(seqname,i,task)) {
              int relativePos=sourceSequence.getRelativePositionFromGenomic(i);
              double newvalue=prob[relativePos];
              ((NumericSequenceData)targetSequence).setValueAtGenomicPosition(i, newvalue);
           } // satisfies 'where'-condition
        }
    }
    
    private void debug(double[] array) {
        System.err.print("[");
        for (int i=0;i<array.length;i++) {
            if (i>0) System.err.print(",");
            System.err.print(array[i]);            
        }
        System.err.println("]");
    }
    
    private double[] logTransform(double[] seq) {
        double[] result=new double[seq.length];
        for (int i=0;i<seq.length;i++) result[i]=Math.log(seq[i]);
        return result;
    }
    private double[] logTransformInverse(double[] seq) {
        double[] result=new double[seq.length];
        for (int i=0;i<seq.length;i++) result[i]=Math.log(1-seq[i]);
        return result;
    }    
        
    private double arraySum(double[] array) {
        double sum=0;
        for (int i=0;i<array.length;i++) sum+=array[i];
        return sum;
    }
    private double arraySumWithInsertion(double[] array, int pos, double specialValue,String seqname) {
        double sum=0;
        for (int i=0;i<array.length;i++) {
            sum+=(i==pos)?specialValue:array[i];
        }        
        return sum;
    }
 
}
