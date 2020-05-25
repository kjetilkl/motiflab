/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;
import motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class DataFormat_MotifLabMotif extends DataFormat {
    private String name="MotifLabMotif";
    
    private Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class};

    
    public DataFormat_MotifLabMotif() {
        addOptionalParameter("Include non-standard fields", Boolean.TRUE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Include non-standard fields that have been added by a user");
        addOptionalParameter("Include derived fields", Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Include fields that can be derived from others, such as e.g. Consensus, IC-content and GC-content");
        addOptionalParameter("Include color info", Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Include information about the colors assigned to each motif");
        addOptionalParameter("Register identifier definitions", Boolean.TRUE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Register information about new identifier types that may be included in Motif Collections.");
        setParameterFilter("Include derived fields","output"); 
        setParameterFilter("Include non-standard fields","output");         
        setParameterFilter("Include color info","output");  
        setParameterFilter("Register identifier definitions","input");           
    }
        
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof MotifCollection || data instanceof Motif);
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(MotifCollection.class) || dataclass.equals(Motif.class));
    }
    
    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof MotifCollection || data instanceof Motif);
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(MotifCollection.class) || dataclass.equals(Motif.class));
    }
    
      
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }
    
    @Override
    public String getSuffix() {
        return "mlx";
    }
 
    @Override
    public String[] getSuffixAlternatives() {return new String[]{"mlx","pmx"};}    
    
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);

        boolean includeDerived=false;
        boolean includeUserDefined=true;
        boolean includeColorInfo=false;      
        
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             includeDerived=(Boolean)settings.getResolvedParameter("Include derived fields",defaults,engine); 
             includeUserDefined=(Boolean)settings.getResolvedParameter("Include non-standard fields",defaults,engine);
             includeColorInfo=(Boolean)settings.getResolvedParameter("Include color info",defaults,engine);           
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           includeDerived=(Boolean)getDefaultValueForParameter("Include derived fields");
           includeUserDefined=(Boolean)getDefaultValueForParameter("Include non-standard fields");
           includeColorInfo=(Boolean)getDefaultValueForParameter("Include color info");                     
        }        
              
        StringBuilder outputString=new StringBuilder("#MotifLabMotif   (inspired by INCLUSive Motif Model v1.0)\n");
        if (dataobject instanceof MotifCollection) {
            if ( ((MotifCollection)dataobject).isPredefined()) {
                outputString.append("#Collection=");
                outputString.append(((MotifCollection)dataobject).getPredefinedCollectionName());
                outputString.append("\n");
            }
            outputString.append("#\n");
            ArrayList<Motif> motiflist=((MotifCollection)dataobject).getAllMotifs(engine);
            int size=motiflist.size();
            int i=0;
            for (Motif motif:motiflist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                outputMotif(motif, outputString, includeDerived, includeUserDefined, includeColorInfo);
                // task.setStatusMessage("Motif "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%100==0) Thread.yield();
            } 
        } else if (dataobject instanceof Motif){
            outputString.append("#\n");
            outputMotif((Motif)dataobject,outputString, includeDerived, includeUserDefined, includeColorInfo);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }    
    
    
    /** outputformats a single motif */
    protected void outputMotif(Motif motif, StringBuilder outputString, boolean includeDerived, boolean includeUserDefined, boolean includeColorInfo) {
            double[][] matrix=motif.getMatrix();
            if (matrix==null) return;
            outputString.append("#ID = ");outputString.append(motif.getName());outputString.append("\n");
            if (motif.getShortName()!=null) {outputString.append("#Short = ");outputString.append(motif.getShortName());outputString.append("\n");}
            if (motif.getLongName()!=null) {outputString.append("#Long = ");outputString.append(motif.getLongName());outputString.append("\n");}
            outputString.append("#W = ");outputString.append(motif.getLength());outputString.append("\n");
            if (includeDerived && motif.getConsensusMotif()!=null) {outputString.append("#Consensus = ");outputString.append(motif.getConsensusMotif());outputString.append("\n");}
            if (motif.getClassification()!=null) {outputString.append("#Class = ");outputString.append(motif.getClassification());outputString.append("\n");}
            if (includeDerived) {outputString.append("#IC = ");outputString.append(motif.getICcontent());outputString.append("\n");}
            if (includeDerived) {outputString.append("#GC-content = ");outputString.append(motif.getGCcontent());outputString.append("\n");}
            if (motif.getQuality()!=6) {outputString.append("#Quality = ");outputString.append(motif.getQuality());outputString.append("\n");}
            if (motif.getBindingFactors()!=null) {outputString.append("#Factors = ");outputString.append(motif.getBindingFactors());outputString.append("\n");}
            if (motif.getOrganisms()!=null) {outputString.append("#Organisms = ");outputString.append(motif.getOrganisms());outputString.append("\n");}
            ArrayList<String> expression=motif.getTissueExpressionAsStringArray();
            if (expression!=null && !expression.isEmpty()) {
                outputString.append("#Expression = ");
                outputArrayList(expression,outputString);
                outputString.append("\n");
            }
            ArrayList<String> goTerms=motif.getGOterms();
            if (goTerms!=null && !goTerms.isEmpty()) {
               outputString.append("#GO = ");
               outputArrayList(goTerms,outputString);
               outputString.append("\n");
            }
            String part=motif.getPart();
            if (part!=null && !part.equals(Motif.FULL)) {outputString.append("#Part = ");outputString.append(part);outputString.append("\n");}
            ArrayList<String> interactionPartners=motif.getInteractionPartnerNames();
            if (interactionPartners!=null && !interactionPartners.isEmpty()) {
                outputString.append("#Interactions = ");
                outputArrayList(interactionPartners,outputString);
                outputString.append("\n");
            }
            ArrayList<String> duplicates=motif.getKnownDuplicatesNames();
            if (duplicates!=null && !duplicates.isEmpty()) {
                outputString.append("#Alternatives = ");
                outputArrayList(duplicates,outputString);
                outputString.append("\n");
            }
            String description=motif.getDescription();
            if (description!=null && !description.isEmpty()) {
                description=Motif.escapePropertyText(description, false);
                outputString.append("#Description = ");
                outputString.append(description);
                outputString.append("\n");
            }            
            if (includeUserDefined) {
                Set<String> userDefined=motif.getUserDefinedProperties();
                if (userDefined!=null) {
                    for (String propertyName:userDefined) {
                        String value=(String)motif.getUserDefinedPropertyValueAsType(propertyName, String.class);
                        if (value!=null) {
                            outputString.append("#");
                            outputString.append(propertyName);
                            outputString.append(" = ");
                            outputString.append(value);
                            outputString.append("\n");
                        }
                    }
                }
            }
            if (includeColorInfo) {
                java.awt.Color color=engine.getClient().getVisualizationSettings().getFeatureColor(motif.getName());
                if (color!=null) {
                    String colorString=VisualizationSettings.convertColorToHTMLrepresentation(color);
                    outputString.append("#$color=");
                    outputString.append(colorString);
                    outputString.append("\n");                   
                }
            }
            for (double[] row:matrix) {
              outputString.append(row[0]);
              outputString.append("\t");
              outputString.append(row[1]);
              outputString.append("\t");
              outputString.append(row[2]);
              outputString.append("\t");
              outputString.append(row[3]);
              outputString.append("\n");
            }
            outputString.append("\n");
    }

    /** Outputs an ArrayList to a StringBuilder buffer as a list of comma-separated values*/
    private void outputArrayList(ArrayList list, StringBuilder outputString) {
        Iterator i = list.iterator();
	if (!i.hasNext()) return;
	for (;;) {
	    Object e = i.next();
	    outputString.append(e.toString());
	    if (!i.hasNext()) return;
	    outputString.append(",");
	}
    }

    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_MotifLabMotif.parseInput(ArrayList<String> input, Data target)");
             if (target instanceof MotifCollection) return parseMotifCollection(input, (MotifCollection)target, settings, task);
        else if (target instanceof Motif) return parseMotif(input, (Motif)target, true);
        else throw new ParseError("Unable to parse Motif input to target data of type "+target.getTypeDescription());
    }
    

    protected Motif parseMotif(List<String> input, Motif target, boolean redraw) throws ParseError {
       int expectedsize=0;
       String consensus="";
       String motifID="unknown";
       String motifShortName=null;
       String motifLongName=null;
       int quality=6;
       String classification=null;
       String description=null;
       String factors=null;
       String organisms=null;
       String part=null;
       String[] partners=null;
       String[] alternatives=null;
       String[] expression=null;
       String[] goterms=null;
       if (target==null) target=new Motif("unknown");
       ArrayList<String>matrixlines=new ArrayList<String>();
       for (String line:input) {
           line=line.trim();
           if (line.isEmpty() || line.startsWith("##")) continue; // a single # denotes a motif property (key=value pair) but a double # is treated as a comment
           else if (line.startsWith("#ID")) {
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2) {motifID=split[1];}
           } else if (line.startsWith("#W")) {
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2) {try {expectedsize=Integer.parseInt(split[1]);} catch(Exception e){ throw new ParseError("Unable to parse expected numeric value for matrix-length for motif '"+motifID+"':"+line);}}               
           } else if (line.startsWith("#Quality")) {
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2) {try {quality=Integer.parseInt(split[1]);} catch(Exception e){ throw new ParseError("Unable to parse expected numeric value for matrix-quality for motif '"+motifID+"':"+line);}}              
           } else if (line.startsWith("#Consensus") || line.startsWith("#Conensus")) { //#conensus is a common misspelling
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2) {consensus=split[1];}
           } else if (line.startsWith("#Description")) { //#conensus is a common misspelling
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2) {
                    description=split[1];
                    description=Motif.unescapePropertyText(description, false);
               }
           } else if (line.startsWith("#IC") || line.startsWith("#IC-content") || line.startsWith("#GC-content")) {
               continue; // skip these derived values
           } else if (line.startsWith("#Short")) {
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2) {motifShortName=split[1];}
           } else if (line.startsWith("#Long")) {
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2) {motifLongName=split[1];}
           } else if (line.startsWith("#Class")) {
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2) {
                   String[] splitSemiColon=split[1].split("\\s*;\\s*");
                   classification=splitSemiColon[0];
               }
           } else if (line.startsWith("#Part")) {
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2) {part=split[1];}
           } else if (line.startsWith("#Factors")) {
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2) {factors=split[1];}
           } else if (line.startsWith("#Organisms")) {
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2) {organisms=split[1];}
           } else if (line.startsWith("#Partners") || line.startsWith("#Interactions")) {
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2 && !split[1].trim().isEmpty()) {partners=split[1].split("\\s*,\\s*");}
           } else if (line.startsWith("#Alternatives") || line.startsWith("#Duplicates")) {
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2 && !split[1].trim().isEmpty()) {alternatives=split[1].split("\\s*,\\s*");}
           } else if (line.startsWith("#Expression")) {
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2 && !split[1].trim().isEmpty()) {expression=split[1].split("\\s*,\\s*");}
           } else if (line.startsWith("#GO")) {
               String[] split=line.split("\\s*=\\s*",2);
               if (split.length==2 && !split[1].trim().isEmpty()) {goterms=split[1].split("\\s*,\\s*");}
           } else if (line.startsWith("#Halfsite")) {
               part=Motif.HALFSITE;
           } else if (line.startsWith("#$color")) {
               String[] split=line.split("\\s*=\\s*",2);
               java.awt.Color color=VisualizationSettings.convertHTMLrepresentationToColor(split[1]);
               if (color!=null) engine.getClient().getVisualizationSettings().setFeatureColor(motifID, color, redraw);
           } else if (line.startsWith("#") && line.contains("=")) { // unknown property (i.e. user-defined, non-standard)
                String[] split=line.split("\\s*=\\s*",2);
                String key=split[0].substring(1);
                if (!Motif.isValidUserDefinedPropertyKey(key)) throw new ParseError("Not a valid property name: '"+key+"' for motif '"+motifID+"'");
                String valuestring=split[1].trim();
                if (valuestring.endsWith(";")) valuestring=valuestring.substring(0,valuestring.length()-1); // allow one ';' at the end but remove it if present
                if (valuestring.contains(";")) throw new ParseError("Value for property '"+key+"' for motif '"+motifID+"' contains illegal character ';'");
                Object value=Motif.getObjectForPropertyValueString(valuestring);
                if (value!=null) target.setUserDefinedPropertyValue(key, value);
           } else if (line.startsWith("#")) {
               continue;
           } else matrixlines.add(line);
       }
       if (expectedsize>0 && matrixlines.size()!=expectedsize) throw new ParseError("Expected "+expectedsize+" matrix rows in Motif input for motif '"+motifID+"'. Got "+matrixlines.size());
       double[][] matrix=parseValueLines(matrixlines, motifID);
       if (consensus!=null) target.setConsensusMotif(consensus);
       else target.setConsensusMotif(Motif.getConsensusForMatrix(matrix));
       target.setName(motifID);
       target.setShortName(motifShortName);
       target.setLongName(motifLongName);
       target.setDescription(description);
       target.setQuality(quality);
       try {target.setClassification(classification);} catch (ExecutionError e) {throw new ParseError(e.getMessage());}
       target.setPart(part);
       target.setBindingFactors(factors);
       target.setOrganisms(organisms);
       target.setTissueExpression(expression);
       target.setKnownDuplicatesNames(alternatives);
       target.setInteractionPartnerNames(partners);
       target.setGOterms(goterms);
       target.setMatrix(matrix);
       return target;
    } 
                        
    
    protected MotifCollection parseMotifCollection(List<String> input, MotifCollection target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        boolean includeTypeDefinitions=false;      
        
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             includeTypeDefinitions=(Boolean)settings.getResolvedParameter("Register identifier definitions",defaults,engine);          
          } catch (Exception e) {
             throw new ParseError("An error occurred during output formatting"+e.getMessage());
          }
        } else {
           includeTypeDefinitions=(Boolean)getDefaultValueForParameter("Register identifier definitions");                   
        }
        
        if (target==null) target=new MotifCollection("MotifCollection");
        int first=0; int last=0; int size=input.size();
        if (size<1) return target; //throw new ParseError("Empty input for MotifCollection");
        if (!(input.get(0).startsWith("#MotifLabMotif") || input.get(0).startsWith("#PriorsEditorMotif"))) throw new ParseError("Unrecognized header for MotifLab Motif format: "+input.get(0), 1);
        int count=0;
        for (int i=1;i<size;i++) { // 
            String line=input.get(i).trim();
            if (line.startsWith("#ID")) {
                if (task!=null) task.setProgress(i, size);
                count++;
                if (count%30==0) {
                  if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                  if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
                  Thread.yield();
                  setProgress(i, size);
                }                 
                if (first==0) {
                    first=i;
                } else {
                    last=i;
                    Motif motif=parseMotif(input.subList(first, last), null, false);
                    target.addMotifToPayload(motif);
                    first=i;
                    last=0;
                }
            } else if (count==0 && line.startsWith("#") && line.contains("=")) { // comments before the first motif can contain meta-data on the form "<key> = <value>"
                String[] parts=line.substring(1).trim().split("\\s*=\\s*",2);
                String lowercaseKey=parts[0].toLowerCase();
                if (includeTypeDefinitions && parts.length==2 && (lowercaseKey.startsWith("identifier(") || lowercaseKey.startsWith("identifier[")) && (lowercaseKey.endsWith(")") || lowercaseKey.endsWith("]"))) {
                    String identifier=parts[0].substring("identifier(".length(),parts[0].length()-1);
                    String definition=parts[1];
                    // register new motif identifier with the engine. Overwrite existing identifier and save configuration to disc
                    try {
                        String oldValue=engine.getGeneIDResolver().addOtherIdentifier(identifier, definition, true, true); // save the new identifiers to the configuration
                        if (oldValue==null || (oldValue!=null && !oldValue.equals(definition))) engine.logMessage("New identifier type registered: "+identifier);
                        if (oldValue!=null && !oldValue.equals(definition)) {
                            engine.logMessage(" - Replaced value for identifier: "+identifier);
                            engine.logMessage("   Old value: "+oldValue);
                            engine.logMessage("   New value: "+definition);                            
                        }
                    } catch (ParseError pe) {
                        //  the existing identifier was not replaced
                    } catch (Exception e) {
                         engine.logMessage("An error occurred while saving configuration to disc: "+e.toString());
                    }
                }
            }
        }
        if (first>0) { // parse last motif
            Motif motif=parseMotif(input.subList(first, size), null, false);
            target.addMotifToPayload(motif);
        }
        engine.getClient().getVisualizationSettings().redraw(); // in case colors have been updated!
        return target;
    }
            
            
    
    /** */
    private double[][] parseValueLines(List<String> list, String motifID) throws ParseError {
        if (list.isEmpty()) throw new ParseError("Missing matrix specification values for matrix: "+motifID);
        double[][] result=new double[list.size()][4];
        for (int i=0;i<list.size();i++) {
            String line=list.get(i);
            String[] split=line.split("\\s+");
            if (split.length!=4) throw new ParseError("Expected 4 columns in data matrix '"+motifID+"'. Got "+split.length);
            for (int j=0;j<4;j++) {
                try {
                    result[i][j]=Double.parseDouble(split[j]);
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value in matrix for '"+motifID+"' = " +split[j]);}
            }
        }
        return result;
    }    
    
}

        
       
        
        
