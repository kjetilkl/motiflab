/*
 
 
 */

package motiflab.engine.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.dataformat.DataFormat_INCLUSive_Background;
import motiflab.engine.operations.Operation_new;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.task.ExecutableTask;

/**
 *
 * @author kjetikl
 */
public class BackgroundModel extends Data {
    private static char[] bases={'A','C','G','T'};
    private String name="";
    private int order=0;
    private static String typedescription="Background Model";
    private HashMap<String,Double> transitionMatrix; // oligo => probability (e.g. "AGT" => 0.07 or "A" => 0.25) 
    // NOTE: the transitionMatrix is stored "flat" and the key is composed of "prefix+next base". Hence the number of bases in the key is the model order+1 !!
    private double[] oligoFrequencies=null; //  
    private String predefined=null; // name of predefined model
    private String trackname=null; // name of DNA track model
    private String trackSequenceCollection=null;
    private boolean trackStrandRelative=true; // this is only used if Background is based on a track (trackname!=null)
    private int organism=0; // Taxonomy ID
    private double[] snf=new double[]{0.25,0.25,0.25,0.25}; // single nucleotide frequencies
    
    /**
     * Creates a default Markov Model of 0th order with equal base probabilities
     * @param name
     */
    public BackgroundModel(String name) {
        this.name=name;       
        transitionMatrix=createTransitionMatrix(new double[]{0.25,0.25,0.25,0.25});
        oligoFrequencies=new double[]{0.25,0.25,0.25,0.25}; // snf is already set
        order=0;
    }
    
    /** 
     * Creates a new Markov model based on the given list of values
     * The number of values in the matrixvalues list should be a power of 4 and the order
     * of the model is determined from the number of values (4=0th order, 16=1st order, 64=2nd order etc.)
     * If matrixvalues is NULL the order is determined from the oligovalues parameter
     * If both these are NULL a model of 
     * This constructor is used by the NEW operation. If any of the parameters
     * @param snf an array of 4 values containing the Single Nucleotide Frequencies for A,C,G and T (0th order model)
     * @param oligovalues an array containing the oligo-frequencies
     * @param matrixvalues an array containing the probabilities for the transition matrix (as 1-dimensional flat format)
     */
    public BackgroundModel(String name, double[] snf, double[] oligovalues, double[] matrixvalues) {
        this.name=name;   
        order=-1;
        if (snf!=null) this.snf=snf;
        else {
            if (oligovalues!=null && oligovalues.length==4) this.snf=new double[]{oligovalues[0],oligovalues[1],oligovalues[2],oligovalues[3]};
            else this.snf=new double[]{0.25,0.25,0.25,0.25};
        }

        if (matrixvalues!=null) {
          transitionMatrix=createTransitionMatrix(matrixvalues);
          order=(int)(Math.log(transitionMatrix.size())/Math.log(4))-1;           
        }        
        if (oligovalues!=null) {
            this.oligoFrequencies=oligovalues;
            if (order<0) order=(oligoFrequencies.length==4)?0:(int)(Math.log(oligoFrequencies.length)/Math.log(4));           
        }
        if (order<0) order=0;
        if (oligovalues==null) {
            if (order==0) {
                this.oligoFrequencies=new double[]{this.snf[0],this.snf[1],this.snf[2],this.snf[3]};
            } else {
                this.oligoFrequencies=new double[(int)Math.pow(4, order)];
                double val=1.0f/oligoFrequencies.length;
                for (int i=0;i<oligoFrequencies.length;i++) oligoFrequencies[i]=val;
            }
        }
        if (matrixvalues==null) {
            matrixvalues=new double[(int)Math.pow(4,order+1)];
            double val=0.25;
            for (int i=0;i<matrixvalues.length;i++) matrixvalues[i]=val;
            transitionMatrix=createTransitionMatrix(matrixvalues);
        }

    }
    
    /* 
     * Creates a new Markov model based on the given list of values
     * The first 4 values in the list should specify the Single Nucleotide Frequencies 
     * for A,C,G and T respectively. The remaining values describes the transition
     * matrix and the number of these should be a power of 2 (The order of the model is 
     * determined from the number of values). For 0th order models this matrix
     * description can be omitted.
     * (thus, 0 or 4 values->0th order, 16 values->1st order, 64 values->2nd order etc.)
     * @param values an array containing the 4 SNF probabilitites followed by the 4**n probabilities for the transition matrix (in 1-dimensional flat format)
     *
    public BackgroundModel(String name, double[] values) {
        this.name=name;       
        double[] transitionMatrixValues=null;
        if (values.length==4) {
            snf=new double[]{values[0],values[1],values[2],values[3]};
            transitionMatrixValues=new double[]{values[0],values[1],values[2],values[3]};
            
        } else if (values.length>4) {
            snf=new double[]{values[0],values[1],values[2],values[3]};
            int matrixSize=values.length-4;
            transitionMatrixValues=new double[matrixSize];
            for (int i=4;i<values.length;i++) {
                transitionMatrixValues[i-4]=values[i];
            }
        }         
        transitionMatrix=createTransitionMatrix(transitionMatrixValues);
        order=(int)(Math.log(transitionMatrix.size())/Math.log(4))-1;
    }
    */
    
    
    /** Creates a new model based on the given internal model and Single Nucleotide Frequencies */
    public BackgroundModel(String name, double[] snf, double[] oligofrequencies, HashMap<String,Double> transitionMatrix){
        this.name=name;
        this.transitionMatrix=transitionMatrix;
        this.snf=snf;
        this.oligoFrequencies=oligofrequencies;
        order=(int)(Math.log(transitionMatrix.size())/Math.log(4))-1;
    }
    
    @Override
    public String[] getResultVariables() {
        return new String[]{"GC-content"};
    }

