/*
 *
 */

package motiflab.engine.util;

import motiflab.engine.data.Motif;

/**
 * This compares two motifs using the sum of squared distances
 *
 * @author kjetikl
 */
public class MotifComparator_SSD extends MotifComparator {
    private int minimumOverlap=3; // minimum number of overlapping columns
    private double[] uniform=new double[]{0.25f,0.25f,0.25f,0.25f};

    @Override
    public String getName() {return "Sum of Squared Distances";}

    @Override
    public String getAbbreviatedName() {return "SSD";}    
    
    @Override
    public boolean isDistanceMetric() {return true;}      
    
    @Override
    public double[] compareMotifs(Motif motifA, Motif motifB) {
        double[][] matrixA=motifA.getMatrixAsFrequencyMatrix(); // forces conversion to frequencies between 0 and 1.0
        double[][] matrixB=motifB.getMatrixAsFrequencyMatrix(); // forces conversion to frequencies between 0 and 1.0

        double[] resultDirect=compareMotifsAndAlign(matrixA, matrixB);
        matrixB=Motif.reverseComplementMatrix(matrixB);
        double[] resultReverse=compareMotifsAndAlign(matrixA, matrixB);
        if (resultDirect[0]<=resultReverse[0]) return new double[]{resultDirect[0],1,resultDirect[1]};
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
        double result=Double.MAX_VALUE; //
        double offset=0; // alignment offset for best alignment. Position of motifB with respect to motifA
        for (int i=start;i<=end;i++) {
            double alignmentresult=compareAtAlignment(largermatrix,smallermatrix,i);
            if (alignmentresult<result) {result=alignmentresult;offset=(motifAlargest)?i:-i;}
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
            double colres=ssd(largermatrixColumn,smallermatrixColumn);
            result+=colres;
        }
        return result;
    }


    /** Returns the sum of squared distances for the two given columns */
    private double ssd(double[] columnA, double[] columnB) {
        return    ((columnA[0]-columnB[0])*(columnA[0]-columnB[0]))
                + ((columnA[1]-columnB[1])*(columnA[1]-columnB[1]))
                + ((columnA[2]-columnB[2])*(columnA[2]-columnB[2]))
                + ((columnA[3]-columnB[3])*(columnA[3]-columnB[3]));
    }


}