/*
 *
 */

package motiflab.engine.util;

import motiflab.engine.data.Motif;

/**
 * This compares two motifs using Weighted Information Content (WIC) as defined in the paper:
 * van Heeringen, Simon J and Veenstra, Gert J (2011) 
 * "GimmeMotifs: a de novo motif prediction pipeline for ChIP-sequencing experiments" 
 * Bioinformatics 27(2) : 270-271
 * 
 * 
 * @author kjetikl
 */
public class MotifComparator_WIC extends MotifComparator {
    private static final int minimumOverlap=3; // minimum number of overlapping columns
    private static final double[] uniform=new double[]{0.25f,0.25f,0.25f,0.25f};
    private static final double pseudo=0.01;

    @Override
    public String getName() {return "Weighted Information Content";}

    @Override
    public String getAbbreviatedName() {return "WIC";}    
    
    @Override
    public boolean isDistanceMetric() {return false;}      
    
    @Override
    public double[] compareMotifs(Motif motifA, Motif motifB) {
        double[][] matrixA=motifA.getMatrixAsFrequencyMatrixWithPseudo(pseudo); // forces conversion to frequencies between 0 and 1.0
        double[][] matrixB=motifB.getMatrixAsFrequencyMatrixWithPseudo(pseudo); // forces conversion to frequencies between 0 and 1.0

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
     * @param startA The position int the large matrix where the start of the smaller matrix has been aligned (can be negative)
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
            double colres=ColumnWIC(largermatrixColumn,smallermatrixColumn);
            result+=colres;
        }
        return result;
    }


    /** Returns the WIC score between two columns from different matrices. Equation (1) in the paper */
    private double ColumnWIC(double[] columnA, double[] columnB) {
        return Math.sqrt(IC(columnA)*IC(columnB))-2.5*DIC(columnA,columnB);
    }

    /** Returns the IC of a column. Equation (2) in the paper */
    private double IC(double[] column) {
        double IC=0;
        for (int n=0;n<4;n++) { // for each base: a,c,g,t
            IC+=(column[n]*log2(column[n]/0.25));           
        }
        return IC;
    }    
    
    /** Returns the DIC score between two columns from different matrices. Equation (3) in the paper */
    private double DIC(double[] columnA, double[] columnB) {
        double dic=0;
        for (int n=0;n<4;n++) { // for each base: a,c,g,t
            dic+=( (columnA[n]*log2(columnA[n]/0.25)) - (columnB[n]*log2(columnB[n]/0.25)) );           
        }
        return dic;
    }

    /** Return the Log2-value of the input*/
    private double log2(double value) {
        return (double)(Math.log(value)/Math.log(2));
    }

}