    @Override
    public boolean hasResult(String variablename) {
       if (variablename.equals("GC-content")) return true;
       else return false;
    }    
    
    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
         if (variablename.equals("GC-content")) {
             return new NumericVariable("temp",getGCcontent());            
         } 
         throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (variablename.equals("GC-content")) return NumericVariable.class;
       else return null;
    }        
    
      
    /** Creates a transitionMatrix for this Markov Model based on the list of input values. 
     * The values should be first 'ordered by prefix' and then by suffix.
     * Eg. like so: AAA, AAC, AAG, AAT, ACA, ACC, ACG, ACT, AGA, AGC, AGG, AGT, ATA, ATC, ATG, ATT, etc...
     */
    private HashMap<String,Double> createTransitionMatrix(double[] values) {
        int modelOrder=(int)(Math.log(values.length)/Math.log(4))-1;
        HashMap<String, Double> newmodel=new HashMap<String, Double>();
        for (int i=0;i<values.length;i++) {
            String seq=integerToPattern(i,modelOrder+1);
            newmodel.put(seq, values[i]);
        }
        return newmodel;
    }
    
    
    
    public static String integerToPattern(int index, int length) {
        String seq="";
        if (length==0) return seq;
        for (int i=1;i<=length;i++) {
            seq=bases[index%4]+seq;
            index=index/4;
        }
        return seq;
    }
    
    public static int patternToInteger(CharSequence pattern) {
        int size=pattern.length();
        int result=0;
        for (int i=0;i<size;i++) {
            char c=pattern.charAt(size-i-1);
            int index=0;
            switch (c) {
               case 'A': case 'a': index=0; break;
               case 'C': case 'c':  index=1; break;
               case 'G': case 'g':  index=2; break;
               case 'T': case 't':  index=3; break;
               default: return -1; // not a valid pattern
            }
            result+=index*(int)Math.pow(4, i);
        }
        return result;
    }
    
    public static int patternToInteger(char[] pattern) {
        int size=pattern.length;
        int result=0;
        for (int i=0;i<size;i++) {
            char c=pattern[size-i-1];
            int index=0;
            switch (c) {
               case 'A': case 'a': index=0; break;
               case 'C': case 'c':  index=1; break;
               case 'G': case 'g':  index=2; break;
               case 'T': case 't':  index=3; break;
               default: return -1; // not a valid pattern
            }
            result+=index*(int)Math.pow(4, i);
        }
        return result;
    }
    
    
    @Override
    public String getName() {return name;}

    @Override
    public void rename(String newname) {
        this.name=newname;
    }     
    
    
    public int getOrganism() {return organism;}
    
    public void setOrganism(int neworganism) {this.organism=neworganism;}
    
    
    @Override
    public Object getValue() {return this;}
    
    @Override
    public String getValueAsParameterString() { // Returns "fields" separated by semicolons. Each field starts with either SNF:, OLIGOS: or MATRIX: and is followed by a list of comma-separated double-values
        if (predefined!=null) return Operation_new.MODEL_PREFIX+predefined;
        else if (trackname!=null) {
            if (trackSequenceCollection!=null) return Operation_new.FROM_TRACK_PREFIX+trackname+",Sequences="+trackSequenceCollection+",Order="+order+",Strand="+((trackStrandRelative)?"Relative":"Direct");
            else return Operation_new.FROM_TRACK_PREFIX+trackname+",Order="+order+",Strand="+((trackStrandRelative)?"Relative":"Direct");
        }
        else {
            String parameterstring="SNF:"+snf[0]+","+snf[1]+","+snf[2]+","+snf[3];
            parameterstring+=";OLIGOS:"+getOligoFrequenciesAsString();
            parameterstring+=";MATRIX:"+getTransitionMatrixAsString();
            if (organism!=0) parameterstring+=";ORGANISM:"+organism;
            return parameterstring;
        }
    }
    
    public int getOrder() {return order;}
    
    public void setOrder(int neworder) {this.order=neworder;}
    
    public void setValue(double[] newSNF, double[] newOligoFrequencies, double[] newMatrix) {     
        this.snf=newSNF;
        this.oligoFrequencies=newOligoFrequencies;
        this.transitionMatrix=createTransitionMatrix(newMatrix);
        this.order=(int)(Math.log(transitionMatrix.size())/Math.log(4))-1;
        predefined=null;
        notifyListenersOfDataUpdate();
    }
    
    public void setPredefinedModel(String model) {
        this.predefined=model;
    }
    
    public String getPredefinedModel() {
        return predefined;
    }
   
    public void setTrackName(String track) {
        this.trackname=track;
    }
    public void setTrackSequenceCollection(String collectionName) {
        this.trackSequenceCollection=collectionName;
    }    
    
    /** Returns the name of the DNA track this background model was created from, or NULL if it was not created from a DNA track */
    public String getTrackName() {
        return trackname;
    }
    
    /** Returns the name of sequence collection this background model was created from, or NULL if it was not created from a DNA track */
    public String getTrackSequenceCollection() {
        return trackSequenceCollection;
    }    
    
    public void setTrackFromRelativeStrand(boolean fromSequenceStrand) {
        this.trackStrandRelative=fromSequenceStrand;
    }
    
    /** if this background model was created based on a DNA track, this method will return TRUE if the model was based on the strand relative to each sequences or FALSE if the direct strand was always used  */ 
    public boolean isTrackFromRelativeStrand() {
        return trackStrandRelative;
    }    

    /**
     * Given a DNA prefix, this method returns the probability of observing the
     * queried nextbase in the next position
     *
     * @param prefix (This should preferably be at least as long as the model order)
     * @return
     */
    public double getTransitionProbability(String prefix, char nextbase) throws ExecutionError {
        prefix=preprocessPrefix(prefix);
        if (prefix.length()>order) prefix=prefix.substring(prefix.length()-order, prefix.length()-1);
        else if (prefix.length()<order) prefix=fixPrefix(prefix);
        nextbase=Character.toUpperCase(nextbase);
        if (!(nextbase=='A' || nextbase=='C' || nextbase=='G' || nextbase=='T')) return 0.25; // uniform probability for non-base characters?
        Double prob=transitionMatrix.get(prefix+nextbase); if (prob==null) throw new ExecutionError("Background model '"+name+"' is missing value for prefix="+prefix+nextbase);
        return prob.doubleValue();
    }

    /**
     * Given a DNA sequence as input, this function returns a base which according to 
     * the model represents is a suitable continuation of the string provided.
     * The base to return is selected at random according to the model  
     * @param prefix If this is longer than the model order only the last part will be considered. If it is shorter
     * @return
     */
    public char getNextBase(String prefix) throws ExecutionError {
        prefix=preprocessPrefix(prefix);
        if (prefix.length()>order) prefix=prefix.substring(prefix.length()-order, prefix.length()-1);
        else if (prefix.length()<order) prefix=fixPrefix(prefix);
        Double probA=transitionMatrix.get(prefix+"A"); if (probA==null) throw new ExecutionError("Background model '"+name+"' is missing value for prefix="+prefix+"A");
        Double probC=transitionMatrix.get(prefix+"C"); if (probC==null) throw new ExecutionError("Background model '"+name+"' is missing value for prefix="+prefix+"C");
        Double probG=transitionMatrix.get(prefix+"G"); if (probG==null) throw new ExecutionError("Background model '"+name+"' is missing value for prefix="+prefix+"G");
        Double probT=transitionMatrix.get(prefix+"T"); if (probT==null) throw new ExecutionError("Background model '"+name+"' is missing value for prefix="+prefix+"T");
        double cumA=probA.doubleValue();
        double cumC=cumA+probC.doubleValue();
        double cumG=cumC+probG.doubleValue();
        double selected=(double)Math.random();
             if (selected<cumA) return 'A';
        else if (selected<cumC) return 'C';
        else if (selected<cumG) return 'G';
        else return 'T';
    }
    
    /**
     * Given a (short) DNA sequence as input, this function returns a list of bases which
     * according to the model represents is a suitable continuation of the string provided.
     * The list of bases is selected at random according to the model
     * @param prefix A string of DNA letters or null
     * @param length The number of bases to return
     * @return
     */
    public char[] getNextBases(String prefix, int length) throws ExecutionError {
        if (prefix.length()>order) prefix=prefix.substring(prefix.length()-order, prefix.length()-1);
        else if (prefix.length()<order) prefix=fixPrefix(prefix);
        char[] buffer=new char[length];
        for (int i=0;i<buffer.length;i++) {
            if (i>=order) prefix=new String(buffer, i-order, order);
            else if (i<order && i>0) { // prefix is made up from part original prefix and part buffer
               prefix=prefix.substring(1); // trim off first letter
               prefix=prefix+buffer[i-1];
            }
            buffer[i]=getNextBase(prefix);
        }
        return buffer;
    }
    
    /**
     * Selects a single base to return based on the single-nucleotide frequency distribution
     * of bases (i.e. a 0th-order model).
     * @return
     */
    public char getBaseFromSNF() {
        double cumA=snf[0];
        double cumC=cumA+snf[1];
        double cumG=cumC+snf[2];
        double selected=(double)Math.random();
             if (selected<cumA) return 'A';
        else if (selected<cumC) return 'C';
        else if (selected<cumG) return 'G';
        else return 'T';        
    }
    
    /**
     * Returns the Single Nucleotide Frequency for the given base according to this model
     * @param base
     * @return
     */
    public double getSNF(char base) {
         switch(base) {       
          case 'A': case 'a': return snf[0];
          case 'C': case 'c':  return snf[1];
          case 'G': case 'g':  return snf[2];
          case 'T': case 't':  return snf[3];
          default: return 0;
        }
    }
    
   /**
     * Returns the Single Nucleotide Frequency all the bases as a double[] (indexes correspond to A,C,G,T)
     * @param base
     * @return a copy of the SNF frequencies for this model
     */
    public double[] getSNF() {
         return new double[]{snf[0],snf[1],snf[2],snf[3]};
    }
    
    /**
     * Returns the GC-content of this background model (as a number between 0 and 1)
     * @return 
     */
    public double getGCcontent() {
        return snf[1]+snf[2];
    }
    
    /**
     * Returns the frequency for the given oligomer according to this model
     * @param oligo
     * @return
     */
    public double getOligoFrequency(String oligo) {
         oligo=oligo.toUpperCase();         
         int index=patternToInteger(oligo);
         if (oligoFrequencies==null || index>=oligoFrequencies.length) return 0; 
         else return oligoFrequencies[index];
    }
    
    
    
    /**
     * Returns the transitionMatrix for this model as a 2-dimensional Double array
     * The first dimension has length 4^order and corresponds to the prefixes while
     * the second dimensions has length 4 corresponding to the 4 bases A,C,G and T
     * @return
     */
    public Double[][] getTransitionMatrix() {
        int size=(int)Math.pow(4, order);
        Double[][] result=new Double[size][4];
        for (int i=0;i<size;i++) {
            String prefix=integerToPattern(i, order);
            result[i][0]=transitionMatrix.get(prefix+"A");
            result[i][1]=transitionMatrix.get(prefix+"C");
            result[i][2]=transitionMatrix.get(prefix+"G");
            result[i][3]=transitionMatrix.get(prefix+"T");
        }
        return result;
    }
    
    /**
     * Returns the transitionMatrix for this model as a large string of comma-separated numbers
     * @return
     */
    public String getTransitionMatrixAsString() {
        String result="";
        int size=(int)Math.pow(4, order);
        for (int i=0;i<size;i++) {
            String prefix=integerToPattern(i, order);
            result+=transitionMatrix.get(prefix+"A")+",";
            result+=transitionMatrix.get(prefix+"C")+",";
            result+=transitionMatrix.get(prefix+"G")+",";
            result+=transitionMatrix.get(prefix+"T")+",";
        }
        result=result.substring(0,result.length()-1);
        return result;
    }
    
    /**
     * Returns the oligoFrequencies for this model as an array 
     * @return
     */
    public double[] getOligoFrequencies() {
        return oligoFrequencies;
    }
    
    /**
     * Returns the oligoFrequencies for this model as a large string of comma-separated numbers
     * @return
     */
    public String getOligoFrequenciesAsString() {
        String result="";
        if (oligoFrequencies==null) return "";
        for (int i=0;i<oligoFrequencies.length-1;i++) {
            result+=oligoFrequencies[i]+",";
        }
        result+=oligoFrequencies[oligoFrequencies.length-1];
        return result;
    }
    
    
    
    /**
     * If the prefix provided is shorter than the model,
     * this method can be used to find a suitable prefix of the correct length 
     */ 
    private String fixPrefix(String prefix) {
        int basesMissing=order-prefix.length();
        if (basesMissing<=0) return prefix; // the prefix is already long enough
        // for now we just add random bases (according to SNF) to the front of the prefix until it is long enough
        // is there a more "scientific" way to determine a suitable prefix?
        while (prefix.length()<order) {
            prefix=getBaseFromSNF()+prefix;
        }
        return prefix;
    }
    
    
    /** Converts prefix to all uppercase and replaces unknown letters with random bases */
    private String preprocessPrefix(String prefix) {
        prefix=prefix.toUpperCase();
        for (int i=0;i<prefix.length();i++) {
            char atI=prefix.charAt(i);
            if (atI!='A' && atI!='C' && atI!='G' &&atI!='T') {
                prefix.replaceFirst(""+atI,""+getBaseFromSNF());
            }
        }
        return prefix;
    }

    /**
     * Returns a randomly selected oligo of length equal to the model order
     * This oligo can be used as a prefix to start off sequence generation
     * @return
     */
    public String selectSuitablePrefix() {
        if (order==0) return "";
        double sum=0;
        double random=Math.random();
        for (int i=0;i<oligoFrequencies.length;i++) {
            sum+=oligoFrequencies[i];
            if (random<sum) return integerToPattern(i, order);
        }
        return integerToPattern(oligoFrequencies.length-1, order); 
    }


    @Override
    public void importData(Data source) throws ClassCastException {
        BackgroundModel datasource=(BackgroundModel)source;
        this.name=datasource.name;    
        this.order=datasource.order;
        this.predefined=datasource.predefined;
        this.trackname=datasource.trackname;
        this.trackSequenceCollection=datasource.trackSequenceCollection;        
        this.organism=datasource.organism;
        this.snf=datasource.snf;
        this.oligoFrequencies=datasource.oligoFrequencies;
        this.transitionMatrix=datasource.transitionMatrix;
        this.trackStrandRelative=datasource.trackStrandRelative;
        //notifyListenersOfDataUpdate(); 
    }
    

    @Override
    public BackgroundModel clone() {
        HashMap<String,Double> newinternalmodel=new HashMap<String,Double>(transitionMatrix.size());
        Set<String> keySet=transitionMatrix.keySet();
        for (String key:keySet) {
            newinternalmodel.put(key,new Double(transitionMatrix.get(key).doubleValue()));
        }
        double[] newsnf=new double[snf.length];
        System.arraycopy(snf, 0, newsnf, 0, snf.length);
        double[] newoligos=new double[oligoFrequencies.length];
        System.arraycopy(oligoFrequencies, 0, newoligos, 0, oligoFrequencies.length);
        
        BackgroundModel newmodel=new BackgroundModel(name,newsnf,newoligos,newinternalmodel);
        newmodel.predefined=predefined;
        newmodel.trackname=trackname;
        newmodel.trackSequenceCollection=trackSequenceCollection;        
        newmodel.organism=organism;
        newmodel.trackStrandRelative=trackStrandRelative;
        return newmodel;
    }
    
    @Override
    public String getTypeDescription() {
        String suffix=null;
        if (order==1) suffix="st";
        else if (order==2) suffix="nd";
        else if (order==3) suffix="rd";
        else suffix="th";
        if (predefined!=null) return typedescription+" : \""+predefined+"\"  "+order+suffix+ " order";
        else return typedescription+" : "+order+suffix+" order";
    }

   
    public static String getType() {
        return typedescription;
    }    
    
    @Override
    public String getDynamicType() {
        return typedescription;
    }
    /** 
     * Returns true if this model has the same Single Nucleotide Frequencies,
     * the same oligo frequency values and the same transition matrix as the given Markov Model
     */
    @Override
    public boolean containsSameData(Data other) {
        if (other==null || !(other instanceof BackgroundModel)) return false;
        BackgroundModel model=(BackgroundModel)other;
        if (getOrder()!=model.getOrder()) return false;
        if (trackname!=null && model.trackname==null) return false;
        if (trackname==null && model.trackname!=null) return false;
        if (trackname!=null && model.trackname!=null && !trackname.equals(model.trackname)) return false;
        if (trackSequenceCollection!=null && model.trackSequenceCollection==null) return false;
        if (trackSequenceCollection==null && model.trackSequenceCollection!=null) return false;
        if (trackSequenceCollection!=null && model.trackSequenceCollection!=null && !trackSequenceCollection.equals(model.trackSequenceCollection)) return false;
        if (trackStrandRelative!=model.trackStrandRelative) return false;
        boolean sameSFN=(snf[0]==model.snf[0] && snf[1]==model.snf[1] && snf[2]==model.snf[2] && snf[3]==model.snf[3]);
        if (!sameSFN) return false;
        if ((this.oligoFrequencies==null && model.oligoFrequencies!=null) || (this.oligoFrequencies!=null && model.oligoFrequencies==null) || (this.oligoFrequencies.length!=model.oligoFrequencies.length)) return false;
        for (int i=0;i<oligoFrequencies.length;i++) {
            if (this.oligoFrequencies[i]!=model.oligoFrequencies[i]) return false;
        }
        Set<String> keySet=transitionMatrix.keySet();
        for (String key:keySet) {
            if (transitionMatrix.get(key).doubleValue()!=model.transitionMatrix.get(key).doubleValue()) return false;
        }
        return true;
    }
    
    @Override
    public String output() {
        double[] frequencies=convertBackgroundToPlainFormat(this);
        String result="";
        for (int i=0;i<frequencies.length;i++) {
            result+=""+frequencies[i]+"\n";
        }
        return result;
    }
    
    @Override
    public void inputFromPlain(ArrayList<String> input, MotifLabEngine engine) throws ParseError {
        Iterator iter=input.iterator();
        while (iter.hasNext()) {
            String next=(String)iter.next();
            if (next.trim().isEmpty()) iter.remove();
        }
        double[] values=new double[input.size()];
        for (int i=0;i<input.size();i++) {
            String string=input.get(i);
            try {
                values[i]=Double.parseDouble(string);
            } catch (NumberFormatException nfe) {throw new ParseError("Unable to parse expected numerical value: "+string);}
        }
        String thisname=getName();
        BackgroundModel model=convertPlainFormatToBackground(values);
        importData(model);
        name=thisname;
    }
    
    /** Returns the probability of generating the given dnaString from this background model
     *  NOTE: THIS METHOD HAS NOT BEEN PROPERLY TESTED AND MIGHT NOT WORK AS IT SHOULD
     *  @param dnaString
     */
    @Deprecated
    public double getStringProbability(String dnaString) {
        dnaString=dnaString.toUpperCase();
        if (dnaString.matches(".*[^ACGT].*")) return 0; // string contains non-base characters
        int size=dnaString.length();
        if (size==order) return getOligoFrequency(dnaString);
        else if (size<order || order==0) { // I just use repeated SNF multiplications, but there is probably a more correct way
          double probability=1;
          for (int i=0;i<dnaString.length();i++) {
              char c=dnaString.charAt(i);
              probability*=getSNF(c);
          }
          return probability;
        } else if (size>order) {
            double probability=getOligoFrequency(dnaString.substring(0,order));
            for (int i=0;i<size-order;i++) {
                String transitionString=dnaString.substring(i,i+order+1);
                double transitionProbability=transitionMatrix.get(transitionString);
                probability*=transitionProbability;
            }
            return probability;
        }
        return 0;
    }
    
    /** Converts a background in MotifLab format to plain (PRIORITY) format */ 
    public static double[] convertBackgroundToPlainFormat(BackgroundModel background) {
        int order=background.getOrder();
        HashMap<String,double[]> conditionalProbabilities=new HashMap<String,double[]>(); // the string is the suffix and the Double contains probabilities for [A,C,G,T] in order
        int arraySize=0;
        for (int k=1;k<=order+1;k++) arraySize+=(int)Math.pow(4,k);
        double[] result=new double[arraySize];
        double[] oligoFrequencies=background.getOligoFrequencies();
        HashMap<String,Double> temp=new HashMap<String,Double>();
        for (int i=0;i<oligoFrequencies.length;i++) {
            String oligo=BackgroundModel.integerToPattern(i, order);
            temp.put(oligo,oligoFrequencies[i]); // initialize temp-map with stored oligo frequencies
        }
        if (order>0) { // process the "raw" oligo frequencies with length==order
            for (int i=0;i<Math.pow(4,order);i++) {
                String oligo=BackgroundModel.integerToPattern(i, order); // the smaller oligos. Size is minimum 1
                String prefix=(oligo.length()>1)?oligo.substring(0,oligo.length()-1):"no_prefix";
                char nextBase=oligo.charAt(oligo.length()-1);
                double value=temp.get(oligo); 
                if (!conditionalProbabilities.containsKey(prefix)) conditionalProbabilities.put(prefix, new double[]{0,0,0,0});
                int index=0;
                if (nextBase=='C') index=1; else if (nextBase=='G') index=2; else if (nextBase=='T') index=3;
                conditionalProbabilities.get(prefix)[index]=value;                    
            }        
        }
        // now calcuate frequencies for all oligos of smaller lengths
        for (int size=order;size>1;size--) { // 
            for (int i=0;i<Math.pow(4,size);i++) {
                String oligo=BackgroundModel.integerToPattern(i, size);
                // an oligo can only contain 2 words that are 1 bp smaller
                String first =oligo.substring(0,oligo.length()-1);
                String second=oligo.substring(1);
                double oligofrequency=temp.get(oligo);
                if (!temp.containsKey(first)) temp.put(first,0.0);
                if (!temp.containsKey(second)) temp.put(second,0.0);
                temp.put(first,  temp.get(first) +oligofrequency);
                temp.put(second, temp.get(second)+oligofrequency); 
            }
            for (int i=0;i<Math.pow(4,size-1);i++) {
                String oligo=BackgroundModel.integerToPattern(i, size-1); // the smaller oligos. Size is minimum 1
                String prefix=(oligo.length()>1)?oligo.substring(0,oligo.length()-1):"no_prefix";
                char nextBase=oligo.charAt(oligo.length()-1);
                double value=temp.get(oligo)/2f; // divide by 2 since each suboligo is counted twice (once as the "second" word in and once as the "first" word)
                temp.put(oligo, value);
                if (!conditionalProbabilities.containsKey(prefix)) conditionalProbabilities.put(prefix, new double[]{0,0,0,0});
                int index=0;
                if (nextBase=='C') index=1; else if (nextBase=='G') index=2; else if (nextBase=='T') index=3;
                conditionalProbabilities.get(prefix)[index]=value;                    
            }                    
        }
        // normalize and fill in result array
        result[0]=background.getSNF('A');
        result[1]=background.getSNF('C');
        result[2]=background.getSNF('G');
        result[3]=background.getSNF('T');
        int index=0;
        for (int size=1;size<order;size++) {
            index+=Math.pow(4,size);
            for (int i=0;i<Math.pow(4,size);i++) {
                String prefix=BackgroundModel.integerToPattern(i, size); 
                double[] values=conditionalProbabilities.get(prefix);
                if (values==null) values=new double[]{0,0,0,0};
                double total=values[0]+values[1]+values[2]+values[3];
                for (int j=0;j<4;j++) {
                    result[index+(i*4)+j]=(values[j]/total);
                }
            }
            
        }
        // finally fill in the transitions from the matrix 
        if (order>0) {
            index+=Math.pow(4,order);
            Double[][] matrix=background.getTransitionMatrix();
            for (int i=0;i<Math.pow(4,order);i++) {
                for (int j=0;j<4;j++) {
                    result[index+(i*4)+j]=matrix[i][j];
                }
            }              
        }
        return result;
    }    
    
    
      /** Converts a background in MotifLab format to oligo-frequency format (used by e.g. MEME)
       * For a background model of order k, the resulting array contains oligo-frequencies for all 
       * word lengths from 1 up to k+1. Thus a zero-order model contains frequencies for "a,c,g,t" while
       * a first-order model contains frequencies for "a,c,g,t" followed by "aa,ac,ag,at,ca,cc,cg,ct,ga,gc,gg,gt,ta,tc,tg,tt"
       * the sum of frequencies for each word-length must sum to 1.
       */ 
    public static double[] convertBackgroundToFrequencyFormat(BackgroundModel background) {
        int order=background.getOrder();
        int arraySize=0;
        for (int k=1;k<=order+1;k++) arraySize+=(int)Math.pow(4,k);
        double[] result=new double[arraySize];
        double[] oligoFrequencies=background.getOligoFrequencies();
        HashMap<String,Double> temp=new HashMap<String,Double>();
        for (int i=0;i<oligoFrequencies.length;i++) {
            String oligo=BackgroundModel.integerToPattern(i, order);
            temp.put(oligo,oligoFrequencies[i]); // initialize temp-map with stored oligo frequencies
        }
        // calculate frequencies for all oligos of smaller lengths
        for (int size=order;size>1;size--) { // process oligo size from max to 1.
            // Frequencies for the largest oligo size is already in the "temp" table. Now process smaller sizes based on frequencies of longer words
            for (int i=0;i<Math.pow(4,size);i++) {
                String oligo=BackgroundModel.integerToPattern(i, size);
                // an oligo can only contain 2 words that are 1 bp smaller
                String first =oligo.substring(0,oligo.length()-1);
                String second=oligo.substring(1);
                double oligofrequency=temp.get(oligo); // find frequency for this oligo and add to both oligos 1 bp smaller
                oligofrequency=oligofrequency/2.0f;
                if (!temp.containsKey(first)) temp.put(first,0.0);
                if (!temp.containsKey(second)) temp.put(second,0.0);
                temp.put(first,  temp.get(first) +oligofrequency); 
                temp.put(second, temp.get(second)+oligofrequency); 
            }
        }
        int index=0;
        for (int size=1;size<=order;size++) {      
            for (int i=0;i<Math.pow(4,size);i++) {
                String oligo=BackgroundModel.integerToPattern(i, size); 
                double value=temp.get(oligo);
                result[index+i]=value;                
            }     
            index+=Math.pow(4,size);   
        }
        result[0]=background.getSNF('A');
        result[1]=background.getSNF('C');
        result[2]=background.getSNF('G');
        result[3]=background.getSNF('T');        
        // finally fill in the transitions from the matrix 
        if (order>0) {
            Double[][] matrix=background.getTransitionMatrix();
            index=result.length-(matrix.length*4);
            for (int i=0;i<Math.pow(4,order);i++) { // for each oligo prefix
                String oligo=BackgroundModel.integerToPattern(i, order); 
                double oligofrequency=temp.get(oligo);
                for (int j=0;j<4;j++) {
                    result[index+(i*4)+j]=oligofrequency*matrix[i][j];
                }
            }              
        }
        return result;
    }    
      
    
    
    
    
    /** Converts a background in plain (PRIORITY) format to MotifLab format 
     * The double values represent transition probabilities at different orders (oligo lengths)
     * each consecutive four values should sum to 1 and represent a transition probabilities from a prefix (which could be empty)
     * to the bases a,c,g and t. Thus, the first 4 values are transition from "null" to a,c,g,t the next 16 are transitions from "a","c","g" and "t" to another base (4 of each)
     * then follows transitions from "aa" and other oligos of length 2 etc.
     */ 
    public static BackgroundModel convertPlainFormatToBackground(double[] values) throws ParseError {
        int order=-1;
        int expectedsize=0;
        while (expectedsize<values.length) {
           order++; // try higher orders
           expectedsize+=Math.pow(4,order+1);
        }
        if (expectedsize>values.length || values.length<4) throw new ParseError("The model does not contain a valid number of probabilities (found "+values.length+")");
        int matrixsize=(int)Math.pow(4,order+1);
        double[] snf=Arrays.copyOfRange(values, 0, 4); // first 4 values represent single nucleotide frequencies. Note that last coordinate in range is exclusive
        double[] oligofrequencies=Arrays.copyOfRange(values, 0, 4); 
        double[] transitionMatrix=Arrays.copyOfRange(values, values.length-matrixsize, values.length);
        if (order>=2) { // need to calculate oligofrequencies from conditional probabilities
            HashMap<String,Double> map=new HashMap<String,Double>(values.length-matrixsize);
            map.put("A",snf[0]);
            map.put("C",snf[1]);
            map.put("G",snf[2]);
            map.put("T",snf[3]);
            int index=0;
            int currentorder=2;
            while (currentorder<=order) {
                index=index+(int)Math.pow(4,currentorder-1);
                int blocksize=(int)Math.pow(4,currentorder);
                for (int i=0;i<blocksize;i++) {
                    String pattern=integerToPattern(i,currentorder);
                    double conditionalProb=values[index+i];
                    String prefix=pattern.substring(0,pattern.length()-1);
                    double prefixProb=map.get(prefix);
                    map.put(pattern, conditionalProb*prefixProb); // The pattern represents the prefix oligo followed by the next base, e.g. for P(A|GTC) the pattern will be GTCA                    
                }
                currentorder++;
            }
            int numberofoligos=(int)Math.pow(4,order);
            oligofrequencies=new double[numberofoligos];
            for (int i=0;i<numberofoligos;i++) {
                String pattern=integerToPattern(i,order);
                oligofrequencies[i]=map.get(pattern);
            }
        }
        BackgroundModel bgmodel=new BackgroundModel("temporary", snf, oligofrequencies, transitionMatrix);
        return bgmodel;
    }     
    
    /** Converts an oligo frequency table to MotifLab format
     * For models of order k the values-table should contain oligo frequencies for all word-lengths
     * from 1 to k+1. Thus, a second-order background model should start with the oligo frequencies for a,c,g and t
     * then for aa,ac,ag,at,ca,cc,cg,ct,ga,gc,gg,gt,ta,tc,tg,tt and last aaa,aac,aag,aat,aca,acc.. etc.
     * The probabilities for each block (oligos of same length) should sum to 1
     */ 
    public static BackgroundModel convertFrequencyFormatToBackground(double[] values) throws ParseError {
        int order=-1;
        int expectedsize=0;
        while (expectedsize<values.length) {
           order++; // try higher orders
           expectedsize+=Math.pow(4,order+1);
        }
        if (expectedsize>values.length || values.length<4) throw new ParseError("The model does not contain a valid number of probability values (found "+values.length+")");
        int matrixsize=(int)Math.pow(4,order+1);
        double[] snf=Arrays.copyOfRange(values, 0, 4); // first 4 values represent single nucleotide frequencies. Note that last coordinate in range is exclusive
        double[] oligofrequencies=null;
        if (order>=2) {
            int oligotablesize=(int)Math.pow(4,order);
            oligofrequencies=Arrays.copyOfRange(values, values.length-(matrixsize+oligotablesize), values.length-matrixsize); 
        } else {
            oligofrequencies=Arrays.copyOfRange(values, 0, 4); // for order 0 and 1, these are the same as the SNF values
        }        
        double[] transitionMatrix=Arrays.copyOfRange(values, values.length-matrixsize, values.length);
        // now renormalize the transitionMatrix probabilities. Each 'row' should sum to 1
        for (int i=0;i<transitionMatrix.length;i+=4) {
            double total=transitionMatrix[i]+transitionMatrix[i+1]+transitionMatrix[i+2]+transitionMatrix[i+3];
            transitionMatrix[i]=transitionMatrix[i]/total;
            transitionMatrix[i+1]=transitionMatrix[i+1]/total;
            transitionMatrix[i+2]=transitionMatrix[i+2]/total;
            transitionMatrix[i+3]=transitionMatrix[i+3]/total;
        }
        BackgroundModel bgmodel=new BackgroundModel("temporary", snf, oligofrequencies, transitionMatrix);
        return bgmodel;
    }     
    
