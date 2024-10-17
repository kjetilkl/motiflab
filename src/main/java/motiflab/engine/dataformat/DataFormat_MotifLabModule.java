/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.HashMap;
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
import motiflab.engine.data.ModuleCRM;
import motiflab.engine.data.ModuleCollection;
import motiflab.engine.data.ModuleMotif;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;
import motiflab.gui.VisualizationSettings;

/**
 * Note: the class extends DataFormat_MotifLabMotif in order to be able to reuse code
 *       concerning input/output of single motifs that could be present also
 * @author kjetikl
 */
public class DataFormat_MotifLabModule extends DataFormat { 
    private String name="MotifLabModule";
    
    private Class[] supportedTypes=new Class[]{ModuleCollection.class, ModuleCRM.class};

    
    public DataFormat_MotifLabModule() {
        addOptionalParameter("Include single motifs", Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Include single motifs used by the module(s)");
        addOptionalParameter("Include non-standard fields", Boolean.TRUE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Include non-standard fields that have been added by a user");              
        addOptionalParameter("Include module color info", Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Include information about the colors assigned to each module");
        setParameterFilter("Include single motifs","output");         
        setParameterFilter("Include module color info","output");      
    }
        
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof ModuleCollection || data instanceof ModuleCRM);
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(ModuleCollection.class) || dataclass.equals(ModuleCRM.class));
    }
    
    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof ModuleCollection || data instanceof ModuleCRM);
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(ModuleCollection.class) || dataclass.equals(ModuleCRM.class));
    }
    
      
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }
    
    @Override
    public String getSuffix() {
        return "mod";
    }
 
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);
        boolean includeSingleMotifs=false;
        boolean includeColorInfo=false;    
        boolean includeUserDefined=true;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             includeSingleMotifs=(Boolean)settings.getResolvedParameter("Include single motifs",defaults,engine);             
             includeUserDefined=(Boolean)getDefaultValueForParameter("Include non-standard fields");
             includeColorInfo=(Boolean)settings.getResolvedParameter("Include module color info",defaults,engine);
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           includeSingleMotifs=(Boolean)getDefaultValueForParameter("Include single motifs");
           includeColorInfo=(Boolean)getDefaultValueForParameter("Include module color info");           
           includeUserDefined=(Boolean)getDefaultValueForParameter("Include non-standard fields");             
        }        
                
        StringBuilder outputString=new StringBuilder("#MotifLabModule\n");
        outputString.append("#\n");
        ArrayList<String> singlemotifs=(includeSingleMotifs)?new ArrayList<String>():null;
        if (dataobject instanceof ModuleCollection) {
            ArrayList<ModuleCRM> modulelist=((ModuleCollection)dataobject).getAllModules(engine);
            int size=modulelist.size();
            int i=0;
            for (ModuleCRM cisRegModule:modulelist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                outputModule(cisRegModule, outputString,includeUserDefined, includeColorInfo);
                if (includeSingleMotifs) {
                    ArrayList<ModuleMotif> mmlist=cisRegModule.getModuleMotifs();
                    for (ModuleMotif mm:mmlist) mergeMotifsList(singlemotifs,mm.getMotifAsCollection());
                }
                // task.setStatusMessage("Motif "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%100==0) Thread.yield();
            } 
        } else if (dataobject instanceof ModuleCRM){
            outputModule((ModuleCRM)dataobject,outputString, includeUserDefined, includeColorInfo);
            if (includeSingleMotifs) {
                ArrayList<ModuleMotif> mmlist=((ModuleCRM)dataobject).getModuleMotifs();
                for (ModuleMotif mm:mmlist) mergeMotifsList(singlemotifs,mm.getMotifAsCollection());
            }
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        if (singlemotifs!=null && !singlemotifs.isEmpty()) { // output single motifs
            DataFormat_MotifLabMotif motiflabmotifformat=(DataFormat_MotifLabMotif)engine.getDataFormat("MotifLabMotif");
            for (String motifname:singlemotifs) {
                Data motif=engine.getDataItem(motifname);
                if (motif instanceof Motif) motiflabmotifformat.outputMotif((Motif)motif, outputString, false, true, false); // note: color is not set for single motifs (only "modulemotifs" and the module itself), hence the last "false"
            }
        }
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }    

    private void mergeMotifsList(ArrayList<String> list, MotifCollection col) {
        for (String motifname:col.getAllMotifNames()) {
            if (!list.contains(motifname)) list.add(motifname);
        }
    }
    
    /** outputformats a single module */
    private void outputModule(ModuleCRM cisRegModule, StringBuilder outputString, boolean includeUserDefined, boolean includeColorInfo) {
            outputString.append("#ModuleID = ");outputString.append(cisRegModule.getName());outputString.append("\n");
            ArrayList<ModuleMotif> motiflist=cisRegModule.getModuleMotifs();
            outputString.append("Motifs = ");
            for (int i=0;i<motiflist.size();i++) {
                if (i>0) outputString.append(",");
                outputString.append(motiflist.get(i).getRepresentativeName());
            }
            outputString.append("\n");
            outputString.append("Ordered = ");
            outputString.append(cisRegModule.isOrdered());
            outputString.append("\n");
            if (cisRegModule.getMaxLength()>0) {
                outputString.append("MaxLength = ");
                outputString.append(cisRegModule.getMaxLength());
                outputString.append("\n");
            }
            for (int i=0;i<motiflist.size();i++) {
                outputString.append("Motif(");
                outputString.append(motiflist.get(i).getRepresentativeName());
                outputString.append(") = ");
                MotifCollection col=motiflist.get(i).getMotifAsCollection();
                if (col!=null) outputString.append(col.getValueAsParameterString());
                outputString.append("\n");
            }
            for (int i=0;i<motiflist.size();i++) {
                int orientation=motiflist.get(i).getOrientation();
                if (orientation==ModuleCRM.DIRECT) {
                    outputString.append("Orientation(");
                    outputString.append(motiflist.get(i).getRepresentativeName());
                    outputString.append(") = Direct");
                    outputString.append("\n");
                } else if (orientation==ModuleCRM.REVERSE) {
                    outputString.append("Orientation(");
                    outputString.append(motiflist.get(i).getRepresentativeName());
                    outputString.append(") = Reverse");
                    outputString.append("\n");
                }
            }
            for (int[] constraint:cisRegModule.getDistanceConstraints()) {
                 String max=(constraint[3]==Integer.MAX_VALUE)?"*":""+constraint[3];
                 String min=(constraint[2]==Integer.MIN_VALUE)?"*":""+constraint[2];
                 String motif1=cisRegModule.getSingleMotifName(constraint[0]);
                 String motif2=cisRegModule.getSingleMotifName(constraint[1]);
                 outputString.append("Distance(");
                 outputString.append(motif1);
                 outputString.append(",");
                 outputString.append(motif2);
                 outputString.append(") = [");
                 outputString.append(min);
                 outputString.append(",");
                 outputString.append(max);
                 outputString.append("]\n");
            }
            ArrayList<String> goTerms=cisRegModule.getGOterms();
            if (goTerms!=null && !goTerms.isEmpty()) {
               outputString.append("GO = ");
               outputArrayList(goTerms,outputString);
               outputString.append("\n");
            } 
            if (includeUserDefined) {
                Set<String> userDefined=cisRegModule.getUserDefinedProperties();
                if (userDefined!=null) {
                    for (String propertyName:userDefined) {
                        String value=(String)cisRegModule.getUserDefinedPropertyValueAsType(propertyName, String.class);
                        if (value!=null) {
                            // outputString.append("#");  // leading # is optional
                            outputString.append(propertyName);
                            outputString.append(" = ");
                            outputString.append(value);
                            outputString.append("\n");
                        }
                    }
                }
            }            
            if (includeColorInfo) {
                VisualizationSettings settings=engine.getClient().getVisualizationSettings();
                java.awt.Color color=settings.getFeatureColor(cisRegModule.getName());
                if (color!=null) {
                    String colorString=VisualizationSettings.convertColorToHTMLrepresentation(color);
                    outputString.append("$color=");
                    outputString.append(colorString);
                    outputString.append("\n");                   
                }                
                for (int i=0;i<motiflist.size();i++) {
                    String modulemotifname=motiflist.get(i).getRepresentativeName();
                    color=settings.getFeatureColor(cisRegModule.getName()+"."+modulemotifname);
                    if (color!=null) {
                        String colorString=VisualizationSettings.convertColorToHTMLrepresentation(color);
                        outputString.append("$color(");
                        outputString.append(modulemotifname);
                        outputString.append(")=");
                        outputString.append(colorString);
                        outputString.append("\n");                   
                    }    
                }  
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
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_MotifLabModule.parseInput(ArrayList<String> input, Data target)");
             if (target instanceof ModuleCollection) return parseModuleCollection(input, (ModuleCollection)target, task);
        else if (target instanceof ModuleCRM) return parseModule(input, (ModuleCRM)target, true);
        else throw new ParseError("Unable to parse Module input to target data of type "+target.getTypeDescription());
    }
    

    private ModuleCRM parseModule(List<String> input, ModuleCRM target, boolean redraw) throws ParseError {
       String moduleID="unknown";
       String[] modulemotifnames=null;
       int maxlength=0;
       boolean ordered=false;
       HashMap<String,Integer> orientations=new HashMap<String,Integer>();
       HashMap<String,int[]> distance=new HashMap<String,int[]>(); // key is motif1+\t\motif2
       HashMap<String,String[]> singlemotifs=new HashMap<String,String[]>(); //
       String[] goterms=null;       
       int count=0;
       if (target==null) target=new ModuleCRM("unknown");
       int lineNumber=0;
       for (String line:input) {
           lineNumber++;
           line=line.trim();
           if (line.isEmpty() || line.startsWith("##") || line.startsWith("//")) continue; // a single # denotes a module property (key=value pair) but a double # is treated as a comment  (as is // )
           String[] split=line.split("\\s*=\\s*",2);
           if (split.length!=2) continue; // not a key-value pair
           String property=split[0].toLowerCase();
           //if (property.startsWith("#")) property=property.substring(1); // the leading # is optional
           String value=split[1];
           if (property.equals("moduleid") || property.equals("#moduleid")) {
                if (count>0) throw new ParseError("The file contains multiple modules. Please load it as Module Collection instead", lineNumber);
                moduleID=value;
                count++;
           } else if (property.equals("maxlength") || property.equals("max length")) {
               try {maxlength=Integer.parseInt(value);} catch(Exception e) {throw new ParseError("Unable to parse expected numeric value for MaxLength in '"+moduleID+"':"+line, lineNumber);}
           } else if (property.equals("ordered")) {
               ordered=(value.equalsIgnoreCase("TRUE") || value.equalsIgnoreCase("YES"));
           } else if (property.equals("motifs")) {
               modulemotifnames=value.trim().split("\\s*,\\s*");
           } else if (property.startsWith("motif")) {
               if (!property.matches("motif\\(\\s*\\S+\\s*\\)")) throw new ParseError("Unable to parse motif line for module '"+moduleID+"' Proper format is Motif(X)=<list of motifs>. Got: "+line, lineNumber);
               String modulemotifname=split[0].replaceAll(".*\\(\\s*","");
               modulemotifname=modulemotifname.replaceAll("\\s*\\)\\s*","");
               if (modulemotifnames==null || !arrayContains(modulemotifnames, modulemotifname)) throw new ParseError("Unknown module motif reference in line: "+line, lineNumber);
               String[] motifslist=value.split("\\s*,\\s*");
               singlemotifs.put(modulemotifname,motifslist);
           } else if (property.startsWith("orientation")) {
               if (!property.matches("orientation\\(\\s*\\S+\\s*\\)")) throw new ParseError("Unable to parse orientation line for module '"+moduleID+"' Proper format is Orientation(X)=DIRECT|REVERSE|+|-|+1|-1. Got: "+line, lineNumber);
               String modulemotifname=split[0].replaceAll(".*\\(\\s*","");
               modulemotifname=modulemotifname.replaceAll("\\s*\\)\\s*","");
               if (modulemotifnames==null || !arrayContains(modulemotifnames, modulemotifname)) throw new ParseError("Unknown module motif reference in line: "+line, lineNumber);
               if (value.equalsIgnoreCase("DIRECT") || value.equalsIgnoreCase("1") || value.equalsIgnoreCase("+1") || value.equalsIgnoreCase("+")) orientations.put(modulemotifname, ModuleCRM.DIRECT);
               else if (value.equalsIgnoreCase("REVERSE") || value.equalsIgnoreCase("-") || value.equalsIgnoreCase("-1")) orientations.put(modulemotifname, ModuleCRM.REVERSE);
           } else if (property.startsWith("distance")) {
               if (!property.matches("distance\\(\\s*\\S+\\s*,\\s*\\S+\\s*\\)")) throw new ParseError("Unable to parse distance line for module '"+moduleID+"' Proper format is Distamce(X,Y)=[min,max]. Got: "+line, lineNumber);
               String pairstring=split[0].replaceAll(".*\\(\\s*","");
               pairstring=pairstring.replaceAll("\\s*\\)\\s*","");
               String[] pair=pairstring.split("\\s*,\\s*");
               String valuepairstring=value.replaceAll(".*\\[\\s*", "");
               valuepairstring=valuepairstring.replaceAll("\\s*\\]\\s*","");
               String[] valuepair=valuepairstring.split("\\s*,\\s*");
               if (modulemotifnames==null || !arrayContains(modulemotifnames, pair[0]) || !arrayContains(modulemotifnames, pair[1])) throw new ParseError("Unknown module motif reference in line: "+line, lineNumber);
               int mindist=0;
               int maxdist=0; 
               if (valuepair[0].equalsIgnoreCase("UNLIMITED") || valuepair[0].equals("*")) mindist=Integer.MIN_VALUE;
               else try {
                   mindist=Integer.parseInt(valuepair[0]);
               } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value for min-distance: "+line, lineNumber);}
               if (valuepair[1].equalsIgnoreCase("UNLIMITED") || valuepair[1].equals("*")) maxdist=Integer.MAX_VALUE;
               else try {
                   maxdist=Integer.parseInt(valuepair[1]);
               } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value for max-distance: "+line, lineNumber);}
               distance.put(pair[0]+"\t"+pair[1], new int[]{mindist,maxdist});
           } else if (property.equals("go")) {
               goterms=value.split("\\s*,\\s*");
           } else if (property.startsWith("$color")) {
               String featureName=moduleID;
               if (property.contains("(") && property.contains(")") && property.length()>"$color()".length()) {
                   String modulemotifname=split[0].substring(property.indexOf('(')+1,property.indexOf(')')); // note that I use split[0] here instead of property since I want the original case.
                   featureName+=("."+modulemotifname);
               }
               java.awt.Color color=VisualizationSettings.convertHTMLrepresentationToColor(value);
               if (color!=null) engine.getClient().getVisualizationSettings().setFeatureColor(featureName, color, false);                            
           } else { // unknown property (i.e. user-defined, non-standard)
                if (!ModuleCRM.isValidUserDefinedPropertyKey(property)) throw new ParseError("Not a valid property name: '"+property+"' for module '"+moduleID+"'", lineNumber);
                if (value.endsWith(";")) value=value.substring(0,value.length()-1); // allow one ';' at the end but remove it if present
                if (value.contains(";")) throw new ParseError("Value for property '"+property+"' for module '"+moduleID+"' contains illegal character ';'", lineNumber);
                Object valueobject=ModuleCRM.getObjectForPropertyValueString(value);
                if (valueobject!=null) target.setUserDefinedPropertyValue(property, valueobject);
           } 
       }
       target.setName(moduleID);
       target.setMaxLength(maxlength);
       target.setOrdered(ordered);
       target.setGOterms(goterms);
       for (String modulemotifname:modulemotifnames) {
           Integer orientation=orientations.get(modulemotifname);
           MotifCollection motcol=new MotifCollection(modulemotifnames+"col");
           String[] singles=singlemotifs.get(modulemotifname);
           if (singles==null || singles.length==0) singles=new String[]{modulemotifname}; //
           for (String mname:singles) motcol.addMotifName(mname);
           target.addModuleMotif(modulemotifname, motcol, ((orientation!=null)?orientation.intValue():ModuleCRM.INDETERMINED));
       }
       for (String pair:distance.keySet()) {
           String[] motifs=pair.split("\\t");
           int[] distances=distance.get(pair);
           try {
               target.addDistanceConstraint(motifs[0], motifs[1], distances[0], distances[1]);
           } catch (ExecutionError e) {throw new ParseError(e.getMessage());}
       }
       if (redraw) engine.getClient().getVisualizationSettings().redraw();
       return target;
    } 
                        
    private boolean arrayContains(String[] array, String target) {
        for (String s:array) {
            if (s.equals(target)) return true;
        }
        return false;
    }

    private ModuleCollection parseModuleCollection(List<String> input, ModuleCollection target, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) target=new ModuleCollection("ModuleCollection");
                    DataFormat_MotifLabMotif motiflabmotifformat=(DataFormat_MotifLabMotif)engine.getDataFormat("MotifLabMotif");
        boolean ismodule=true; // true=>module, false=>single motif
        int first=0; int last=0; int size=input.size();
        if (size<1) return target; //throw new ParseError("Empty input for MotifCollection");
        if (!input.get(0).startsWith("#MotifLabModule")) throw new ParseError("Unrecognized header for MotifLab Module format: "+input.get(0));
        int count=0;
        for (int i=1;i<size;i++) {
            String line=input.get(i).trim();
            if (line.startsWith("#ID") || line.startsWith("#ModuleID") || line.startsWith("ID") || line.startsWith("ModuleID")) {
                count++;
                if (count%30==0) {
                  if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                  if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
                  Thread.yield();
                }                 
                if (first==0) {
                    first=i;
                } else { // start of new module/motif also marks end of previous si output previous motif encountered
                    last=i;
                    if (ismodule) {
                        ModuleCRM cisRegModule=parseModule(input.subList(first, last), null, false);
                        target.addModuleToPayload(cisRegModule);
                    } else {
                        Motif motif=motiflabmotifformat.parseMotif(input.subList(first, last), null, false);
                        target.addMotifToPayload(motif);
                    }
                    first=i;
                    last=0;
                }
                ismodule=(line.startsWith("#ModuleID") || line.startsWith("ModuleID"));
            }          
        }
        if (first>0) { // if there only was one module/motif it hasn't been output before
            if (ismodule) {
                ModuleCRM cisRegModule=parseModule(input.subList(first, size), null, false);
                target.addModuleToPayload(cisRegModule);
            } else {
                Motif motif=motiflabmotifformat.parseMotif(input.subList(first, size), null,false);
                target.addMotifToPayload(motif);
            }
        }       
        engine.getClient().getVisualizationSettings().redraw();  // in case colors have been updated!
        return target;
    }
            
            
    
  
}

        
       
        
        
