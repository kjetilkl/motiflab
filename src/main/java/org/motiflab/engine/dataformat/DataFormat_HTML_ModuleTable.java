/*
 
 
 */

package org.motiflab.engine.dataformat;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.data.DataMap;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModuleNumericMap;
import org.motiflab.engine.data.ModuleTextMap;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.data.OutputDataDependency;
import org.motiflab.engine.data.analysis.Analysis;
import org.motiflab.gui.ModuleLogo;
import org.motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class DataFormat_HTML_ModuleTable extends DataFormat {
    private String name="HTML_ModuleTable";
    private static final String HTML=DataFormat_HTML.HTML;

    private Class[] supportedTypes=new Class[]{ModuleCollection.class, ModuleCRM.class};
    
    public DataFormat_HTML_ModuleTable() {      
        String[] standard=ModuleCRM.getProperties(engine);
        String[] standardExt=new String[standard.length+1];
        System.arraycopy(standard, 0, standardExt, 0, standard.length);
        standardExt[standardExt.length-1]="Logo"; 
        Arrays.sort(standardExt);
        StringBuilder builder=new StringBuilder();
        builder.append("<html>An ordered, comma-separated list of properties to output for each module.<br>");
        builder.append("<br>Standard module properties include:<br><br>");
        for (String prop:standardExt) {builder.append(prop);builder.append("<br>");}
        builder.append("<br>The name of a property can be followed by the name of another property within parenthesis.");
        builder.append("<br>This second property will be displayed as a tooltip when the mouse is over the table-cell with the first property:");
//        builder.append("<br>E.g. the format:<pre>  ID, Short Name(Long Name)  </pre>will create a table with 2 columns.");
//        builder.append("<br>The first column will be the Motif ID and the second the motif's short name.");
//        builder.append("<br>If the user points the mouse at the short name of a motif, its long name will be displayed in a tooltip.");
//        builder.append("</html>");
        addParameter("Format", "ID,Size,Logo", null,builder.toString(),true,false);
        //addOptionalParameter("Logo height",24, new Integer[]{0,200},"<html>Height of sequence logos (if included in the table)</html>");
        addOptionalParameter("Logo max width",0, new Integer[]{0,10000},"<html>Maximum width of sequence logos<br>If this is set, logos will be scaled to fit within the limit.<br>A value of 0 means no limit</html>");
        addOptionalParameter("Logos",Analysis.getMotifLogoDefaultOption(HTML), Analysis.getMotifLogoOptions(HTML),"Should the image files create for logos have standard names (moduleID.gif) or be unique to each output");           
        addOptionalParameter("Multiline",Boolean.TRUE, new Boolean[]{true,false},"If selected, list-properties will be split over several lines");        
        addOptionalParameter("Headline", "", null,"Add an optional headline which will be displayed at the top of the page");
        //addOptionalParameter("Sort by","Motif ID", new String[]{"Motif ID","Short name","Long name","Size","Information content","Classification"},null);        
        //addOptionalParameter("Sort direction","Ascending", new String[]{"Ascending","Descending"},null);        
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
        return false;
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return false;
    }
    
      
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }
    
    @Override
    public String getSuffix() {
        return "html";
    }

    @Override
    public boolean isAppendable() {
        return false;
    }
    
   @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(1);
        String format="";
        // int sequenceLogoHeight=32; 
        int sequenceLogoMaxWidth=0; 
        boolean multiline=true;        
        String headline="";
        String showSequenceLogosString="";
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             format=(String)settings.getResolvedParameter("Format",defaults,engine);
             //sequenceLogoHeight=(Integer)settings.getResolvedParameter("Logo height",defaults,engine); 
             sequenceLogoMaxWidth=(Integer)settings.getResolvedParameter("Logo max width",defaults,engine); 
             multiline=(Boolean)settings.getResolvedParameter("Multiline",defaults,engine); 
             headline=(String)settings.getResolvedParameter("Headline",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);             
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
             format=(String)getDefaultValueForParameter("Format");
             //sequenceLogoHeight=(Integer)getDefaultValueForParameter("Logo height");
             sequenceLogoMaxWidth=(Integer)getDefaultValueForParameter("Logo max width");
             multiline=(Boolean)getDefaultValueForParameter("multiline");         
             headline=(String)getDefaultValueForParameter("Header");
        }
        headline=escapeHTML(headline.trim());
        engine.createHTMLheader((!headline.isEmpty())?headline:"Modules", null, null, true, true, true, outputobject);
        if (!headline.isEmpty()) {
            outputobject.append("<h1 class=\"headline\">"+headline+"</h1>\n",HTML);
        }        
        outputobject.append("<table class=\"sortable\">\n<tr>",HTML);        
        // process format
        Pattern pattern=Pattern.compile("(.+)?\\((.+)?\\)");
       
        String[] parts=format.trim().split("\\s*,\\s*");
        String[][] properties=new String[parts.length][2];
                
        for (int i=0;i<parts.length;i++) {
            Matcher matcher=pattern.matcher(parts[i]);
            if (matcher.find()) {
               properties[i][0]=matcher.group(1).trim();
               properties[i][1]=matcher.group(2);
            } else properties[i][0]=parts[i];  
            if (properties[i][1]!=null && properties[i][1].equalsIgnoreCase("Logo")) properties[i][1]="Consensus"; // I don't think graphical tooltips are allowed in standard HTML            
            else outputobject.append("<th>\n",HTML);
            outputobject.append(escapeHTML(properties[i][0]),HTML);
            outputobject.append("</th>\n",HTML);
        }        
        outputobject.append("</tr>\n",HTML);        
        ModuleLogo sequencelogo=null;   

        if (dataobject instanceof ModuleCollection) {
            ArrayList<ModuleCRM> modulelist=((ModuleCollection)dataobject).getAllModules(engine);
            int size=modulelist.size();
            int i=0;
            for (ModuleCRM cisRegModule:modulelist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                outputModule(cisRegModule, outputobject, properties, multiline, sequencelogo, sequenceLogoMaxWidth, showSequenceLogosString);
                // task.setStatusMessage("Motif "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%100==0) Thread.yield();
            }
        } else if (dataobject instanceof ModuleCRM){
                outputModule((ModuleCRM)dataobject, outputobject, properties, multiline, sequencelogo, sequenceLogoMaxWidth, showSequenceLogosString);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append("</table>\n",HTML);
        outputobject.append("</body>\n",HTML);
        outputobject.append("</html>\n",HTML);
        setProgress(100);        
        return outputobject;
    }


    /** output-formats a single module */
    protected void outputModule(ModuleCRM cisRegModule, OutputData outputobject, String[][] properties, boolean multiline, ModuleLogo sequencelogo, int sequenceLogoMaxWidth, String logoFormat) throws ExecutionError {
        outputobject.append("<tr>", HTML);
        for (String[] propertyPair:properties) {
            String property=propertyPair[0];
            String tooltip=propertyPair[1];           
            outputobject.append("<td", HTML);
            if (tooltip!=null) {
               Object tooltipvalue=null;
               try {
                   cisRegModule.getPropertyValue(tooltip,engine);
               } catch (Exception e) { // check if the property could be a map instead
                   Data item=engine.getDataItem(tooltip);
                   if (item instanceof ModuleTextMap || item instanceof ModuleNumericMap) tooltipvalue=((DataMap)item).getValue(cisRegModule.getName());
                   else throw new ExecutionError("'"+property+"' is not a recognized module property or applicable Map");
               }
               if (tooltipvalue!=null) {
                   outputobject.append(" title=\"", HTML);
                   outputProperty(tooltipvalue, outputobject, false, true);
                   outputobject.append("\"", HTML);  
               }
            }
            // append td-class here if necessary
            outputobject.append(">", HTML);
            if (property.equalsIgnoreCase("Logo")) {
                String link=getModuleLogoTag(cisRegModule, outputobject, logoFormat, sequenceLogoMaxWidth, engine);
                outputobject.append(link, HTML);    
            } else {
                Object value=null;
                try {
                    value=cisRegModule.getPropertyValue(property,engine);
                } catch (Exception e) { // check if the property could be a map instead
                   Data item=engine.getDataItem(property);
                   if (item instanceof ModuleTextMap || item instanceof ModuleNumericMap) value=((DataMap)item).getValue(cisRegModule.getName());
                   else throw new ExecutionError("'"+property+"' is not a recognized module property or applicable Map");
                }
                if (value==null && (property.equalsIgnoreCase("class") || property.equalsIgnoreCase("classification"))) value="unknown";
                outputProperty(value, outputobject, multiline, false);
            }                        
            outputobject.append("</td>", HTML);     
        }
        outputobject.append("</tr>\n", HTML);
    }

    private void outputProperty(Object value, OutputData output, boolean multiline, boolean tooltip) {
        if (value==null) {}
        else if (value instanceof ArrayList) outputArrayList((ArrayList)value, output, multiline, tooltip);
        else {
	    String string=escapeHTML((value!=null)?value.toString():"");
            if (string.indexOf(' ')>0 && !tooltip) output.append("<nobr>"+string+"</nobr>",HTML); // try to  avoid linebreaks in normal table cells	     
            else output.append(string,HTML);            
        }
    }
    
    /** Outputs an ArrayList either as a list of comma-separated values or one entry on each line */
    private void outputArrayList(ArrayList list, OutputData output, boolean multiline, boolean tooltip) {
        Iterator i = list.iterator();
	boolean first=true;
        if (!multiline && !tooltip) output.append("<nobr>",HTML);
	while (i.hasNext()) {
	    if (first) first=false; else output.append((multiline)?"<br>":", ",HTML);
            Object e = i.next();
	    String string=escapeHTML((e!=null)?e.toString():"");
            if (string.indexOf(' ')>0 && !tooltip) output.append("<nobr>"+string+"</nobr>",HTML);	     
            else output.append(string,HTML);	    
	}
        if (!multiline && !tooltip) output.append("</nobr>",HTML);
    }    
    
    
    /** Creates a sequence logo image for the given module, saves it to a temp-file and return an IMG-tag that can 
     *  be inserted in HTML-documents to display the image
     */
    protected String getModuleLogoTag(ModuleCRM cisRegModule, OutputData outputobject, String logoFormat, int maxwidth, MotifLabEngine engine) {
        if (!Analysis.includeLogosInOutput(logoFormat)) return "";
        else if (cisRegModule==null) return "?";        
        else if (Analysis.includeLogosInOutputAsText(logoFormat)) return escapeHTML(cisRegModule.getModuleLogo());
        else { 
            File imagefile=null;
            if (Analysis.includeLogosInOutputAsSharedImages(logoFormat)) {
                String logofileID=cisRegModule.getName();
                boolean sharedDependencyExists=(engine.getSharedOutputDependency(logofileID)!=null);
                OutputDataDependency dependency=outputobject.createSharedDependency(engine,logofileID, "gif",true); // returns new or existing shared dependency
                if (!sharedDependencyExists) { // the dependency has not been created before so we must save the image to file
                    imagefile=dependency.getFile();
                    try {
                        savModuleLogoImage(imagefile,cisRegModule,engine.getClient().getVisualizationSettings(),maxwidth); //
                    } catch (IOException e) {
                        engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
                    }
                } else {
                    imagefile=new File(dependency.getInternalPathName());
                }
            } else { // always save any logo to a new file
                imagefile=outputobject.createDependentFile(engine,"gif");
                try {
                    savModuleLogoImage(imagefile,cisRegModule,engine.getClient().getVisualizationSettings(),maxwidth); //
                } catch (IOException e) {
                    engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
                }
            }        
            return "<img src=\"file:///"+imagefile.getAbsolutePath()+"\" />";
        }
    }


    private void savModuleLogoImage(File file, ModuleCRM cisRegModule, VisualizationSettings settings,int maxwidth) throws IOException {
        Font modulemotiffont=ModuleLogo.getModuleMotifFont();
        BufferedImage test=new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        FontMetrics modulelogometrics=test.getGraphics().getFontMetrics(modulemotiffont);
        int width=ModuleLogo.getLogoWidth(modulelogometrics,cisRegModule)+2;
        int height=28; // I think this will be OK...
        BufferedImage image=new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();
        g.setBackground(new Color(255, 255, 255, 0)); // make the image translucent white       
        g.clearRect(0,0, width+2, height+2); // bleed a little just in case
        ModuleLogo.paintModuleLogo(g, cisRegModule, 5, 7, settings, null, maxwidth); //
        OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
        ImageIO.write(image, "png", output);
        output.close(); 
        g.dispose();
    }
   
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
       throw new ParseError("Unable to parse Module input with data format HTML_ModuleTable");
    }
    
    protected String escapeHTML(String string) {
        if (string==null) return "";
        if (string.contains("&")) string=string.replace("&", "&amp;"); // this must be first
        if (string.contains("<")) string=string.replace("<", "&lt;");
        if (string.contains(">")) string=string.replace(">", "&gt;");
        if (string.contains("\"")) string=string.replace("\"", "&#34;");    
        return string;
    }     
    
}

        
       
        
        