public static Object[][] predefinedModels={
    {"Uniform",new Integer(0),new Integer(0),"uniform.bg"},
    {"Ensembl_Human_Upstream_2000US_to_200DS",new Integer(3),new Integer(9606),"Ensembl_human_2000-200bp_aroundTSS.bg"},
    {"Ensembl_Human_Upstream_400US",new Integer(3),new Integer(9606),"Ensembl_human_400bp_upstreamTSS.bg"},
    {"Ensembl_Human_Mouse_Syntenic_1000US_to_200DS",new Integer(3),new Integer(9606),"Ensembl_human_mouseSyntenic_1000-200bp_aroundTSS.bg"},
    {"Ensembl_Human_Mouse_Syntenic_10000US_to_200DS",new Integer(3),new Integer(9606),"Ensembl_human_mouseSyntenic_10000-200bp_aroundTSS.bg"},
    {"Ensembl_Human_Mouse_Syntenic_1000US",new Integer(3),new Integer(9606),"Ensembl_human_mouseSyntenic_up1000bp_fromTSS.bg"},
    {"EPD_cow_0",new Integer(0),new Integer(9913),"epd_bos_taurus_499_chromgenes_non_split_0.bg"},
    {"EPD_cow_1",new Integer(1),new Integer(9913),"epd_bos_taurus_499_chromgenes_non_split_1.bg"},
    {"EPD_cow_2",new Integer(2),new Integer(9913),"epd_bos_taurus_499_chromgenes_non_split_2.bg"},
    {"EPD_cow_3",new Integer(3),new Integer(9913),"epd_bos_taurus_499_chromgenes_non_split_3.bg"},
    {"EPD_chicken_0",new Integer(0),new Integer(9031),"epd_gallus_gallus_499_chromgenes_non_split_0.bg"},
    {"EPD_chicken_1",new Integer(1),new Integer(9031),"epd_gallus_gallus_499_chromgenes_non_split_1.bg"},
    {"EPD_chicken_2",new Integer(2),new Integer(9031),"epd_gallus_gallus_499_chromgenes_non_split_2.bg"},
    {"EPD_chicken_3",new Integer(3),new Integer(9031),"epd_gallus_gallus_499_chromgenes_non_split_3.bg"},
    {"EPD_human_0",new Integer(0),new Integer(9606),"epd_homo_sapiens_499_chromgenes_non_split_0.bg"},
    {"EPD_human_1",new Integer(1),new Integer(9606),"epd_homo_sapiens_499_chromgenes_non_split_1.bg"},
    {"EPD_human_2",new Integer(2),new Integer(9606),"epd_homo_sapiens_499_chromgenes_non_split_2.bg"},
    {"EPD_human_3",new Integer(3),new Integer(9606),"epd_homo_sapiens_499_chromgenes_non_split_3.bg"},
    {"EPD_mouse_0",new Integer(0),new Integer(10090),"epd_mus_musculus_499_chromgenes_non_split_0.bg"},
    {"EPD_mouse_1",new Integer(1),new Integer(10090),"epd_mus_musculus_499_chromgenes_non_split_1.bg"},
    {"EPD_mouse_2",new Integer(2),new Integer(10090),"epd_mus_musculus_499_chromgenes_non_split_2.bg"},
    {"EPD_mouse_3",new Integer(3),new Integer(10090),"epd_mus_musculus_499_chromgenes_non_split_3.bg"},
    {"EPD_rat_0",new Integer(0),new Integer(10116),"epd_rattus_norvegicus_499_chromgenes_non_split_0.bg"},
    {"EPD_rat_1",new Integer(1),new Integer(10116),"epd_rattus_norvegicus_499_chromgenes_non_split_1.bg"},
    {"EPD_rat_2",new Integer(2),new Integer(10116),"epd_rattus_norvegicus_499_chromgenes_non_split_2.bg"},
    {"EPD_rat_3",new Integer(3),new Integer(10116),"epd_rattus_norvegicus_499_chromgenes_non_split_3.bg"},
    {"EPD_vertebrate_0",new Integer(0),new Integer(0),"epd_vertebrates_499_chromgenes_non_split_0.bg"},
    {"EPD_vertebrate_1",new Integer(1),new Integer(0),"epd_vertebrates_499_chromgenes_non_split_1.bg"},
    {"EPD_vertebrate_2",new Integer(2),new Integer(0),"epd_vertebrates_499_chromgenes_non_split_2.bg"},
    {"EPD_vertebrate_3",new Integer(3),new Integer(0),"epd_vertebrates_499_chromgenes_non_split_3.bg"},
    {"EPD_frog_0",new Integer(0),new Integer(8364),"epd_xenopus_laevis_499_chromgenes_non_split_0.bg"},
    {"EPD_frog_1",new Integer(1),new Integer(8364),"epd_xenopus_laevis_499_chromgenes_non_split_1.bg"},
    {"EPD_frog_2",new Integer(2),new Integer(8364),"epd_xenopus_laevis_499_chromgenes_non_split_2.bg"},
    {"EPD_frog_3",new Integer(3),new Integer(8364),"epd_xenopus_laevis_499_chromgenes_non_split_3.bg"},
    {"hs200bp5",new Integer(3),new Integer(9606),"hs200bp5.bg"},
    {"Human_distal",new Integer(3),new Integer(9606),"hs_distal_manual_r18_3.bg"},
    {"Ensembl_Human_23299genes",new Integer(3),new Integer(9606),"hs_ensembl16_23299genes_1000bp_upstream.bg"},
    {"Human_Mouse_upstream",new Integer(3),new Integer(9606),"hs_upstreamcr_mm_3.bg"},
    {"human3",new Integer(3),new Integer(9606),"human3.bg"},
    {"Mouse_distal",new Integer(3),new Integer(10090),"mm_distal_manual_r18_3.bg"},
    {"SCerevisiae_0",new Integer(0),new Integer(4932),"scerevisiae_0.bg"},
    {"SCerevisiae_1",new Integer(1),new Integer(4932),"scerevisiae_1.bg"},
    {"SCerevisiae_2",new Integer(2),new Integer(4932),"scerevisiae_2.bg"},
    {"SCerevisiae_3",new Integer(3),new Integer(4932),"scerevisiae_3.bg"},
    {"SCerevisiae_4",new Integer(4),new Integer(4932),"scerevisiae_4.bg"},
    {"SCerevisiae_5",new Integer(5),new Integer(4932),"scerevisiae_5.bg"}     
};

    public static String getFilenameForModel(String modelname) {
        for (Object[] entry:predefinedModels) {
            if (modelname.equals((String)entry[0])) return (String)entry[3];
        }
        return null;
    }

    /** Given the first line of a file, this method returns the name of the
     *  dataformat believed to be used for the file, or null if the format is
     *  not recognized
     */
    public static String determineDataFormatFromHeader(String firstline) {
             if (firstline.startsWith("#INCLUSive Background Model")) return "INCLUSive_Background_Model";
        else if (firstline.split("\\t").length==2) return "MEME_Background";
        else return "PriorityBackground";
    }


    public static BackgroundModel parseBackgroundModelParameters(String text, String targetName, MotifLabEngine engine, OperationTask task) throws ExecutionError, InterruptedException {
        if (text.startsWith(Operation_new.MODEL_PREFIX)) {
            String modelName=text.substring(Operation_new.MODEL_PREFIX.length());
            String filename=BackgroundModel.getFilenameForModel(modelName);
            if (filename==null) throw new ExecutionError("Unknown Background Model: "+modelName);
            BufferedReader inputStream=null;
            ArrayList<String> input=new ArrayList<String>();
            try {
                inputStream=new BufferedReader(new InputStreamReader(task.getClass().getResourceAsStream("/motiflab/engine/resources/"+filename)));
                String line;
                while((line=inputStream.readLine())!=null) {input.add(line);}
            } catch (IOException e) {
                throw new ExecutionError("An error occurred when loading predefined Background model: ["+e.getClass().getSimpleName()+"] "+e.getMessage(),0);
            } finally {
                try {
                    if (inputStream!=null) inputStream.close();
                } catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader Operation_new.parseMarkovModelParameters(): "+ioe.getMessage());}
            }
            DataFormat_INCLUSive_Background format=new DataFormat_INCLUSive_Background();
            BackgroundModel data=new BackgroundModel(targetName);
            try {data=(BackgroundModel)format.parseInput(input, data, null, task);}
            catch (InterruptedException ie) {throw ie;}
            catch (Exception e) {throw new ExecutionError(e.getMessage());}
            data.setPredefinedModel(modelName);
            return data;
        } else if (text.startsWith(Operation_new.FROM_TRACK_PREFIX)) {
            String parameter=text.substring(Operation_new.FROM_TRACK_PREFIX.length());
            String[] parts=parameter.split("\\s*,\\s*");
            String modelorderString="0";
            boolean relativeOrientation=true;
            String trackSequencesString=null;
            String trackname=parts[0];            
            if (trackname.isEmpty()) throw new ExecutionError("Missing track name");
            if (parts.length>1) {
                for (int i=1;i<parts.length;i++) {
                    String part=parts[i];
                    if (part.matches("(?i)Sequences\\s*(=|:)\\s*(\\w+)")) {
                        String[] elements=part.split("\\s*(=|:)\\s*");
                        trackSequencesString=elements[1];
                    } else if (part.matches("(?i)Order\\s*(=|:)\\s*(\\w+)")) {
                        String[] elements=part.split("\\s*(=|:)\\s*");
                        modelorderString=elements[1];
                    } else if (part.matches("(?i)Strand\\s*(=|:)\\s*(\\w+)")) {
                        String[] elements=part.split("\\s*(=|:)\\s*");
                        if (elements[1].equalsIgnoreCase("sequence")||elements[1].equalsIgnoreCase("gene")||elements[1].equalsIgnoreCase("relative")) relativeOrientation=true;
                        else if (elements[1].equalsIgnoreCase("direct")) relativeOrientation=false;
                        else throw new ExecutionError("Unrecognized value for parameter 'Strand': "+elements[1]);
                    } else throw new ExecutionError("Unrecognized parameter: "+part);
                }
            }
            Data datatrack=engine.getDataItem(trackname);
            if (datatrack==null) throw new ExecutionError("Unknown data item: "+trackname);
            if (!(datatrack instanceof DNASequenceDataset)) throw new ExecutionError(trackname+" is not a DNA Sequence Dataset");
            SequenceCollection collection=engine.getDefaultSequenceCollection();
            if (trackSequencesString!=null) {
                Data seqcol=engine.getDataItem(trackSequencesString);
                if (seqcol==null) throw new ExecutionError("Unknown data item: "+trackSequencesString);
                if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(trackSequencesString+" is not a Sequence Collection");
                else collection=(SequenceCollection)seqcol;
            }            
            BackgroundModel data=createModelFromDNATrack((DNASequenceDataset)datatrack, collection, modelorderString, relativeOrientation, engine, task);
            data.rename(targetName);
            return data;
        } else {
            double[] snf=null;
            double[] matrix=null;
            double[] oligos=null;
            text=text.toUpperCase();
            String[] segments=text.split("\\s*;\\s*");
            int organism=0;
            for (String segment:segments) {
                if (segment.trim().isEmpty()) continue;
                if (segment.startsWith("SNF:") && segment.length()>4) {
                    snf=parseMarkovModelParameters_SNF(segment.substring(4));
                } else if (segment.startsWith("OLIGOS:") && segment.length()>7) {
                    oligos=parseMarkovModelParameters_OligosAndMatrix(segment.substring(7));
                } else if (segment.startsWith("MATRIX:") && segment.length()>7) {
                    matrix=parseMarkovModelParameters_OligosAndMatrix(segment.substring(7));
                } else if (segment.startsWith("ORGANISM:") && segment.length()>9) {
                    try {
                        organism=Integer.parseInt(segment.substring(9));
                    } catch (Exception e) {throw new ExecutionError("Unable to parse expected numeric Organism taxonomy ID: "+segment);}
                } else throw new ExecutionError("Background model parameters should start with either SNF:, OLIGOS: or MATRIX:");
            }
            if (matrix!=null) {
                double log4=Math.log(matrix.length)/Math.log(4);
                int order=(int)log4;
                if (oligos!=null) {
                    if (order<1 && oligos.length!=4) throw new ExecutionError("Expected 4 oligo frequency values for Background Model of order "+order);
                    else if (order>1 && oligos.length!=matrix.length/4) throw new ExecutionError("Expected "+(matrix.length/4)+" oligo frequency values for Background Model of order "+order);
                }
            }
            BackgroundModel newmodel=new BackgroundModel(targetName,snf,oligos,matrix);
            if (organism!=0) newmodel.setOrganism(organism);
            return newmodel;
        }
    }

    private static double[] parseMarkovModelParameters_SNF(String text) throws ExecutionError {
        String[] list=text.split("\\s*,\\s*");
        if (list.length!=4) throw new ExecutionError("The number of Background Model SNF values must be 4");
        double values[] = new double[list.length];
        for (int i=0;i<list.length;i++) {
            try {
              values[i]=Double.parseDouble(list[i]);
            } catch(Exception e) {throw new ExecutionError("Not a valid numeric value: "+list[i]);}
        }
        return values;
    }

    private static double[] parseMarkovModelParameters_OligosAndMatrix(String text) throws ExecutionError {
        String[] list=text.split("\\s*,\\s*");
        int matrixSize=list.length;
        double log4=Math.log(matrixSize)/Math.log(4);
        int order=(int)log4;
        if (order!=log4 && matrixSize!=0) throw new ExecutionError("Number of values in Background Model oligo frequencies or transition matrix must be a power of 4");
        double values[] = new double[list.length];
        for (int i=0;i<list.length;i++) {
            try {
              values[i]=Double.parseDouble(list[i]);
            } catch(Exception e) {throw new ExecutionError("Not a valid numeric value: "+list[i]);}
        }
        return values;
    }

    /** Creates a new Background model based on oligo-frequencies in a DNA track     
     *  @param orderString A String which can either contain a literal number or be the name of a Numeric Variable
     */
    public static BackgroundModel createModelFromDNATrack(DNASequenceDataset dnatrack, SequenceCollection collection, String orderString, boolean relativeOrientation, MotifLabEngine engine, ExecutableTask task) throws ExecutionError,InterruptedException {
        Data orderData=engine.getDataItem(orderString);
        if (orderData instanceof NumericVariable) {
            int useorder=((NumericVariable)orderData).getValue().intValue();
            if (useorder<0 || useorder>5) throw new ExecutionError("The background model order must be a number between 0 and 5. '"+orderString+"' has value "+((NumericVariable)orderData).getValue());
            return BackgroundModel.createModelFromDNATrack(dnatrack, collection, useorder, relativeOrientation, task);
        } else if (orderData!=null) throw new ExecutionError("The 'order' of the background model must refer to a literal number [0-5] or a Numeric Variable");
        try {
            int useorder=Integer.parseInt(orderString);
            return BackgroundModel.createModelFromDNATrack(dnatrack, collection, useorder, relativeOrientation, task);
        } catch (NumberFormatException nfe) {
            throw new ExecutionError("The 'order' of the background model must refer to a literal number [0-5] or a Numeric Variable");
        }
    }
        
    /** Creates a new Background model based on oligo-frequencies in a DNA track */
    public static BackgroundModel createModelFromDNATrack(DNASequenceDataset dnatrack, SequenceCollection collection, int order, boolean relativeOrientation, ExecutableTask task) throws ExecutionError,InterruptedException {
        if (order<0 || order>5) throw new ExecutionError("Model order must be in the range 0 to 5");
        if (task!=null) task.setProgress(1);
        int size=0;
        for (int i=0;i<=order;i++) size+=Math.pow(4, i+1);
        double[] frequencies=new double[size];
        int blockstart=0;
        int oligoCountProgressRange=95; // 
        for (int i=0;i<=order;i++) {
            if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();
            int oligosize=i+1;
            int lowPercentageRange=(int)(1.0/(order+1.0)*i*oligoCountProgressRange);
            int highPercentageRange=(int)(1.0/(order+1.0)*(i+1)*oligoCountProgressRange);
            double[] freqs=countOligoFrequencyInDataset(dnatrack, collection, oligosize, relativeOrientation, task, lowPercentageRange, highPercentageRange); // the two int's low/highlowPercentageRange are numbers between 0 and 100 which denote the relative progress range to be used by the method
            System.arraycopy(freqs, 0, frequencies, blockstart, freqs.length);
            blockstart+=Math.pow(4, i+1);
        }
        if (Thread.interrupted()) throw new InterruptedException();
        if (task!=null) {
            if (task.isAborted()) throw new InterruptedException();
            task.setProgress(oligoCountProgressRange);
        }
        try {
            BackgroundModel model=convertFrequencyFormatToBackground(frequencies);
            model.setTrackName(dnatrack.getName());
            if (collection!=null) model.setTrackSequenceCollection(collection.getName());            
            model.setTrackFromRelativeStrand(relativeOrientation);
            if (task!=null) task.setProgress(99);
            return model;
        } catch (ParseError pe) {
            throw new ExecutionError(pe.getMessage());
        }
    }

    /**
     *
     * @param dnatrack
     * @param oligosize Size of oligos to be counted
     * @param relativeOrientation if TRUE the counts will be based on the sequence strand, if FALSE the direct strand will be used
     * @return An double[] array with (normalized) frequencies for each oligo of the specified length based on the supplied track
     */
    private static double[] countOligoFrequencyInDataset(DNASequenceDataset dnatrack, SequenceCollection collection, int oligosize, boolean relativeOrientation, ExecutableTask task, int lowRange, int highRange) throws InterruptedException {
        if (oligosize==1) return countSNFInDataset(dnatrack,collection,relativeOrientation);
        double[] frequencies=new double[(int)Math.pow(4, oligosize)];
        int size=0;
        int totalLength=0;          
        int count=0;
        ArrayList<FeatureSequenceData> sequences=getSequencesFromCollection(dnatrack.getAllSequences(),collection);
        if (task!=null) { // find total length of sequence set for progress report
            for (FeatureSequenceData seq:sequences) {
                DNASequenceData dnaseq=(DNASequenceData)seq;
                totalLength+=dnaseq.getSize(); 
            }
        }        
        int onePercent=(int)(totalLength/100.0);        
        for (FeatureSequenceData seq:sequences) {
            if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();
            DNASequenceData dnaseq=(DNASequenceData)seq;
            boolean reverse=(relativeOrientation && dnaseq.getStrandOrientation()==Sequence.REVERSE);
            int offset=dnaseq.getGenomicPositionFromRelative(0);
            for (int i=0;i<=dnaseq.getSize()-oligosize;i++) {
                char[] pattern=(char[])dnaseq.getValueInGenomicInterval(offset+i,offset+i+oligosize-1);
                if (reverse) pattern=MotifLabEngine.reverseSequence(pattern);
                int patternIndex=patternToInteger(pattern);
                if (patternIndex>=0) {
                    frequencies[patternIndex]+=1;
                    size++;
                }
                count++;
                if (task!=null) {                 
                    if (totalLength!=0 && onePercent!=0 && count%onePercent==0) {
                        double relativeProgress=(double)count/(double)totalLength;
                        int progress=(int)((highRange-lowRange)*relativeProgress+lowRange);
                        task.setProgress(progress);
                    } 
                }
            } // end for each pos
        } // end for each sequence
        for (int i=0;i<frequencies.length;i++) {
            frequencies[i]=frequencies[i]/(double)size; // normalize
        }
        return frequencies;
    }


     /** Returns the frequencies for each single nucleotide in the dna track [0]=A,[1]=C,[2]=G,[3]=T */
     private static double[] countSNFInDataset(DNASequenceDataset dnatrack, SequenceCollection collection, boolean relativeOrientation) {
        double[] frequencies=new double[4];
        int size=0;
        ArrayList<FeatureSequenceData> sequences=getSequencesFromCollection(dnatrack.getAllSequences(),collection);
        for (FeatureSequenceData seq:sequences) {
            DNASequenceData dnaseq=(DNASequenceData)seq;
            boolean reverse=(relativeOrientation && dnaseq.getStrandOrientation()==Sequence.REVERSE);
            for (int i=0;i<dnaseq.getSize();i++) {
                char c=dnaseq.getValueAtRelativePosition(i);
                switch (c) {
                   case 'A': case 'a': frequencies[(reverse)?3:0]+=1;size++; break;
                   case 'C': case 'c': frequencies[(reverse)?2:1]+=1;size++; break;
                   case 'G': case 'g': frequencies[(reverse)?1:2]+=1;size++; break;
                   case 'T': case 't': frequencies[(reverse)?0:3]+=1;size++; break;
                }
            } // end for each pos
        } // end for each sequence
        for (int i=0;i<frequencies.length;i++) {
            frequencies[i]=frequencies[i]/(double)size; // normalize
        }
        return frequencies;
    }

    private static ArrayList<FeatureSequenceData> getSequencesFromCollection(ArrayList<FeatureSequenceData> storage, SequenceCollection collection) {
        if (collection==null) return storage;
        ArrayList<FeatureSequenceData> subset=new ArrayList<FeatureSequenceData>();
        for (FeatureSequenceData seq:storage) {
            if (collection.contains(seq.getSequenceName())) subset.add(seq);
        }
        return subset;
    }      

    // ------------ Serialization ---------
    private static final long serialVersionUID = 1L; // to remain backwards compatible

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
         short currentinternalversion=1; // this is an internal version number for serialization of objects of this type
         out.writeShort(currentinternalversion);
         //out.writeObject(fromTrackSequences);
         out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
         short currentinternalversion=in.readShort(); // the internalversion number is used to determine correct format of data
         if (currentinternalversion==1) {
             in.defaultReadObject();
         } else if (currentinternalversion>1) throw new ClassNotFoundException("Newer version");
    }

}
