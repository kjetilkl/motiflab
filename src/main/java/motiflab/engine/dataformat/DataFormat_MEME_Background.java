/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.Data;
import motiflab.engine.data.BackgroundModel;
import motiflab.engine.data.OutputData;
import motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class DataFormat_MEME_Background extends DataFormat {
    private String name="MEME_Background";
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
        return "freq";
    } 
    
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) target=new BackgroundModel("Background");
        if (input.isEmpty()) throw new ParseError("Empty input document");
        if (input.size()<=1) throw new ParseError("No background model data in input document");
        Iterator iter=input.iterator();
        int order=-1;
        HashMap<String,Double> probabilities=new HashMap<String,Double>();
        while (iter.hasNext()) { // remove blank lines
            String next=((String)iter.next()).trim();
            if (next.isEmpty() || next.startsWith("#")) continue;
            String[] parts=next.split("\\s");
            if (parts.length!=2) throw new ParseError("Expected oligo followed by probability value.\nFound '"+next+"'");
            String oligo=parts[0].toUpperCase();
            if (!isValidOligo(oligo)) throw new ParseError("Invalid DNA oligo encountered: '"+oligo+"'");
            try {
               double value=Double.parseDouble(parts[1]);
               probabilities.put(oligo, value);
               int eorder=oligo.length()-1;
               if (eorder>order) order=eorder;
            } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: '"+parts[1]+"'");}
        }
        // now check that all oligos are present! 
        int index=0;
        String missingText="";
        int missing=0;
        int tablesize=0;
        for (int size=1;size<=order+1;size++) {
             tablesize+=Math.pow(4,size);
        }
        double[] table=new double[tablesize];
        for (int size=1;size<=order+1;size++) {
            for (int i=0;i<Math.pow(4,size);i++) {
                String oligo=BackgroundModel.integerToPattern(i, size); 
                if (!probabilities.containsKey(oligo)) {
                    missing++; 
                    if (missing<5) {
                        if (missingText.isEmpty()) missingText=oligo; else missingText+=","+oligo;
                    }
                }
                else table[index+i]=probabilities.get(oligo);
            }            
            index+=Math.pow(4,size);
        }          
        if (missing>0) {
            String exceptionString="Missing probabilities for "+missing+" oligos ["+missingText;
            if (missing>=5) exceptionString+="...]";
            else exceptionString+="]";
            throw new ParseError(exceptionString);
        }
        BackgroundModel model=BackgroundModel.convertFrequencyFormatToBackground(table);
        model.rename(target.getName()); // the new model returned from the conversion above is called "temporary"
        target.importData(model); 
        return target;
    }

    /** Checks if the argument string is a valid DNA oligo (containing only A,C,G and T) 
     *  oligos should be in uppercase!
     */
    private boolean isValidOligo(String oligo) {
        for (int i=0;i<oligo.length();i++) {
            char c=oligo.charAt(i);
            if (!(c=='A' || c=='C' || c=='G' || c=='T')) return false;
        }
        return true;
    }

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(10); // just to get a progress value going...
        BackgroundModel model=(BackgroundModel)dataobject;
        double[] frequencies=BackgroundModel.convertBackgroundToFrequencyFormat(model);
        StringBuilder outputString=new StringBuilder();
        int index=0;
        int order=model.getOrder();
        for (int size=1;size<=order+1;size++) {
            for (int i=0;i<Math.pow(4,size);i++) {
                String oligo=BackgroundModel.integerToPattern(i, size); 
                outputString.append(oligo.toLowerCase());
                outputString.append("\t");
                outputString.append(frequencies[index+i]);
                outputString.append("\n");
            }            
            index+=Math.pow(4,size);
        }        
        outputobject.append(outputString.toString(),getName());
        setProgress(100);
        return outputobject;   
    }

}
