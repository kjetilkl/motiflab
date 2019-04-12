/*
 *
 */

package motiflab.engine.util;

import motiflab.engine.data.Motif;

/**
 * This compares two motifs using Average Log Likelihood Ratio
 *
 * @author kjetikl
 */
public class MotifComparator_ALLR extends MotifComparator {
    private int minimumOverlap=3; // minimum number of overlapping columns
    private double[] uniform=new double[]{0.25f,0.25f,0.25f,0.25f};
    private double p=0.25; // background probability for a base
    private double pseudo=0.01; // pseudo-count frequency to use instead of 0.0

    @Override
    public String getName() {return "Average Log-Likelihood Ratio";}

    @Override
    public String getAbbreviatedName() {return "ALLR";} 
    
    @Override
    public boolean isDistanceMetric() {return false;}   
    
    @Override
    public double[] compareMotifs(Motif motifA, Motif motifB) {
        double[][] matrixA=motifA.getMatrixAsFrequencyMatrix(); // forces conversion to frequencies between 0 and 1.0
        double[][] matrixB=motifB.getMatrixAsFrequencyMatrix(); // forces conversion to frequencies between 0 and 1.0

        double[] resultDirect=compareMotifsAndAlign(matrixA, matrixB);
        matrixB=Motif.reverseComplementMatrix(matrixB);
        double[] resultReverse=compareMotifsAndAlign(matrixA, matrixB);
        if (resultDirect[0]>=resultReverse[0]) return new double[]{resultDirect[0],1,resultDirect[1]};
        else return new double[]{resultReverse[0],-1,resultReverse[1]};
    }

    @Override
    public double[] compareMotifsBothDirections(Motif motifA, Motif motifB) {
        double[][] matrixA=motifA.getMatrixAsFrequencyMatrix(); // forces conversion to frequencies between 0 and 1.0
        double[][] matrixB=motifB.getMatrixAsFrequencyMatrix(); // forces conversion to frequencies between 0 and 1.0

        double[] resultDirect=compareMotifsAndAlign(matrixA, matrixB);
        matrixB=Motif.reverseComplementMatrix(matrixB);
        double[] resultReverse=compareMotifsAndAlign(matrixA, matrixB);
        return new double[]{resultDirect[0],resultDirect[1],resultReverse[0],resultReverse[1]};
    }

    /** Returns a double[] with 2 entries: [0]=similarity score, [1]=best alignment offset of matrixB with respect to matrixA */
    private double[] compareMotifsAndAlign(double[][] matrixA, double[][] matrixB) {
        boolean motifAlargest=true;
        double[][] largermatrix=null;
        double[][] smallermatrix=null;
        if (matrixA.length>matrixB.length) {
            largermatrix=matrixA;
            smallermatrix=matrixB;
        } else {
            motifAlargest=false;
            largermatrix=matrixB;
            smallermatrix=matrixA;
        }
    //    System.err.println("----------------  "+smallermotif.getName()+"  vs  "+largermotif.getName()+"  ---------------");
        int start=minimumOverlap-smallermatrix.length;
        if (start>0) return new double[]{-100000,0}; // overlap is smaller than minimumOverlap (return 'default' bad value, large negative but NOT -Inf)
        int end=largermatrix.length-minimumOverlap;
        double result=-Double.MAX_VALUE; //
        double offset=0; // alignment offset for best alignment. Position of motifB with respect to motifA
        for (int i=start;i<=end;i++) {
            double alignmentresult=compareAtAlignment(largermatrix,smallermatrix,i);
            if (alignmentresult>result) {result=alignmentresult;offset=(motifAlargest)?i:-i;}
        }
        return new double[]{result,offset};
    }

    /**
     * Compares two aligned matrices (a large one and a smaller). The alignment can "partial" (with non-overlapping edges)
     * but with at least a minimum required number of bases overlapping
     * @param largematrix
     * @param smallermatrix
     * @param startA The position in the large matrix where the start of the smaller matrix has been aligned (can be negative)
     * @return
     */
    private double compareAtAlignment(double[][] largematrix,double[][] smallermatrix, int startA) {
        double result=0;
        int endsmallalignment=startA+smallermatrix.length-1;
        int end=(largematrix.length>endsmallalignment)?largematrix.length:endsmallalignment;
        int start=(startA<0)?startA:0;
        for (int i=start;i<end;i++) { //
            int largermatrixpos=i;
            int smallermatrixpos=i-startA;
            // get correct column from each matrix based on alignment (use uniform distr. column, if position is outside respective matrices)
            double[] largermatrixColumn=(largermatrixpos>=0 && largermatrixpos<largematrix.length)?largematrix[largermatrixpos]:uniform;
            double[] smallermatrixColumn=(smallermatrixpos>=0 && smallermatrixpos<smallermatrix.length)?smallermatrix[smallermatrixpos]:uniform;
            double colres=allr(largermatrixColumn,smallermatrixColumn);
            result+=colres;
        }
        return result;
    }


    /** Returns the average log-likelihood ratio for the two given columns */
    private double allr(double[] colA, double[] colB) {
        double[] columnA=new double[]{colA[0],colA[1],colA[2],colA[3]};
        double[] columnB=new double[]{colB[0],colB[1],colB[2],colB[3]};
        for (int i=0;i<columnA.length;i++) if (columnA[i]==0) columnA[i]=pseudo;
        for (int i=0;i<columnB.length;i++) if (columnB[i]==0) columnB[i]=pseudo;
        double leftSide=columnB[0]*Math.log(columnA[0]/p)+columnB[1]*Math.log(columnA[1]/p)+columnB[2]*Math.log(columnA[2]/p)+columnB[3]*Math.log(columnA[3]/p);
        double rightSide=columnA[0]*Math.log(columnB[0]/p)+columnA[1]*Math.log(columnB[1]/p)+columnA[2]*Math.log(columnB[2]/p)+columnA[3]*Math.log(columnB[3]/p);
        return (leftSide+rightSide)/2;
    }


}