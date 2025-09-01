/*
 
 
 */

package org.motiflab.engine.dataformat;

import java.util.ArrayList;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Organism;
import org.motiflab.engine.data.BackgroundModel;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class DataFormat_INCLUSive_Background extends DataFormat {
    private String name="INCLUSive_Background_Model";
    private Class[] supportedTypes=new Class[]{BackgroundModel.class};

        
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return data instanceof BackgroundModel;
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return dataclass.equals(BackgroundModel.class);
    }
    
    @Override
    public boolean canParseInput(Data data) {
        return data instanceof BackgroundModel;
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return dataclass.equals(BackgroundModel.class);
    }
    
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "bg";
    } 
    
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        ArrayList<String> snfSegment=new ArrayList<String>();
        ArrayList<String> oligoSegment=new ArrayList<String>();
        ArrayList<String> matrixSegment=new ArrayList<String>();
        ArrayList<String> current=null;
        double[] newSNF=null;
        double[] newOligoFrequencies=null;
        double[] newMatrix=null;
        int order=-1;
        int organism=0;
        if (target==null) target=new BackgroundModel("Background");
        if (input.isEmpty()) throw new ParseError("Empty input document");
        if (!input.get(0).startsWith("#INCLUSive Background Model")) throw new ParseError("Unknown header for INCLUSive Background Model format: "+input.get(0),1);
        if (input.size()<=1) throw new ParseError("No background model data in input document");
        for (int i=1;i<input.size();i++) {
            int lineNumber=i+1;
            String line=input.get(i).trim().toLowerCase();
                 if (line.isEmpty()) continue;
            else if (line.startsWith("#snf")) current=snfSegment;
            else if (line.startsWith("#oligo frequency")) current=oligoSegment;
            else if (line.startsWith("#transition matrix")) current=matrixSegment;
            else if (line.startsWith("#order")) {
                String[] split=line.split("=");               
                try {
                   String orderText=split[1].trim();
                   order=Integer.parseInt(orderText);
                } catch (Exception e) {throw new ParseError("Unable to parse expected numeric value for Model order: "+line, lineNumber);}
            }
            else if (line.startsWith("#organism")) {
                String[] split=line.split("=");
                if (split.length>1) {
                    String organismText=split[1].trim();
                    organism=Organism.getTaxonomyID(organismText);
                }
            }
            else if (!line.startsWith("#")) {
                if (current==null) throw new ParseError("Unexpected line in input: "+line, lineNumber);
                current.add(line);
            }           
        }
        if (order==-1) throw new ParseError("Missing specification of background order in input");
        if (snfSegment.size()!=1) throw new ParseError("Expected 1 line of data for Single Nucleotide Frequencies. Got "+snfSegment.size());
        newSNF=parseValueLines(snfSegment,4,"Single Nucleotide Frequencies");
        int oligorows=4;
        if (order>1) oligorows=(int)Math.pow(4,order);
        if (oligoSegment.size()!=oligorows) throw new ParseError("Expected "+oligorows+" lines of data for Oligo Frequencies. Got "+oligoSegment.size());
        newOligoFrequencies=parseValueLines(oligoSegment,1,"Oligo Frequencies");
        if (matrixSegment.size()!=oligorows) throw new ParseError("Expected "+oligorows+" lines of data for Transition Matrix. Got "+matrixSegment.size());
        if (order==0) { // just a hack
            String line=matrixSegment.get(0);
            matrixSegment.clear();
            matrixSegment.add(line);
        }
        newMatrix=parseValueLines(matrixSegment, 4, "Transition Matrix");        
        ((BackgroundModel)target).setValue(newSNF, newOligoFrequencies, newMatrix); // this will also update the order!
        if (organism!=0) ((BackgroundModel)target).setOrganism(organism);
        return target;
    }

    /** */
    private double[] parseValueLines(ArrayList<String> list, int columns, String part) throws ParseError {
        double[] result=new double[list.size()*columns];
        for (int i=0;i<list.size();i++) {
            String line=list.get(i);
            String[] split=line.split("\\s+");
            if (split.length!=columns) throw new ParseError("Expected "+columns+" columns of data for "+part+". Got "+split.length);
            for (int j=0;j<columns;j++) {
                try {
                    result[i*columns+j]=Double.parseDouble(split[j]);
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value:" +split[j]);}
            }
        }
        return result;
    }
    
    

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(5);
        BackgroundModel model=(BackgroundModel)dataobject;
        String organismString="";
        if (model.getOrganism()!=0) organismString=Organism.getCommonName(model.getOrganism());
        StringBuilder outputString=new StringBuilder("#INCLUSive Background Model v1.0\n#\n#Order = "+model.getOrder()+"\n#Organism = "+organismString+"\n#Sequences = \n#Path =\n#\n\n");
        outputString.append("#snf\n");outputString.append(model.getSNF('A'));outputString.append("\t");outputString.append(model.getSNF('C'));outputString.append("\t");outputString.append(model.getSNF('G'));outputString.append("\t");outputString.append(model.getSNF('T'));outputString.append("\n\n");
        String oligoFreqAsString=model.getOligoFrequenciesAsString().replace(",", "\n");
        outputString.append("#oligo frequency\n");
        outputString.append(oligoFreqAsString);
        outputString.append("\n\n");
        outputString.append("#transition matrix\n");
        Double[][] matrix=model.getTransitionMatrix();
        if (model.getOrder()==0) {
               Double[] line=matrix[0];
               outputString.append(line[0]);outputString.append("\t");outputString.append(line[1]);outputString.append("\t");outputString.append(line[2]);outputString.append("\t");outputString.append(line[3]);outputString.append("\n");
               outputString.append(line[0]);outputString.append("\t");outputString.append(line[1]);outputString.append("\t");outputString.append(line[2]);outputString.append("\t");outputString.append(line[3]);outputString.append("\n");
               outputString.append(line[0]);outputString.append("\t");outputString.append(line[1]);outputString.append("\t");outputString.append(line[2]);outputString.append("\t");outputString.append(line[3]);outputString.append("\n");
               outputString.append(line[0]);outputString.append("\t");outputString.append(line[1]);outputString.append("\t");outputString.append(line[2]);outputString.append("\t");outputString.append(line[3]);outputString.append("\n");
        } else {
            for (int i=0;i<matrix.length;i++) {
               Double[] line=matrix[i];
               outputString.append(line[0]);outputString.append("\t");outputString.append(line[1]);outputString.append("\t");outputString.append(line[2]);outputString.append("\t");outputString.append(line[3]);outputString.append("\n");
            }
        }
        outputobject.append(outputString.toString(),getName());
        setProgress(100);
        return outputobject;   
    }

}
